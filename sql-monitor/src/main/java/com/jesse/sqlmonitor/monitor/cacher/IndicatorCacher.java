package com.jesse.sqlmonitor.monitor.cacher;

import com.jesse.sqlmonitor.monitor.constants.IndicatorKeyNames;
import com.jesse.sqlmonitor.monitor.impl.qps.QPSCounter;
import com.jesse.sqlmonitor.response_body.base.ResponseBase;
import org.jetbrains.annotations.NotNull;
import reactor.core.publisher.Mono;

public interface IndicatorCacher
{
    /**
     * 将指定指标数据缓存到 Redis，更新缓存的同时，也发送指标数据到消息队列。
     *
     * @param keyNames  指标数据键名
     * @param indicator 指标数据实例
     * @param type      指标数据实际类型
     *
     * @return 指标数据本身，供下游构造响应体使用
     */
    <T extends ResponseBase<T>>
    @NotNull Mono<ResponseBase<T>>
    cacheIndicatorData(
        IndicatorKeyNames keyNames,
        ResponseBase<T> indicator,
        Class<T> type
    );

    /**
     * 尝试从 Redis 缓存中读取指标数据，
     * 如果读取不到则加锁去数据库获取并读取指标数据，并更新至缓存。
     * <strong>（使用双重检查策略，避免缓存被击穿）</strong>
     *
     * <p>这里还是得补充说明一下选择双重检查策略的原因：</p>
     * <p>
     *     假设部署在不同服务器的应用共计有 4 个请求先后进入本方法，在缓存中都没找到数据（第一次检查），
     *     第一个请求会加锁并再检查一次缓存后进入数据库拿到数据后更新缓存，
     *     后续的请求依次加锁后则都会再次检查缓存中是否有数据（第二次检查），
     *     因此，在同一时刻下，只会有一个请求真正进入数据库，
     *     其他请求要么被阻塞，要么已经从缓存中拿到了数据（这样就极大的缓解了分布式环境下的数据库压力）。
     * </p>
     *
     * @param keyNames          指标数据键名
     * @param indicatorSupplier 从数据库获取指标数据的响应式流（比如 {@link QPSCounter#calculateQPS()}）
     * @param indicatorType     指标数据类型，用于安全转换
     *
     * @return 最终读取到的指标类型
     */
    <T extends ResponseBase<T>>
    @NotNull Mono<T>
    getIndicatorCacheWithLock(
        @NotNull IndicatorKeyNames keyNames,
        Mono<T>  indicatorSupplier,
        Class<T> indicatorType
    );
}
