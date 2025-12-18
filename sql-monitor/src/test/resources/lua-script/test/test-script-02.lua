-- 测试 Redis Lua 脚本如何处理 Map<> 类型的数据

local function getTimestamp()
    local redisTime = redis.call('TIME')

    return tonumber(redisTime[1]) * 1000 + math.floor(tonumber(redisTime[2]) / 1000.00)
end

local key = KEYS[1]
local map = ARGV[1]
-- local sequenceTaable = ARGV[2]
local decodeMap = cjson.decode(map)

-- {"name":"Jesse","age":114}
redis.log(redis.LOG_NOTICE, map)
redis.log(redis.LOG_NOTICE, decodeMap.name .. ' ' .. decodeMap.age)

if
    redis.call('EXISTS', key) == 1
then
    return cjson.encode({
        status    = "DUPLICATE_HSET_CALL",
        message   = "Key: " .. key.. " already exist!",
        timestamp = getTimestamp()
    })
end

for field, value in pairs(decodeMap) do
    redis.call('HSET', key, field, value)
end

redis.call('EXPIRE', key, 10)

return cjson.encode({
    status    = "SUCCESS",
    message   = nil,
    timestamp = getTimestamp()
})