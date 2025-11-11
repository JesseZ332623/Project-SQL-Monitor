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

/** MySQL QPS 计算器实现。*/
@Slf4j
@Component
@RequiredArgsConstructor
public class QPSCounterImpl implements QPSCounter
{
    /** MySQL 全局状态查询器。*/
    private final GlobalStatusQuery globalStatusQuery;

    /** 使用 AtomicReference 管理总查询快照，实现无锁操作。*/
    private final
    AtomicReference<QueriesSnapshot> queriesSnapshot
        = new AtomicReference<>(QueriesSnapshot.empty());

    /** 快照计数器，忽略前两次的快照数据（避免头几次 QPS 计算值虚高）。*/
    private final AtomicInteger snapshotInitCount
        = new AtomicInteger(0);

    /**
     * 根据两张总查询数快照，计算此刻本数据库的 QPS（保留 8 位小数且四舍五入）。
     *
     * <pre>公式：(上一次总查询数 - 本次总查询数) / 两张总查询数快照的时间差（单位：秒）</pre>
     */
    private QPS
    calculate(@NotNull QueriesSnapshot previous, QueriesSnapshot current)
    {
        // 计算两次快照的时间差
        long timeDiff = previous.getTimeDiffMills(current);

        // 计算两次快照的总查询差
        long queriesDiff = current.getQueries() - previous.getQueries();

        // 如果时间差小于 MIN_TIME_DIFF_MS 毫秒，直接返回空结果
        if (timeDiff < MIN_TIME_DIFF_MS) {
            return QPS.zero();
        }

        // 若检查到指标被外部重置，返回重置结果
        if (previous.isReset(current)) {
            return QPS.reset();
        }

        // 正式的计算 QPS
        BigDecimal qps
            = BigDecimal.valueOf(queriesDiff)
                  .divide(
                      BigDecimal.valueOf(timeDiff / 1000.00),
                      8, RoundingMode.HALF_UP
                  );

        // 构建 QPS 实例
        return
        QPS.of(
            qps, current.getQueries(),
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
                (Long) status.get(GlobalStatusName.QUERIES.getStatusName()))
            .map(QueriesSnapshot::of);
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

            if (retries > 0 )
            {
                // 可以考虑调用该方法优化自旋循环
                //（虽然在低频调用下根本用不到就是了）
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
        QPS zero()
        {
            return new
            QPS(BigDecimal.ZERO,
            0L, 0L, 0L,
            false
            );
        }

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