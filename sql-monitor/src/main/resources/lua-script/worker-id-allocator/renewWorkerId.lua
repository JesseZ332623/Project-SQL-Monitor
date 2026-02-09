--[[
    本服务实例定时续租 worker ID

    KEYS:
        workIdKey 服务实例 workerId 键（例 snowflake:wid:lock:22）

    ARGV:
        instanceUUID 本服务实例唯一 ID
        leasDuration 本服务对 workerId 的租期（单位：毫秒）
]]

local workIdKey = KEYS[1]

local instanceUUID  = ARGV[1]
local leasDuration = ARGV[2]

-- 获取当前时间戳
local function getTimestamp()
    local redisTime = redis.call('TIME')

    return tonumber(redisTime[1]) * 1000 +
           math.floor(tonumber(redisTime[2]) / 1000)
end

local status, result = pcall(
        function()
            if redis.call('GET', workIdKey) == instanceUUID then
                redis.call('PEXPIRE', workIdKey, leasDuration)

                return {
                    status    = "SUCCESS",
                    message   = "Server instance " .. instanceUUID .. " renew worker ID success!",
                    timestamp = getTimestamp()
                }
            else
                return {
                    status    = "RENEW_FAILED",
                    message   = "Renew failed, worker ID may lost! "
                                .. "(key: " .. workIdKey .. ",  instance ID: " .. instanceUUID .. ")",
                    timestamp = getTimestamp()
                }
            end
        end
)

if status then
    -- result 是 table（成功或业务失败）
    return cjson.encode(result)
else
    -- Redis 服务端级别的错误
    return cjson.encode({
        status    = "UNKNOWN_ERROR",
        message   = tostring(result),
        timestamp = getTimestamp()
    })
end