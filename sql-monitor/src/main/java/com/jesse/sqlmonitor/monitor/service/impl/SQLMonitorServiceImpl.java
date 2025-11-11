package com.jesse.sqlmonitor.monitor.service.impl;

import com.jesse.sqlmonitor.properties.R2dbcMasterProperties;
import com.jesse.sqlmonitor.monitor.constants.GlobalStatusName;
import com.jesse.sqlmonitor.monitor.MySQLIndicatorsRepository;
import com.jesse.sqlmonitor.constants.QueryOrder;
import com.jesse.sqlmonitor.monitor.constants.SizeUnit;
import com.jesse.sqlmonitor.response_body.*;
import com.jesse.sqlmonitor.monitor.service.SQLMonitorService;
import io.github.jessez332623.reactive_response_builder.ReactiveResponseBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.time.Duration;

import static com.jesse.sqlmonitor.utils.SQLMonitorUtils.*;
import static io.github.jessez332623.reactive_response_builder.utils.URLParamPrase.praseRequestParam;

/** SQL 指标监控程序服务实现。*/
@Slf4j
@Service
@RequiredArgsConstructor
public class SQLMonitorServiceImpl implements SQLMonitorService
{
    /** 主数据库连接属性。*/
    private final R2dbcMasterProperties properties;

    /** MySQL 指标数据查询仓储类接口。*/
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

    /** 获取数据库的地址和端口号。*/
    @Override
    public Mono<ServerResponse>
    getDatabaseAddress(ServerRequest request)
    {
        return
        ReactiveResponseBuilder
            .OK(properties.getHost() + ":" + properties.getPort(), null)
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
            .flatMap((qpsResult) ->
                ReactiveResponseBuilder.OK(qpsResult, null))
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
                if (isEmptyParam(param)) { return SizeUnit.KB; }
                return SizeUnit.valueOf(param);
            })
            .flatMap((unit) ->
                this.mySQLIndicatorsRepository
                    .getNetWorkTraffic(unit)
                    .flatMap((networkTraffic) ->
                        ReactiveResponseBuilder.OK(networkTraffic, null)
                    )
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
            .flatMap((connectionUsage) ->
               ReactiveResponseBuilder.OK(connectionUsage, null)
            )
            .onErrorResume(this::genericErrorHandle);
    }

    /** 查询数据库大小服务的接口。*/
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
            .flatMap((innodbBufferCacheHitRate) ->
                    ReactiveResponseBuilder.OK(innodbBufferCacheHitRate, null))
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

                long[] runtimeArray = new long[4];

                runtimeArray[0] = runDuration.toDays();
                runtimeArray[1] = runDuration.toHoursPart();
                runtimeArray[2] = runDuration.toMinutesPart();
                runtimeArray[3] = runDuration.toSecondsPart();

                return runtimeArray;
            }).flatMap((runtimeArray) ->
                ReactiveResponseBuilder.OK(runtimeArray, null)
            );
    }
}