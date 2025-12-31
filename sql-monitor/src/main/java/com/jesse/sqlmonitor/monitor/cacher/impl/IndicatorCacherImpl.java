package com.jesse.sqlmonitor.monitor.cacher.impl;

import cn.hutool.core.util.IdUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jesse.sqlmonitor.indicator_record.service.IndicatorSender;
import com.jesse.sqlmonitor.luascript_reader.LuaScriptReader;
import com.jesse.sqlmonitor.luascript_reader.impl.LuaOperatorResult;
import com.jesse.sqlmonitor.luascript_reader.impl.LuaScriptOperatorType;
import com.jesse.sqlmonitor.luascript_reader.impl.exception.LuaScriptExecuteFailed;
import com.jesse.sqlmonitor.monitor.cacher.IndicatorCacher;
import com.jesse.sqlmonitor.monitor.cacher.impl.warm_up.health.RedisHealthChecker;
import com.jesse.sqlmonitor.monitor.cacher.impl.util.CacheDataConverter;
import com.jesse.sqlmonitor.monitor.cacher.impl.warm_up.CacherWarmUpEventPublisher;
import com.jesse.sqlmonitor.monitor.constants.IndicatorKeyNames;
import com.jesse.sqlmonitor.properties.R2dbcMasterProperties;
import com.jesse.sqlmonitor.properties.RedisCacheProperties;
import com.jesse.sqlmonitor.response_body.SentIndicator;
import com.jesse.sqlmonitor.response_body.base.ResponseBase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.redisson.api.RLockReactive;
import org.redisson.api.RedissonReactiveClient;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/** 指标数据缓存器实现。*/
@Slf4j
@Component
@RequiredArgsConstructor
public class IndicatorCacherImpl implements IndicatorCacher
{
    /**
     * 一个空的超时异常，
     * 目前在 {@link IndicatorCacher#getIndicatorCacheWithLock(IndicatorKeyNames, Mono, Class)}
     * 方法复用抛出。
     */
    private final static
    TimeoutException EMPTY_TIMEOUT_EXCEPTION
        = new TimeoutException();

    /** Jackson 对象映射器。*/
    private final ObjectMapper objectMapper;

    /** 主数据库连接属性。*/
    private final R2dbcMasterProperties properties;

    /** Redis 缓存操作相关属性。*/
    private final RedisCacheProperties redisCacheProperties;

    /** 指标数据发送器接口。*/
    private final IndicatorSender indicatorSender;

    /** Redisson 响应式客户端实例。*/
    private final RedissonReactiveClient redissonReactiveClient;

    /** Redis Lua 脚本读取器接口。*/
    private final LuaScriptReader luaScriptReader;

    /** 专门用于执行 Lua 脚本的响应式 Redis 模板。*/
    private final
    ReactiveRedisTemplate<String, LuaOperatorResult> redisLuaTemplate;

    /** 响应式通用 Redis 模板。*/
    private final
    ReactiveRedisTemplate<String, Object> redisTemplate;

    /** 缓存预热事件发布器。*/
    private final
    CacherWarmUpEventPublisher cacherWarmUpEventPublisher;

    /** Redis 健康状态检查器。*/
    private final
    RedisHealthChecker redisHealthChecker;

    /** 获取主数据的 IP + PORT 字符串。*/
    private @NotNull String
    getMasterBaseAddress() {
        return
        this.properties.getHost() + ":" + this.properties.getPort();
    }

    /** 拼接指标缓存数据键。*/
    @Contract(pure = true)
    private @NotNull String
    getCacheKey(@NotNull IndicatorKeyNames keyNames)
    {
        return
        redisCacheProperties.getKeyPrefix() +
        this.properties.getHost() + ":" +
        keyNames.getKeyName();
    }

    /** 获取分布式锁键。*/
    private @NotNull String
    getLockKey(@NotNull IndicatorKeyNames keyNames)
    {
        return
        this.properties.getHost() + "-" + keyNames.getKeyName();
    }

