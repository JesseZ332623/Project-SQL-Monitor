--[[
    使用令牌桶策略对邮件的发送进行限流。

    KEYS:
        key  邮件发送限流键前缀 (sql-monitor-mail-rate:<db-host>)

    ARGV:
        rate  (填充桶的速率，单位：令牌/秒，如 0.1667 即为 10 令牌每分钟)
        burst 桶的容量
]]

local key    = KEYS[1]

local rate   = tonumber(ARGV[1])
local burst  = ARGV[2]

-- 当前时间戳（秒）
local now = tonumber(redis.call('TIME')[1])

-- 获取当前时间戳
local function getTimestamp()
    local redisTime = redis.call('TIME')

    return tonumber(redisTime[1]) * 1000 +
           math.floor(tonumber(redisTime[2]) / 1000)
end

-- 动态的 TTL 设置（最多不超过 1 天，最少不超过 60 秒）
local function getSafeTTL(tokens)
    -- 公式为：填满桶需要的时间 * 2 + 300 秒缓冲
    -- 这样不活跃的桶会更快过期，活跃的会自动续期
    local timeToFull = (burst - tokens) / rate
    if timeToFull < 0 then timeToFull = 0 end
    return math.max(60, math.min(86400, math.floor(timeToFull * 2 + 300)))
end

local status, result = pcall(
        function()
            -- 获取当前桶中的令牌数（第一次执行时则填满桶）
            local tokens = tonumber(redis.call('HGET', key, 'tokens') or burst)

            -- 获取上次填充令牌桶的时间
            local last   = tonumber(redis.call('HGET', key, 'last') or now)

            -- 需要补充的令牌数 = 两次填充时间之差 * 填充速率
            local added = (now - last) * rate

            -- 将令牌数强制限制在 [0, burst] 内，避免浮点精度问题
            tokens = math.max(0, math.min(burst, tokens + added))

            -- 尝试消耗 1 个令牌
            if tokens >= 1 then
                tokens = math.max(0, tokens - 1)

                -- 更新记录
                redis.call('HSET', key, 'tokens', tokens, 'last', now)
                redis.call('EXPIRE', key, getSafeTTL(tokens))

                -- 放行
                return {
                    status  = "SEND_PASS",
                    message = nil,
                    timestamp = getTimestamp()
                }
            else
                redis.call('EXPIRE', key, getSafeTTL(tokens))
                -- 拒绝
                return {
                    status  = "SEND_REJECT",
                    message = nil,
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