package com.jesse.sqlmonitor.monitor.impl.network_traffic.impl;

import com.jesse.sqlmonitor.monitor.impl.network_traffic.NetWorkTrafficCounter;
import com.jesse.sqlmonitor.response_body.NetWorkTraffic;
import com.jesse.sqlmonitor.monitor.constants.SizeUnit;
import com.jesse.sqlmonitor.monitor.impl.GlobalStatusQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static com.jesse.sqlmonitor.monitor.constants.MonitorConstants.*;
import static com.jesse.sqlmonitor.monitor.constants.GlobalStatusName.BYTES_RECEIVED;
import static com.jesse.sqlmonitor.monitor.constants.GlobalStatusName.BYTES_SENT;
import static com.jesse.sqlmonitor.utils.SQLMonitorUtils.extractLongValue;

/** MySQL 服务器网络流量计算器实现。*/
@Slf4j
@Component
@RequiredArgsConstructor
public class NetWorkTrafficCounterImpl implements NetWorkTrafficCounter
{
    /** 全局状态查询器。*/
    private final GlobalStatusQuery globalStatusQuery;

    /** 网络流量计算器。*/
    private final TrafficRateCalculator trafficRateCalculator;

    /** 网络流量快照，使用 {@link AtomicReference} 实现无锁操作。*/
    private final AtomicReference<TrafficStateSnapshot> lastState
        = new AtomicReference<>(TrafficStateSnapshot.empty());

    /** 快照计数器，忽略前两次的快照数据（避免头几次 网络流量 计算值虚高）。*/
    private final AtomicInteger snapshotInitCount
        = new AtomicInteger(0);

    /** 错误处理逻辑，输出异常信息并返回降级值。*/
    private @NotNull Mono<NetWorkTraffic>
    errorHandler(Throwable throwable)
    {
        log.error(
            "Network traffic calculation error. Caused by: {}",
            throwable.getMessage(), throwable
        );

        return Mono.just(NetWorkTraffic.onError());
    }

    /** 获取当前网络流量快照。*/
    private @NotNull Mono<TrafficStateSnapshot>
    fetchCurrentTrafficState()
    {
        return
        Mono.zip(
            this.globalStatusQuery.getGlobalStatus(BYTES_SENT),
            this.globalStatusQuery.getGlobalStatus(BYTES_RECEIVED)
        ).map((indicators) -> {
            long bytesSent
                = extractLongValue(indicators.getT1(), BYTES_SENT.getStatusName());
            long bytesReceived
                = extractLongValue(indicators.getT2(), BYTES_RECEIVED.getStatusName());

            return new
            TrafficStateSnapshot(bytesSent, bytesReceived);
        });
    }

    private @NotNull Mono<NetWorkTraffic>
    calculateTrafficRate(TrafficStateSnapshot currentState, SizeUnit unit)
    {
        return
        Mono.fromCallable(() -> {
            int retries = 0;

            if (this.snapshotInitCount.get() < IGNORE_SNAPSHOTS)
            {
                this.lastState.getAndSet(currentState);
                this.snapshotInitCount.getAndIncrement();

                return
                NetWorkTraffic.buildZeroRate();
            }

            do
            {
                TrafficStateSnapshot previousState = this.lastState.get();

                if (this.lastState.compareAndSet(previousState, currentState))
                {
                    return
                    NetWorkTraffic.buildTrafficResult(
                        currentState,
                        this.trafficRateCalculator
                            .calculateRate(previousState, currentState, unit)
                    );
                }

                ++retries;
            }
            while (retries < MAX_RETRIES);

            // 如果连着尝试 10 回都发现不一致，
            // 说明竞争过于激烈了，输出警告日志并返回降级值
            log.warn("Failed to update QPS after {} retries.", MAX_RETRIES);

            return NetWorkTraffic.onError();
        });
    }

    /** 计算此刻数据库的网络流量（支持不同的计量单位）*/
    @Override
    public Mono<NetWorkTraffic>
    calculateNetWorkTraffic(SizeUnit unit)
    {
        return
        this.fetchCurrentTrafficState()
            .flatMap((currentState) ->
                this.calculateTrafficRate(currentState, unit))
            .onErrorResume(this::errorHandler)
            .subscribeOn(Schedulers.parallel());
    }
}