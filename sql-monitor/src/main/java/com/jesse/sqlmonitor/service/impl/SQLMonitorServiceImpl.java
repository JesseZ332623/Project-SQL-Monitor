package com.jesse.sqlmonitor.service.impl;

import com.jesse.sqlmonitor.properties.R2dbcMasterProperties;
import com.jesse.sqlmonitor.monitor.constants.GlobalStatusName;
import com.jesse.sqlmonitor.monitor.MySQLIndicatorsRepository;
import com.jesse.sqlmonitor.constants.QueryOrder;
import com.jesse.sqlmonitor.monitor.constants.SizeUnit;
import com.jesse.sqlmonitor.response_body.*;
import com.jesse.sqlmonitor.response_body.qps.QPSResult;
import com.jesse.sqlmonitor.response_builder.ReactiveResponseBuilder;
import com.jesse.sqlmonitor.service.SQLMonitorService;
import com.jesse.sqlmonitor.indicator_record.service.IndicatorSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDateTime;

import static com.jesse.sqlmonitor.response_builder.utils.URLParamPrase.praseRequestParam;
import static com.jesse.sqlmonitor.utils.SQLMonitorUtils.*;

/** SQL 指标监控程序服务实现。*/
@Slf4j
@Service
@RequiredArgsConstructor
public class SQLMonitorServiceImpl implements SQLMonitorService
{
    /** 数据库连接属性。*/
    private final R2dbcMasterProperties properties;

    /** 指标数据发送器接口。*/
    private final IndicatorSender indicatorSender;

    private final
    MySQLIndicatorsRepository mySQLIndicatorsRepository;

    /** 本服务类通用的错误处理方法。*/
    private Mono<ServerResponse>
    genericErrorHandle(Throwable throwable)
    {
        switch (throwable)
        {
            case IllegalArgumentException illegalArgument -> {
                return
                ReactiveResponseBuilder.BAD_REQUEST(illegalArgument.getMessage(), illegalArgument);
            }
            case Exception exception -> {
                return
                ReactiveResponseBuilder.INTERNAL_SERVER_ERROR(exception.getMessage(), exception);
            }
            case null, default -> {
                return
                ReactiveResponseBuilder.INTERNAL_SERVER_ERROR(
                    new RuntimeException("Unknown exception").getMessage(), null
                );
            }
        }
    }

    private @NotNull String
    getDatabaseAddress()
    {
        return
        properties.getHost() + ":" + properties.getPort();
    }

    /** 获取数据库的地址和端口号。*/
    @Override
    public Mono<ServerResponse>
    getDatabaseAddress(ServerRequest request)
    {
        final String address
            = properties.getHost() + ":" + properties.getPort();

        return
        ReactiveResponseBuilder
            .OK(address, null)
            .onErrorResume(this::genericErrorHandle);
    }

    /** 获取本数据库 QPS 的服务接口。*/
    @Override
    public Mono<ServerResponse>
    getQPS(ServerRequest request)
    {
        return
        this.mySQLIndicatorsRepository
            .getQPS()
            .flatMap((qpsResult) -> {
                SentIndicator<QPSResult> qpsIndicator
                    = new SentIndicator<>(
                        LocalDateTime.now(), getDatabaseAddress(), qpsResult
                    );

                return
                this.indicatorSender
                    .sendIndicator(qpsIndicator)
                    .then(ReactiveResponseBuilder.OK(qpsResult, null));
            })
            .onErrorResume(this::genericErrorHandle);
    }

    /** 获取服务器接收 / 发送数据量相关信息的服务的接口。*/
    @Override
    public Mono<ServerResponse>
    getNetWorkTraffic(ServerRequest request)
    {
        return
        praseRequestParam(request, "sizeUnit")
            .map((param) -> {
                if (isEmptyParam(param)) {
                    return SizeUnit.KB;
                }
                return SizeUnit.valueOf(param);
            })
            .flatMap((unit) ->
                this.mySQLIndicatorsRepository
                    .getNetWorkTraffic(unit)
                    .flatMap((networkTraffic) -> {
                        SentIndicator<NetWorkTraffic> trafficIndicator
                            = new SentIndicator<>(
                                LocalDateTime.now(),
                                getDatabaseAddress(),
                                networkTraffic
                            );

                        return
                        this.indicatorSender
                            .sendIndicator(trafficIndicator)
                            .then(ReactiveResponseBuilder.OK(networkTraffic, null));
                    })
            )
            .onErrorResume(this::genericErrorHandle);
    }