    /** 更新指标数据至 Redis 缓存。*/
    private @NotNull Mono<Void>
    saveIndicatorMapToCache(
        @NotNull IndicatorKeyNames   keyNames,
        @NotNull Map<String, Object> indicatorMap
    )
    {
        final String cacheKey = this.getCacheKey(keyNames);
        final long cacheTTL   = this.redisCacheProperties.getTtl().toMillis();
        final Duration cacheOperatorTimeout
            = this.redisCacheProperties.getCacheOperatorTimeout();

        return
        this.luaScriptReader
            .read(LuaScriptOperatorType.INDICATOR_CACHER, "saveIndicatorMapToCache.lua")
            .flatMap((script) ->
                this.redisLuaTemplate
                    .execute(script, List.of(cacheKey), indicatorMap, cacheTTL)
                    .timeout(cacheOperatorTimeout)
                    .next()
                    .flatMap((result) ->
                        ("SUCCESS".equals(result.getStatus()))
                            ? Mono.empty()
                            : Mono.error(
                                new LuaScriptExecuteFailed(
                                    result.getStatus(), result.getMessage(), result.getTimestamp()
                                )
                            )
                    )
            );
    }

    /** 将最新的指标数据包装成 {@link SentIndicator} 后发往 RabbitMQ。*/
    private <T extends ResponseBase<T>>
    @NotNull Mono<Void>
    sendIndicatorToTaskQueue(
        @NotNull ResponseBase<T> indicatorBase,
        @NotNull Class<T>        type
    )
    {
        return
        CacheDataConverter.safeIndicatorTypeCast(indicatorBase, type)
            .flatMap((indicator) -> {
                // 检查这个指标是否有效（头几次指标数据不纳入统计）
                if (indicator.isValid())
                {
                    SentIndicator<T> sentIndicator
                        = new SentIndicator<>(
                            LocalDateTime.now(),
                            this.getMasterBaseAddress(),
                            indicator
                        );

                    // 发送至 RabbitMQ
                    return
                    this.indicatorSender.sendIndicator(sentIndicator);
                }

                return Mono.empty();
            });

    }

    /**
     * 尝试从 Redis 缓存中读取指标数据，
     * 如果读取不到则返回 {@link Mono#empty()} 。
     *
     * @param keyNames  指标数据键名
     * @param type      指标数据实际类型
     *
     * @return 指标数据本身，供下游构造响应体使用
     */
    public <T extends ResponseBase<T>>
    @NotNull Mono<T>
    getIndicatorCache(
        @NotNull IndicatorKeyNames keyNames,
        Class<T> type
    )
    {
        final String cacheKey = this.getCacheKey(keyNames);

        return
        this.redisTemplate
            .opsForHash()
            .entries(cacheKey)
            .timeout(this.redisCacheProperties.getCacheOperatorTimeout())
            .collectMap((entry) ->
                (String) entry.getKey(), Map.Entry::getValue)
            .flatMap((indicatorMap) -> {
                // 若从缓存中没拿到数据，直接返回 Mono.empty() 即可。
                if (indicatorMap.isEmpty()) {
                    return Mono.empty();
                }

                return
                CacheDataConverter.restoreIndicatorMapToInstance(
                    indicatorMap, type, this.objectMapper
                );
            })
            .onErrorResume((exception) -> {
                // 若从 Redis 缓存中获取失败（比如 Redis 服务重启、宕机等情况）
                // 则直接视为缓存获取失败，后续的所有操作（加锁、更新缓存等）就没有意义了，
                // 因此可以直接返回从数据库获取指标数据的响应式流，
                // 确保前端的请求不被堆积（优雅降级）
                log.warn(
                    "Get indicator {} from cache failed! Caused by {}. fallback to database.",
                    this.getCacheKey(keyNames), exception.getMessage()
                );

                return Mono.empty();
            });
    }

