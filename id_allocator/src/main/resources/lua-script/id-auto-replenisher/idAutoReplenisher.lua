--[[
    ID 自动补货操作。

    KEYS:
        listKey ID 列表键

    ARGV:
        targetBuffer  期望队列中保持的 ID 数量
        ARGV[2 ...]   预生成的 ID 数组
]]
local listKey = KEYS[1]

local targetBuffer = tonumber(ARGV[1])

-- 获取当前时间戳
local function getTimestamp()
    local redisTime = redis.call('TIME')

    return tonumber(redisTime[1]) * 1000 +
           math.floor(tonumber(redisTime[2]) / 1000)
end

local status, result = pcall(
        function()
            local currentLen = redis.call('LLEN', listKey)

            -- 如果 ID 列表已满，直接返回
            if currentLen >= targetBuffer then
                return {
                    status  = "BUFFER_FULL",
                    message = "Buffer already sufficient.",
                    data    = cjson.encode({
                        added     = 0,
                        beforeLen = currentLen,
                        afterLen  = currentLen,
                        target    = targetBuffer
                    }),
                    timestamp = getTimestamp()
                }
            end

            local needed    = targetBuffer - currentLen
            local available = #ARGV - 1
            local toAdd     = math.min(needed, available)

            -- 若无 ID 可用，也返回（罕见的）
            if toAdd <= 0 then
                return {
                    status  = "NO_MORE_IDS",
                    message = "No additional ids available to replanish.",
                    data    = cjson.encode({
                        added     = 0,
                        beforeLen = currentLen,
                        afterLen  = currentLen,
                        target    = targetBuffer
                    }),
                    timestamp = getTimestamp()
                }
            end

            -- 正式执行填充
            redis.call('LPUSH', listKey, unpack(ARGV, 2, 1 + toAdd))

            -- 计算填充后的列表长度
            local newLen = currentLen + toAdd

            return {
                status  = "REPLENISHED",
                message = "Successfully added ids to buffer.",
                data    = cjson.encode({
                    added     = toAdd,
                    beforeLen = currentLen,
                    afterLen  = newLen,
                    target    = targetBuffer
                }),
                timestamp = getTimestamp()
            }
        end
)

if status then
    -- result 是 table（成功或业务失败）
    return cjson.encode(result)
else
    -- Redis 服务端级别的错误或者 Lua 运行时错误
    return cjson.encode({
        status    = "UNKNOWN_ERROR",
        message   = tostring(result),
        timestamp = getTimestamp()
    })
end