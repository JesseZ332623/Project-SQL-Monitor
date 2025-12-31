--[[
    更新指标数据至 Redis 缓存。

    cacheKey   缓存键名
    cacheData  缓存数据，最初是 JSON，被 cjson 解析成一个 table
    cacheTTL   缓存有效期，数字类型（单位：毫秒）
]]

-- 获取当前时间戳
local function getTimestamp()
    local redisTime = redis.call('TIME')

    return tonumber(redisTime[1]) * 1000 +
           math.floor(tonumber(redisTime[2]) / 1000)
end

local cacheKey  = KEYS[1]
local cacheData = cjson.decode(ARGV[1])
local cacheTTL  = tonumber(ARGV[2])

-- TTL 不得为负值
if cacheTTL < 0 then
    return cjson.encode({
        status    = "NEGATIVE_CACHE_TTL",
        message   = "Negative cacheTTL detected! (which is " .. cacheTTL .. ")",
        timestamp = getTimestamp()
    })
end

local success, err = pcall(
        function()
            for field, value in pairs(cacheData) do
                if type(field) ~= "string" then
                    error(cjson.encode({
                        status    = "INVALID_FILED_TYPE",
                        message   = "Field name must be strings",
                        timestamp = getTimestamp()
                    }))
                end

                -- 如果 value 是 nil 或者空 JSON，我考虑设 “空” 字段
                local valueString
                    = (value == nil or value == cjson.null)
                        and ""
                        or cjson.encode(value)

                redis.pcall('HSET', cacheKey, field, valueString)
            end

            redis.pcall('PEXPIRE', cacheKey, cacheTTL)
        end
)

if not success then
    return cjson.encode({
        status    = "ERROR",
        message   = err,
        timestamp = getTimestamp()
    })
else
    -- 缓存执行成功后，
    -- 返回信息这一块确实没什么好写的。。。
    return cjson.encode({
        status    = "SUCCESS",
        message   = nil,
        timestamp = getTimestamp()
    })
end