    /** 查询本数据库指定全局状态的接口。*/
    @Override
    public Mono<ServerResponse>
    getGlobalStatus(ServerRequest request)
    {
        return
        praseRequestParam(request, "statusName")
            .map(GlobalStatusName::valueOf)
            .flatMap(this.mySQLIndicatorsRepository::getGlobalStatus)
            .flatMap((status) ->
                ReactiveResponseBuilder.OK(status, null))
            .onErrorResume(this::genericErrorHandle);
    }

    /** 获取数据库连接使用率相关数据的接口。*/
    @Override
    public Mono<ServerResponse>
    getConnectionUsage(ServerRequest request)
    {
        return
        this.mySQLIndicatorsRepository
            .getConnectionUsage()
            .flatMap((connectionUsage) -> {
                SentIndicator<ConnectionUsage>
                    connectionUsageIndicator = new SentIndicator<>(
                        LocalDateTime.now(),
                        getDatabaseAddress(),
                        connectionUsage
                    );


                return
                this.indicatorSender
                    .sendIndicator(connectionUsageIndicator)
                    .then(ReactiveResponseBuilder.OK(connectionUsage, null));
            })
            .onErrorResume(this::genericErrorHandle);
    }

    /** 获取本数据库所有库的大小的接口。*/
    @Override
    public Mono<ServerResponse>
    getDatabaseSize(ServerRequest request)
    {
        return
        Mono.zip(
            praseRequestParam(request, "schemaName"),
            praseRequestParam(request, "order")
        )
        .flatMap((params) -> {
            final String schemaName
                = isNotValidSchemaName(params.getT1());

            final QueryOrder order
                = isEmptyParam(params.getT2())
                    ? QueryOrder.DESC
                    : QueryOrder.valueOf(params.getT2());

            return
            this.mySQLIndicatorsRepository
                .getDatabaseSize(schemaName, order);
        })
        .flatMap((databaseSizeMap) ->
            ReactiveResponseBuilder.OK(databaseSizeMap, null))
        .onErrorResume(this::genericErrorHandle);
    }

    /** 查询 InnoDB 缓存命中率服务的接口。*/
    public Mono<ServerResponse>
    getInnodbBufferCacheHitRate(ServerRequest request)
    {
        return
        this.mySQLIndicatorsRepository
            .getInnodbBufferCacheHitRate()
            .flatMap((innodbBufferCacheHitRate) -> {

                SentIndicator<InnodbBufferCacheHitRate> cacheHitRateIndicator
                    = new SentIndicator<>(
                    LocalDateTime.now(), getDatabaseAddress(),
                    innodbBufferCacheHitRate
                );

                return
                this.indicatorSender
                    .sendIndicator(cacheHitRateIndicator)
                    .then(ReactiveResponseBuilder.OK(innodbBufferCacheHitRate, null));
            })
            .onErrorResume(this::genericErrorHandle);
    }

    /** 查询服务器运行时间服务的接口。*/
    @Override
    public Mono<ServerResponse>
    getServerUpTime(ServerRequest request)
    {
        return
        this.mySQLIndicatorsRepository
            .getGlobalStatus(GlobalStatusName.UPTIME)
            .map((status) -> {
                final Duration runDuration
                    = Duration.ofSeconds((Long) status.get("Uptime"));

                long days    = runDuration.toDays();
                long hours   = runDuration.toHoursPart();
                long minutes = runDuration.toMinutesPart();
                long seconds = runDuration.toSecondsPart();

                return
                days  + " day "   +
                hours + " hour "  +
                minutes + " min " +
                seconds + " sec";
            }).flatMap((timeString) ->
                ReactiveResponseBuilder.OK(timeString, null)
            );
    }
}