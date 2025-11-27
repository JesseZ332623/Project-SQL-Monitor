package com.jesse.sqlmonitor.monitor.impl.qps.impl;

import com.jesse.sqlmonitor.monitor.constants.GlobalStatusName;
import com.jesse.sqlmonitor.monitor.impl.GlobalStatusQuery;
import com.jesse.sqlmonitor.monitor.impl.qps.QPSCounter;
import com.jesse.sqlmonitor.response_body.QPSResult;
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

/** MySQL QPS 指标计算器实现。*/
@Slf4j
@Component
@RequiredArgsConstructor
public class QPSCounterImpl implements QPSCounter
{
    /** 加权平均平滑因子（代表历史数据的权重）。*/
    private static final BigDecimal
    SMOOTHING_FACTOR = BigDecimal.valueOf(0.70);

    /** 加权平均平滑因子（代表新数据的权重）。*/
    private static final BigDecimal
    SMOOTHING_FACTOR_CUR = BigDecimal.ONE.subtract(SMOOTHING_FACTOR);

    /** MySQL 全局状态查询器。*/
    private final GlobalStatusQuery globalStatusQuery;

    /** 使用 {@link AtomicReference} 管理总查询快照，实现无锁操作。*/
    private final
    AtomicReference<QueriesSnapshot> queriesSnapshot
        = new AtomicReference<>(QueriesSnapshot.empty());

    /** 上一次经过加权平均求和计算得出的 QPS 值。*/
    private final
    AtomicReference<BigDecimal> lastStableQPS
        = new AtomicReference<>(BigDecimal.ZERO);

    /** 快照计数器，忽略前两次的快照数据（避免头几次 QPS 计算值虚高）。*/
    private final AtomicInteger snapshotInitCount
        = new AtomicInteger(0);

    /**
     * 结合当前和上一次的 QPS 数据，进行加权平均求和，
     * 确保在缓存层失效时，涌入数据库层的计算请求不会造成 QPS 剧烈变化，
     * 而是平滑的变化。
     *
     * @param currentRawQPS 当前计算得出的原始 QPS
     *
     * @return 平滑处理后的 QPS 结果
     */
    private BigDecimal
    smoothQPS(BigDecimal currentRawQPS)
    {
        BigDecimal lastQPS = this.lastStableQPS.get();

        if (lastQPS.compareTo(BigDecimal.ZERO) == 0) {
            return currentRawQPS;
        }

        if (lastQPS.compareTo(currentRawQPS) == 0) {
            return currentRawQPS;
        }

        return
        lastQPS.multiply(SMOOTHING_FACTOR)
               .add(currentRawQPS.multiply(SMOOTHING_FACTOR_CUR));
    }

    /**
     * 根据两张总查询数快照，计算此刻本数据库的 QPS（保留 8 位小数且四舍五入）。
     *
     * <pre>公式：(本次总查询数 - 上一次总查询数) / 两张总查询数快照的时间差（单位：秒）</pre>
     */
    private QPS
    calculate(@NotNull QueriesSnapshot previous, QueriesSnapshot current)
    {
        // 计算两次快照的时间差
        long timeDiff = previous.getTimeDiffMills(current);

        // 如果时间差小于 MIN_TIME_DIFF_MS 毫秒，
        // 直接用 lastStableQPS 构造结果
        if (timeDiff < MIN_TIME_DIFF_MS)
        {
            return
            QPS.of(
                this.lastStableQPS.get(), current.getQueries(),
                0L, timeDiff, false
            );
        }

        // 若检查到指标被外部重置，返回重置结果
        if (previous.isReset(current)) {
            return QPS.reset();
        }

        // 计算两次快照的总查询差
        long queriesDiff = current.getQueries() - previous.getQueries();

        // 正式的计算 QPS
        BigDecimal rawQPS
            = BigDecimal.valueOf(queriesDiff)
                  .divide(
                      BigDecimal.valueOf(timeDiff / 1000.00),
                      8, RoundingMode.HALF_UP
                  );

        // 平滑处理 QPS 值并更新。
        BigDecimal smoothQPS = this.smoothQPS(rawQPS);
        this.lastStableQPS.set(smoothQPS);

        // 构建 QPS 实例
        return
        QPS.of(
            smoothQPS, current.getQueries(),
            queriesDiff, timeDiff, false
        );
    }

    /** 错误处理逻辑，输出异常信息并返回降级值。*/
    private @NotNull Mono<QPSResult>
    errorHandler(Throwable throwable)
    {
        log.error(
            "QPS calculation error. Caused by: {}",
            throwable.getMessage(), throwable
        );

        return Mono.just(QPSResult.onError());
    }

    /** 从数据库获取当前的总查询数，并构造成快照 {@link QueriesSnapshot}。*/
    private @NotNull Mono<QueriesSnapshot> fetchQueries()
    {
        return
        this.globalStatusQuery
            .getGlobalStatus(GlobalStatusName.QUERIES)
            .map((status) ->
                QueriesSnapshot.of(
                    (Long) status.get(GlobalStatusName.QUERIES.getStatusName())
                )
            );
    }

    /**
     * 计算 QPS 并更新总查询数快照。</br>
     * 使用 {@link AtomicReference#compareAndSet(Object, Object)} 确保
     * this.queriesSnapshot 和 previousQueries 完全相同时才更新快照并计算 QPS，
     * 本质上就是一个乐观锁的实现。
     */
    private QPSResult
    calculateAndUpdate(QueriesSnapshot currentQueries)
    {
        int retries = 0;

        if (this.snapshotInitCount.get() < IGNORE_SNAPSHOTS)
        {
            this.queriesSnapshot.getAndSet(currentQueries);

            this.snapshotInitCount.getAndIncrement();
            return QPSResult.buildZeroQPS();

        }

        /*
         * 之前使用 queriesSnapshot.getAndSet() 可能出现的竞争条件
         *（如果有多个线程同时调用 getAndSet()，会导致部分 QPS 计算不准确）
         */
        do {
            QueriesSnapshot previousQueries = this.queriesSnapshot.get();

            if (this.queriesSnapshot.compareAndSet(previousQueries, currentQueries))
            {
                return
                QPSResult.buildQPSResult(
                    this.calculate(previousQueries, currentQueries)
                );
            }

            if (retries > 0)
            {
                // 可以考虑调用该方法优化自旋循环
                Thread.onSpinWait();
            }

            ++retries;
        }
        while (retries < MAX_RETRIES);

        // 如果连着尝试 MAX_RETRY_TIMES 回都发现不一致，
        // 说明竞争过于激烈了输出警告日志并返回降级值
        log.warn("Failed to update QPS after {} retries.", MAX_RETRIES);

        return QPSResult.onError();
    }

    @Override
    public Mono<QPSResult> calculateQPS()
    {
        return
        this.fetchQueries()
            .map(this::calculateAndUpdate)
            .onErrorResume(this::errorHandler)
            .subscribeOn(Schedulers.parallel());
    }

    @Value(staticConstructor = "of")
    public static class QPS
    {
        /** QPS 具体值 */
        BigDecimal qps;

        /** 当前查询总数 */
        Long currentQuires;

        /** 与上一次获取的总查询数的差值 */
        Long queryDiff;

        /** 与上一次获取的总查询数的时间间隔（单位：毫秒）*/
        Long timeDiffMs;

        /** 总查询数是否被外部重置？*/
        boolean resetDetected;

        @Contract(" -> new")
        public static @NotNull
        QPS reset()
        {
            return new
            QPS(BigDecimal.ZERO,
            0L,0L, 0L,
            true
            );
        }
    }
}