package com.jesse.sqlmonitor.monitor.rsocket.controller;

import com.jesse.sqlmonitor.monitor.constants.SizeUnit;
import com.jesse.sqlmonitor.response_body.ConnectionUsage;
import com.jesse.sqlmonitor.response_body.InnodbBufferCacheHitRate;
import com.jesse.sqlmonitor.response_body.NetWorkTraffic;
import com.jesse.sqlmonitor.response_body.QPSResult;
import com.jesse.sqlmonitor.monitor.rsocket.contants.TimeInterval;
import com.jesse.sqlmonitor.monitor.rsocket.service.SQLMonitorStreamService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Flux;
import reactor.util.retry.Retry;

import java.time.Duration;

/** SQL 指标监视流控制器类。*/
@Slf4j
@Controller
@RequiredArgsConstructor
public class SQLMonitorController
{
    /** SQL 指标监视流服务接口。*/
    private final
    SQLMonitorStreamService sqlMonitorStreamService;

    private Duration
    makeTimeInterval(String timeInterval) throws IllegalArgumentException
    {
        return
        TimeInterval.valueOf("_" + timeInterval + "_SECONDS")
                    .getInterval();
    }

    /**
     * 获取 QPS 指标流服务。
     * wss//address:port/sql-indicator-stream/qps/{interval}
     */
    @Operation(
        tags        = { "指标流服务" },
        summary     = "获取 QPS 指标流服务",
        description = "获取 QPS 指标流服务（通过 WebSocket 协议传输）",
        operationId = "getQPSIndicatorStream"
    )
    @MessageMapping("qps/{interval}")
    public Flux<QPSResult>
    getQPSIndicatorStream(
        @DestinationVariable("interval") String timeInterval
    )
    {
        return
        Flux.interval(this.makeTimeInterval(timeInterval))
            .onBackpressureDrop(tick ->
                log.warn("(QPS Indicator Stream) Dropped tick due to backpressure."))
            .concatMap(ignore ->
                this.sqlMonitorStreamService.getQPSIndicator()) // 顺序执行，避免并发
            .doOnError(error ->
                log.error("(QPS Indicator Stream) Stream terminated", error))
            .retryWhen(Retry.backoff(3, Duration.ofSeconds(1)))
            .doFinally(signal ->
                log.info("(QPS Indicator Stream) Stream completed with: {}", signal))
            .onErrorResume((error) -> {
                log.error("{}", error.getMessage());
                return Flux.error(error);
            });
    }

    /**
     * 获取数据库网络流量指标流服务。
     * wss//address:port/sql-indicator-stream/network-traffic/{unit}/{interval}
     */
    @Operation(
        tags        = { "指标流服务" },
        summary     = "获取数据库网络流量指标流",
        description = "获取数据库网络流量指标流（通过 WebSocket 协议传输）",
        operationId = "getNetWorkTrafficStream"
    )
    @MessageMapping("network-traffic/{unit}/{interval}")
    public Flux<NetWorkTraffic>
    getNetWorkTrafficStream(
        @DestinationVariable("unit")     String unitParam,
        @DestinationVariable("interval") String timeInterval
    )
    {
        return
        Flux.interval(this.makeTimeInterval(timeInterval))
            .onBackpressureDrop(tick ->
                log.debug("(NetWork Traffic Stream) Dropped tick due to backpressure."))
            .concatMap((ignore) ->
                this.sqlMonitorStreamService
                    .getNetWorkTraffic(SizeUnit.valueOf(unitParam)))
            .doOnError(error ->
                log.error("(NetWork Traffic Stream) Stream terminated", error))
            .retryWhen(Retry.backoff(3, Duration.ofSeconds(1)))
            .doFinally(signal ->
                log.info("(NetWork Traffic Stream) Stream completed with: {}", signal))
            .onErrorResume((error) -> {
                // 出现参数枚举解析错误等控制器这一层的错误时，
                // 不要启动流，而是将错误包装后直接返回
                log.error("{}", error.getMessage());
                return Flux.error(error);
            });
    }

    /**
     * 获取数据库连接使用率指标流服务。
     * wss://address:port/sql-indicator-stream/connection-usage/{interval}
     */
    @Operation(
        tags        = { "指标流服务" },
        summary     = "获取数据库连接使用率指标流",
        description = "获取数据库连接使用率指标流（通过 WebSocket 协议传输）",
        operationId = "getConnectionUsageStream"
    )
    @MessageMapping("connection-usage/{interval}")
    public Flux<ConnectionUsage>
    getConnectionUsageStream(
        @DestinationVariable("interval") String timeInterval
    )
    {
        return
        Flux.interval(this.makeTimeInterval(timeInterval))
            .onBackpressureDrop(tick ->
                log.debug("(Connection Usage Stream) Dropped tick due to backpressure.")
            )
            .concatMap((ignore) ->
                 this.sqlMonitorStreamService.getConnectionUsage())
            .doOnError(error ->
                log.error("(Connection Usage Stream) Stream terminated", error))
            .retryWhen(Retry.backoff(3, Duration.ofSeconds(1)))
            .doFinally(signal ->
                log.info("(Connection Usage Stream) Stream completed with: {}", signal))
            .onErrorResume((error) -> {
                log.error("{}", error.getMessage());
                return Flux.error(error);
            });
    }

    /**
     * 获取数据库 InnoDB 缓冲区命中率指标流服务。
     * wss://address:port/sql-indicator-stream/cache-hit-rate/{interval}
     */
    @Operation(
        tags        = { "指标流服务" },
        summary     = "获取数据库 InnoDB 缓冲区命中率指标流",
        description = "获取数据库 InnoDB 缓冲区命中率指标流（通过 WebSocket 协议传输）",
        operationId = "getInnodbBufferCacheHitRateStream"
    )
    @MessageMapping("cache-hit-rate/{interval}")
    public Flux<InnodbBufferCacheHitRate>
    getInnodbBufferCacheHitRateStream(
        @DestinationVariable("interval") String timeInterval
    )
    {
        return
        Flux.interval(this.makeTimeInterval(timeInterval))
            .onBackpressureDrop(tick ->
                log.debug(
                    "(Innodb Buffer Cache HitRate Stream)" +
                     "Dropped tick due to backpressure.")
            )
            .concatMap((ignore) ->
                this.sqlMonitorStreamService.getInnodbBufferCacheHitRate())
            .doOnError(error ->
                log.error("(Innodb Buffer Cache HitRate Stream) Stream terminated", error))
            .retryWhen(Retry.backoff(3, Duration.ofSeconds(1)))
            .doFinally(signal ->
                log.info("(Innodb Buffer Cache HitRate Stream) Stream completed with: {}", signal))
            .onErrorResume((error) -> {
                log.error("{}", error.getMessage());
                return Flux.error(error);
            });
    }
}