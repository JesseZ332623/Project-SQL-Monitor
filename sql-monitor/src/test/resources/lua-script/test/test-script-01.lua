-- 本脚本主要测试 Redis Lua 脚本对 CJSON 的支持

local randomVal = tonumber(ARGV[1])

local function getTimestamp()
    local redisTime = redis.call('TIME')

    return tonumber(redisTime[1]) * 1000 + math.floor(tonumber(redisTime[2]) / 1000.00)
end

local suceessResult = {
    status    = "SUCCESS",
    message   = "Randoom value less then 150",
    timestamp = getTimestamp()
}

local failedResult = {
    status    = "TEST_FIALED",
    message   = "Randoom value bigger then 150",
    timestamp = getTimestamp()
}

if (randomVal < 150) then
    return cjson.encode(suceessResult)
else
    return cjson.encode(failedResult)
end
