package com.jesse.sqlmonitor.monitor.impl.innodb_cache_hit.impl;

import com.jesse.sqlmonitor.monitor.constants.GlobalStatusName;
import com.jesse.sqlmonitor.monitor.impl.GlobalStatusQuery;
import com.jesse.sqlmonitor.monitor.impl.innodb_cache_hit.InnoDBCacheHitCounter;
import com.jesse.sqlmonitor.response_body.InnodbBufferCacheHitRate;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static com.jesse.sqlmonitor.monitor.constants.MonitorConstants.*;

/** InnDB 缓存命中率计算器实现。*/
@Slf4j
@Component
@RequiredArgsConstructor
public class InnoDBCacheHitCounterImpl implements InnoDBCacheHitCounter
{
    /** 全局状态查询器。*/
    private final GlobalStatusQuery globalStatusQuery;

    /** 使用 AtomicReference 管理快照，实现无锁操作。*/
    private final AtomicReference<BufferPoolSnapshot> bufferPoolSnapshot
        = new AtomicReference<>(BufferPoolSnapshot.empty());

    /** 快照计数器，忽略前两次的快照数据。*/
    private final AtomicInteger snapshotInitCount
        = new AtomicInteger(0);

    /** 存储上一次有效地计算结果的原子引用。*/
    private final
    AtomicReference<CacheHitRate> previousResult
        = new AtomicReference<>(CacheHitRate.zero());

    private @NotNull Mono<BufferPoolSnapshot>
    fetchBufferPoolIndicator()
    {
        return
        this.globalStatusQuery
            .getGlobalStatus(GlobalStatusName.INNODB_STATUS)
            .map((indicator) ->
                BufferPoolSnapshot.builder()
                    .readAhead((Long) indicator.get("Innodb_buffer_pool_read_ahead"))
                    .readRequests((Long) indicator.get("Innodb_buffer_pool_read_requests"))
                    .reads((Long) indicator.get("Innodb_buffer_pool_reads"))
                    .build()
            );
    }

    /** 错误处理逻辑，输出异常信息并返回降级值。*/
    private @NotNull Mono<InnodbBufferCacheHitRate>
    errorHandler(Throwable throwable)
    {
        log.error(
            "InnoDB cache hit calculation error. Caused by: {}",
            throwable.getMessage(), throwable
        );

        return
        Mono.just(InnodbBufferCacheHitRate.onError());
    }

    private @NotNull CacheHitRate
    calculate(@NotNull BufferPoolSnapshot previous, BufferPoolSnapshot current)
    {
        // 计算两次快照的时间差
        long timeDiff = previous.getTimeDiffMills(current);

        // 如果时间差小于 MIN_TIME_DIFF_MS 毫秒，直接返回空结果
        if (timeDiff < MIN_TIME_DIFF_MS) {
            return CacheHitRate.zero();
        }

        // 若检查到指标被外部重置，返回重置空结果
        if (previous.isReset(current)) {
            return CacheHitRate.reset();
        }

        // 两次查询的 INNODB_BUFFER_POOL_READS 差值
        long readsDiff = current.getReads() - previous.getReads();

        // 两次查询的 INNODB_BUFFER_POOL_READ_AHEAD 差值
        long readAheadDiff = current.getReadAhead() - previous.getReadAhead();

        // 两次查询的 INNODB_BUFFER_POOL_READ_REQUESTS 差值
        long readRequestDiff = current.getReadRequests() - previous.getReadRequests();

        // 若查询间隔过短，或者数据库闲置，readRequestDiff 的值可能为 0，
        // 若真是如此，直接返回上一次的有效计算结果，
        // 但使用当前的查询间隔并告知使用了历史数据
        if (readRequestDiff <= 0L)
        {
            CacheHitRate lastHitRate = this.previousResult.get();

            return CacheHitRate.of(
                lastHitRate.getCacheHitRate(),
                timeDiff,
                lastHitRate.isResetDetected(),
                true
            );
        }

        /*
         * InnoDB 缓存命中率计算公式 =
         *   1 - (两次查询的 INNODB_BUFFER_POOL_READS 差值 + 两次查询的 INNODB_BUFFER_POOL_READ_AHEAD 差值) /
         *   两次查询的 INNODB_BUFFER_POOL_READ_REQUESTS 差值
         */
        BigDecimal missRate
            = BigDecimal.valueOf(readsDiff + readAheadDiff)
                .divide(BigDecimal.valueOf(readRequestDiff), 8, RoundingMode.HALF_UP);

        // 确保命中率在 [0, 1] 范围内
        BigDecimal cacheHitRate
            = BigDecimal.ONE.subtract(missRate)
                .min(BigDecimal.ONE)
                .max(BigDecimal.ZERO);

        return
        CacheHitRate.of(
            cacheHitRate, timeDiff,
            false, false
        );
    }

    private InnodbBufferCacheHitRate
    calculateAndUpdate(BufferPoolSnapshot currentSnapshot)
    {
        int retries = 0;

        BufferPoolSnapshot previousSnapshot;

        do {
            previousSnapshot = this.bufferPoolSnapshot.get();

            if (snapshotInitCount.get() < IGNORE_SNAPSHOTS)
            {
                if (this.bufferPoolSnapshot.compareAndSet(previousSnapshot, currentSnapshot))
                {
                    snapshotInitCount.getAndIncrement();

                    return
                    InnodbBufferCacheHitRate.buildZeroRate();
                }
            }
            else
            {
                if (this.bufferPoolSnapshot.compareAndSet(previousSnapshot, currentSnapshot))
                {
                    CacheHitRate hitRate = this.calculate(previousSnapshot, currentSnapshot);

                    this.previousResult.set(hitRate);

                    return
                    InnodbBufferCacheHitRate
                        .buildResult(hitRate);
                }
            }

            ++retries;
        }
        while (retries < MAX_RETRIES);


        // 如果连着尝试 10 回都发现不一致，
        // 说明竞争过于激烈了，输出警告日志并返回降级值
        log.warn(
            "Failed to update InnoDB buffer pool hit cache after {} retries",
            MAX_RETRIES
        );

        return
        InnodbBufferCacheHitRate.onError();
    }

    @Override
    public Mono<InnodbBufferCacheHitRate>
    calculateBufferCacheHitRate()
    {
        return
        this.fetchBufferPoolIndicator()
            .map(this::calculateAndUpdate)
            .onErrorResume(this::errorHandler)
            .subscribeOn(Schedulers.boundedElastic());
    }

    @Value(staticConstructor = "of")
    public static class CacheHitRate
    {
        BigDecimal  cacheHitRate;
        long        queryDiff;
        boolean     resetDetected;
        boolean     usedPreviousResult;  // 标记是否使用了历史数据

        @Contract(" -> new")
        public static @NotNull
        CacheHitRate zero()
        {
            return new
            CacheHitRate(
                BigDecimal.ZERO,
                0L, false, false
            );
        }

        @Contract(" -> new")
        public static @NotNull
        CacheHitRate reset()
        {
            return new
            CacheHitRate(
                BigDecimal.ZERO,
                0L, true, false
            );
        }
    }
}