    @Override
    public <T extends ResponseBase<T>>
    @NotNull Mono<ResponseBase<T>>
    cacheIndicatorData(
        IndicatorKeyNames keyNames,
        ResponseBase<T>   indicator,
        Class<T>          type
    )
    {
        return
        CacheDataConverter
            .makeCacheDataFromIndicator(indicator, type, this.objectMapper)
            .flatMap((indicatorMap) ->
                Mono.when(
                    this.saveIndicatorMapToCache(keyNames, indicatorMap),
                    this.sendIndicatorToTaskQueue(indicator, type)
                ))
            // 更新缓存操作成功后，需要标记这一类的缓存数据预热成功
            .doOnSuccess((ignore) ->
                this.cacherWarmUpEventPublisher.markAsWarnUp(keyNames))
            .thenReturn(indicator)
            // 若因为某些原因出错了，
            // 这一次的缓存操作算做失败，直接返回指标数据给下游即可
            .onErrorResume((exception) -> {
                if (exception instanceof LuaScriptExecuteFailed)
                {
                    log.error(
                        "Cache indicator data to redis failed, " +
                        "key: {}, error type: {}, timestamp: {}",
                        this.getCacheKey(keyNames),
                        ((LuaScriptExecuteFailed) exception).getErrorType(),
                        ((LuaScriptExecuteFailed) exception).getTimestamp()
                    );
                }
                else
                {
                    log.error(
                        "Cache indicator data to redis failed, key: {}, Caused by: {}",
                        this.getCacheKey(keyNames), exception.getMessage()
                    );
                }

                return Mono.just(indicator);
            });
    }

    @Override
    public <T extends ResponseBase<T>>
    @NotNull Mono<T>
    getIndicatorCacheWithLock(
        @NotNull IndicatorKeyNames keyNames,
        Mono<T>  indicatorSupplier,
        Class<T> indicatorType
    )
    {
        // 先检查 Redis 的健康状态
        if (!this.redisHealthChecker.isHealthy()) {
            return indicatorSupplier;
        }

        // 再检查 Redis 的预热情况
        if (!this.cacherWarmUpEventPublisher.isWarmUp(keyNames))
        {
            // 先从数据库取数据然后再更新到缓存，
            // 直接返回数据库查询结果给上层调用者
            // 避免 Redis 重新上线后被大量堆积的请求冲击
            // 直到缓存数据被预热后，再放行至正常逻辑
            return
            indicatorSupplier.doOnSuccess((data) -> {
                if (Objects.nonNull(data)) {
                    this.cacherWarmUpEventPublisher
                        .publishWarnUpEvent(keyNames, data, indicatorType);
                }
            });
        }

        return
        this.getIndicatorCache(keyNames, indicatorType) // 先尝试从缓存获取数据
            .flatMap((indicator) ->
                CacheDataConverter.safeIndicatorTypeCast(indicator, indicatorType))
            .switchIfEmpty(             // 如果缓存内部没有数据
                Mono.defer(() -> {
                    // 获取锁实例
                    final RLockReactive lock
                        = this.redissonReactiveClient
                              .getLock(this.getLockKey(keyNames));
                    // 获取线程号
                    //（响应式环境下线程号不可靠，这里使用雪花算法生成的 ID 在上下文传递）
                    final long threadId  = IdUtil.getSnowflakeNextId();
                    final long waitTime  = this.redisCacheProperties
                                               .getLockWaitTimeout().toSeconds();
                    final long leaseTime = this.redisCacheProperties
                                               .getLockLeaseTime();

                    return
                    Mono.usingWhen(
                        lock.tryLock(waitTime, leaseTime, TimeUnit.SECONDS, threadId),
                        (isLocked) -> {
                            if (isLocked)
                            {
                               return
                               this.getIndicatorCache(keyNames, indicatorType)
                                   .flatMap((indicator) ->
                                       CacheDataConverter.safeIndicatorTypeCast(indicator, indicatorType))
                                   .switchIfEmpty(
                                       // 第二次检查仍然没有数据，
                                       // 最终去数据库获取并计算指标数据然后更新缓存并同时发往消息队列
                                       indicatorSupplier
                                           .flatMap((indicator) ->
                                               this.cacheIndicatorData(keyNames, indicator, indicatorType)
                                                   .thenReturn(indicator)
                                           )
                                   );
                            }
                            else
                            {
                               /* 指定时间内没有获取锁，抛出异常降级处理。*/
                               log.error(
                                   "Acquire lock of {} timeout! (wait time: {} seconds)",
                                   keyNames, waitTime
                               );

                               return Mono.error(EMPTY_TIMEOUT_EXCEPTION);
                           }
                       },
                        (ignore) ->
                            lock.unlock(threadId) // 释放锁
                    );
                })
            )
            .onErrorResume((exception) -> {
               // 若 Redis 缓存、锁操作失败、队列发送失败或者发生其他错误，
               // 则视为缓存获取失败，直接返回从数据库获取指标数据的响应式流即可（优雅降级）
                return indicatorSupplier;
            });
    }
}