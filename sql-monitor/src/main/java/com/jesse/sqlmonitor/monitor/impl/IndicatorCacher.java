package com.jesse.sqlmonitor.monitor.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jesse.sqlmonitor.monitor.constants.IndicatorKeyNames;
import com.jesse.sqlmonitor.monitor.impl.qps.QPSCounter;
import com.jesse.sqlmonitor.response_body.base.ResponseBase;
import io.github.jessez332623.redis_lock.distributed_lock.RedisDistributedLock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;

/** 指标数据缓存器。*/
@Slf4j
@Component
@RequiredArgsConstructor
public class IndicatorCacher
{
    /** 所有指标缓存数据的前缀命名空间。*/
    private static final
    String INDICATOR_KEY_PREFIX = "sql-monitor-cache:";

    /**
     * 缓存的有效期（为前端的最短查询间隔 - 500 毫秒的冗余）
     * 确保正确的触发缓存更新，避免读到旧数据。
     */
    private static final
    Duration CACHE_TTL = Duration.ofMillis(2500L);

    /** Jackson 对象映射器。*/
    private final ObjectMapper objectMapper;

    /** Redis 分布式锁实例。*/
    private final RedisDistributedLock distributedLock;

    /** 响应式通用 Redis 模板。*/
    private final
    ReactiveRedisTemplate<String, Object> redisTemplate;

    /**
     * 安全的类型转换操作，
     * 从基类 {@link ResponseBase} 转化成它指定的子类，并包装为 {@link Mono}。
     *
     * @param indicator 指标数据实例（此时为基类）
     * @param type      要转化成的子类类型令牌
     *
     * @return 转化后的子类型
     *
     * @throws ClassCastException
     * 如果类型令牌呈示的类型不是 {@literal ResponseBase<T>} 的子类时仍进行转换，则抛出本异常
     */
    @SuppressWarnings("unchecked")
    private <T extends ResponseBase<T>>
    @NotNull Mono<T>
    safeCast(@NotNull ResponseBase<?> indicator, @NotNull Class<T> type)
    {
        if (type.isInstance(indicator)) {
            return Mono.just((T) indicator);
        }

        return
        Mono.error(
            new ClassCastException(
                "Can not cast" + indicator.getClass() + "to" + type
            )
        );

    }

    /** 拼接指标缓存数据键。*/
    @Contract(pure = true)
    private @NotNull String
    getCacheKey(@NotNull IndicatorKeyNames keyNames) {
        return INDICATOR_KEY_PREFIX + keyNames.getKeyName();
    }

    /**
     * 将指定指标数据缓存到 Redis。
     *
     * @param keyNames  指标数据键名
     * @param indicator 指标数据实例
     *
     * @return 指标数据本身，供下游构造响应体使用
     */
    private  <T extends ResponseBase<T>>
    @NotNull Mono<ResponseBase<T>>
    cacheIndicatorData(IndicatorKeyNames keyNames, ResponseBase<T> indicator)
    {
        return
        Mono.defer(() -> {
            final String cacheKey = this.getCacheKey(keyNames);
            String indicatorJSON;

            log.info("Cache indicator data to key: {}", cacheKey);
            
           try
           {
               indicatorJSON
                   = this.objectMapper.writeValueAsString(indicator);
           }
           catch (JsonProcessingException exception)
           {
               // 若 JSON 解析失败，
               // 这一次的缓存操作就算失败，直接返回指标数据给下游即可。
               log.error(
                   "Process indicator instance to JSON failed, key: {}, Caused by: {}",
                   this.getCacheKey(keyNames), exception.getMessage()
               );

               return Mono.just(indicator);
           }
           
           // JSON 成功解析，将数据存入 Redis 缓存
           return 
           this.redisTemplate
               .opsForValue()
               .set(cacheKey, indicatorJSON, CACHE_TTL)
               .timeout(Duration.ofSeconds(3L))
               .thenReturn(indicator);
        })
        // 若 Redis 因为某些原因出错了，
        // 这一次的缓存操作也算失败，直接返回指标数据给下游即可
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
     */
    private <T extends ResponseBase<T>>
    @NotNull Mono<ResponseBase<T>>
    getIndicatorCache(@NotNull IndicatorKeyNames keyNames)
    {
        return
        Mono.defer(() -> {
            final String cacheKey = this.getCacheKey(keyNames);

            log.info("Try to get indicator data (key: {}) from cache.", cacheKey);

            return
            this.redisTemplate.opsForValue().get(cacheKey)
                .timeout(Duration.ofSeconds(5L))
                .cast(String.class) // 确保类型安全
                .flatMap(indicatorJSON -> {
                    try
                    {
                        ResponseBase<T> indicator = this.objectMapper
                            .readValue(indicatorJSON, new TypeReference<>() {});
                        return Mono.just(indicator);
                    }
                    catch (JsonProcessingException exception)
                    {
                        log.error(
                            "Processing indicator JSON to instance failed! Caused by: {}",
                            exception.getMessage()
                        );

                        return Mono.empty();
                    }
                })
                .doOnError(exception ->
                    log.error(
                        "Get indicator (Key: {}) from cache failed! Caused by: {}",
                        cacheKey, exception.getMessage()
                    )
                )
                .onErrorResume(exception -> Mono.empty())
                .switchIfEmpty(Mono.defer(() -> {
                    log.info(
                        "Indicator data (key: {}) not in the cache, retrieve data from the database.",
                        cacheKey
                    );
                    return Mono.empty();
                }));
        });
    }

    /**
     * 尝试从 Redis 缓存中读取指标数据，
     * 如果读取不到则加锁去数据库获取并读取指标数据，并更新至缓存。
     *<strong>（使用双重检查策略，避免缓存被击穿）</strong>
     *
     * <p>这里还是得补充说明一下选择双重检查策略的原因：</p>
     * <p>
     *     假设有 4 个线程先后进入本方法，在缓存中都没找到数据（第一次检查），
     *     第一个线程会加锁并再检查一次缓存后进入数据库拿到数据后更新缓存，
     *     后续的线程依次加锁后则都会再次检查缓存中是否有数据（第二次检查），
     *     因此，在同一时刻下，只会有一个线程进入数据库，其他线程要么被阻塞，要么已经从缓存中拿到了数据。
     * </p>
     *
     * @param keyNames              指标数据键名
     * @param indicatorSupplier     从数据库获取指标数据的响应式流（比如 {@link QPSCounter#calculateQPS()}）
     * @param indicatorType         指标数据类型，用于安全转换
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
        this.getIndicatorCache(keyNames) // 先尝试从缓存获取数据
            .flatMap((indicator) ->
                this.safeCast(indicator, indicatorType))
            .switchIfEmpty(             // 如果缓存内部没有数据
                Mono.defer(() ->
                    this.distributedLock.withLock(  // 加分布式锁
                        keyNames.getKeyName(),
                        5L, 1L,
                        (lockName) ->      // 在进入数据库前再检查一次缓存防止击穿
                            this.getIndicatorCache(keyNames)
                                .flatMap((indicator) ->
                                    this.safeCast(indicator, indicatorType))
                                .switchIfEmpty(
                                    indicatorSupplier   // 第二次检查仍然没有数据，最终去数据库获取并计算指标数据
                                        .flatMap((indicator) ->
                                            this.cacheIndicatorData(keyNames, indicator)
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
            })
            // I/O 密集型操作，得放到无界弹性线程池去执行，可别让宝贵的事件循环线程干这个活。
            .subscribeOn(Schedulers.boundedElastic());
    }
}