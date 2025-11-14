package com.jesse.sqlmonitor.monitor.cacher;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jesse.sqlmonitor.indicator_record.service.IndicatorSender;
import com.jesse.sqlmonitor.monitor.constants.IndicatorKeyNames;
import com.jesse.sqlmonitor.monitor.impl.qps.QPSCounter;
import com.jesse.sqlmonitor.properties.R2dbcMasterProperties;
import com.jesse.sqlmonitor.response_body.SentIndicator;
import com.jesse.sqlmonitor.response_body.base.ResponseBase;
import io.github.jessez332623.redis_lock.distributed_lock.RedisDistributedLock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;

/** 指标数据缓存器实现。*/
@Slf4j
@Component
@RequiredArgsConstructor
public class IndicatorCacher
{
    /** 所有指标缓存数据的前缀命名空间。*/
    @Value("${app.redis-cache.key-prefix}")
    private String INDICATOR_KEY_PREFIX;

    /**
     * 缓存的有效期（为前端的最短查询间隔 - 500 毫秒的冗余）
     * 确保正确的触发缓存更新，避免读到旧数据。
     */
    @Value("${app.redis-cache.ttl}")
    private Duration CACHE_TTL;

    /** Jackson 对象映射器。*/
    private final ObjectMapper objectMapper;

    /** 主数据库连接属性。*/
    private final R2dbcMasterProperties properties;

    /** 指标数据发送器接口。*/
    private final IndicatorSender indicatorSender;

    /** Redis 分布式锁实例。*/
    private final RedisDistributedLock distributedLock;

    /** 响应式通用 Redis 模板。*/
    private final
    ReactiveRedisTemplate<String, Object> redisTemplate;

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
        INDICATOR_KEY_PREFIX      +
        this.properties.getHost() + ":" +
        keyNames.getKeyName();
    }

    /** 更新指标数据至 Redis 缓存。*/
    private @NotNull Mono<Void>
    saveIndicatorMapToCache(
        @NotNull String              cacheKey,
        @NotNull Map<String, Object> indicatorMap
    )
    {
        return
        this.redisTemplate
            .opsForHash()
            .putAll(cacheKey, indicatorMap)
            .then(this.redisTemplate.expire(cacheKey, CACHE_TTL))
            .timeout(Duration.ofSeconds(3L))
            .then();
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
        CacheDataConverter.safeCast(indicatorBase, type)
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
     * 将指定指标数据缓存到 Redis，更新缓存的同时，也发送指标数据到消息队列。
     *
     * @param keyNames  指标数据键名
     * @param indicator 指标数据实例
     * @param type      指标数据实际类型
     *
     * @return 指标数据本身，供下游构造响应体使用
     */
    public <T extends ResponseBase<T>>
    @NotNull Mono<ResponseBase<T>>
    cacheIndicatorData(
        IndicatorKeyNames keyNames,
        ResponseBase<T>   indicator,
        Class<T>          type
    )
    {
        final String cacheKey = this.getCacheKey(keyNames);

        return
        CacheDataConverter
            .makeCacheDataFromIndicator(indicator, type, this.objectMapper)
            .flatMap((indicatorMap) ->
                Mono.when(
                    this.saveIndicatorMapToCache(cacheKey, indicatorMap),
                    this.sendIndicatorToTaskQueue(indicator, type)
                )
            )
            .thenReturn(indicator)
            // 若 Redis 因为某些原因出错了，
            // 这一次的缓存操作算做失败，直接返回指标数据给下游即可
           .onErrorResume((exception) -> {
               log.error(
                   "Cache indicator data to redis failed, key: {}, Caused by: {}",
                   this.getCacheKey(keyNames), exception.getMessage()
               );

               return Mono.just(indicator);
           });
    }

    /**
     * 尝试从 Redis 缓存中读取指标数据，
     * 如果读取不到则返回 {@link Mono#empty()}。
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
            .collectMap((entry) ->
                (String) entry.getKey(), Map.Entry::getValue)
            .timeout(Duration.ofSeconds(5L))
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
            .doOnError(exception ->
                log.error(
                    "Get indicator (Key: {}) from cache failed! Caused by: {}",
                    cacheKey, exception.getMessage()
                )
            )
            .onErrorResume(exception -> Mono.empty());
    }

    /**
     * 尝试从 Redis 缓存中读取指标数据，
     * 如果读取不到则加锁去数据库获取并读取指标数据，并更新至缓存。
     * <strong>（使用双重检查策略，避免缓存被击穿）</strong>
     *
     * <p>这里还是得补充说明一下选择双重检查策略的原因：</p>
     * <p>
     *     假设有 4 个线程先后进入本方法，在缓存中都没找到数据（第一次检查），
     *     第一个线程会加锁并再检查一次缓存后进入数据库拿到数据后更新缓存，
     *     后续的线程依次加锁后则都会再次检查缓存中是否有数据（第二次检查），
     *     因此，在同一时刻下，只会有一个线程进入数据库，其他线程要么被阻塞，要么已经从缓存中拿到了数据。
     * </p>
     *
     * @param keyNames          指标数据键名
     * @param indicatorSupplier 从数据库获取指标数据的响应式流（比如 {@link QPSCounter#calculateQPS()}）
     * @param indicatorType     指标数据类型，用于安全转换
     *
     * @return 最终读取到的指标类型
     */
    public <T extends ResponseBase<T>>
    @NotNull Mono<T>
    getIndicatorCacheWithLock(
        @NotNull IndicatorKeyNames keyNames,
        Mono<T>  indicatorSupplier,
        Class<T> indicatorType
    )
    {
        return
        this.getIndicatorCache(keyNames, indicatorType) // 先尝试从缓存获取数据
            .flatMap((indicator) ->
                CacheDataConverter.safeCast(indicator, indicatorType))
            .switchIfEmpty(             // 如果缓存内部没有数据
                Mono.defer(() ->
                    this.distributedLock.withLock(  // 加分布式锁，给 5 秒的时间获取锁，锁期限为 2.5 秒
                        keyNames.getKeyName(),
                        Duration.ofSeconds(5L), Duration.ofMillis(2500L),
                        (lockName) ->      // 在进入数据库前再检查一次缓存防止击穿
                            this.getIndicatorCache(keyNames, indicatorType)
                                .flatMap((indicator) ->
                                    CacheDataConverter.safeCast(indicator, indicatorType))
                                .switchIfEmpty(
                                    // 第二次检查仍然没有数据，
                                    // 最终去数据库获取并计算指标数据然后更新缓存并同时发往消息队列
                                    indicatorSupplier
                                        .flatMap((indicator) ->
                                            this.cacheIndicatorData(keyNames, indicator, indicatorType)
                                                .thenReturn(indicator)
                                        )
                                )
                    )
                )
            )
            .onErrorResume((exception) -> {
                // 若 Redis 操作失败或者发生其他错误，
                // 则视为缓存获取失败，直接返回从数据库获取指标数据的响应式流即可（优雅降级）
                log.warn(
                    "Cache lookup failed for key: {}, fallback to database",
                    this.getCacheKey(keyNames)
                );

                return indicatorSupplier;
            });
    }
}