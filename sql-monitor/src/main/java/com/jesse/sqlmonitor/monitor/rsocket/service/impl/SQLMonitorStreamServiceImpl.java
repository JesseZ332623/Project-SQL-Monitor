package com.jesse.sqlmonitor.monitor.rsocket.service.impl;

import com.jesse.sqlmonitor.indicator_record.service.IndicatorSender;
import com.jesse.sqlmonitor.monitor.MySQLIndicatorsRepository;
import com.jesse.sqlmonitor.monitor.constants.SizeUnit;
import com.jesse.sqlmonitor.properties.R2dbcMasterProperties;
import com.jesse.sqlmonitor.response_body.*;
import com.jesse.sqlmonitor.monitor.rsocket.service.SQLMonitorStreamService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import java.time.Duration;
import java.time.LocalDateTime;

/** SQL 指标监视流服务实现。*/
@Slf4j
@Service
@RequiredArgsConstructor
public class SQLMonitorStreamServiceImpl implements SQLMonitorStreamService
{
    /** 数据库连接属性。*/
    private final R2dbcMasterProperties properties;

    /** 指标数据发送器接口。*/
    private final IndicatorSender indicatorSender;

    /** MySQL 指标数据查询仓储类接口。*/
    private final
    MySQLIndicatorsRepository mySQLIndicatorsRepository;

    /** 获取数据库服务器的 IP 地址 + 端口号。*/
    private @NotNull String
    getDatabaseAddress() {
        return properties.getHost() + ":" + properties.getPort();
    }

    @Override
    public @NotNull Mono<QPSResult> getQPSIndicator()
    {
        return
        this.mySQLIndicatorsRepository.getQPS()
            .timeout(Duration.ofSeconds(8L))
            .flatMap((qpsResult) -> {
                if (qpsResult.isValid())  // 只将有效的 QPS 结果发送给 RabbitMQ
                {
                    SentIndicator<QPSResult> qpsIndicator
                        = new SentIndicator<>(LocalDateTime.now(), this.getDatabaseAddress(), qpsResult);

                            return
                            this.indicatorSender
                                .sendIndicator(qpsIndicator)
                                .thenReturn(qpsResult);
                        }
                        else {
                            return Mono.just(qpsResult);
                        }
                    })
            .onErrorResume((error) -> {
                //  处理错误时，不可以直接抛出异常，
                //  不然整个流会终止，这里输出日志并返回降级值
                log.error(
                    "Fetch QPS indicator occurring error, Caused by: {}",
                    error.getMessage()
                );

                return Mono.just(QPSResult.onError());
            });
    }

    @Override
    public Mono<NetWorkTraffic>
    getNetWorkTraffic(SizeUnit sizeUnit)
    {
        return
        this.mySQLIndicatorsRepository
            .getNetWorkTraffic(sizeUnit)
            .timeout(Duration.ofSeconds(8L))
            .flatMap((networkTraffic) -> {
                if (networkTraffic.isValid())
                {
                    SentIndicator<NetWorkTraffic> trafficIndicator
                        = new SentIndicator<>(
                        LocalDateTime.now(),
                        getDatabaseAddress(),
                        networkTraffic
                    );

                    return
                    this.indicatorSender
                        .sendIndicator(trafficIndicator)
                        .thenReturn(networkTraffic);
                }
                else {
                    return Mono.just(networkTraffic);
                }
            })
            .onErrorResume((error) -> {
                log.error(
                    "Fetch network indicator occurring error, Caused by: {}",
                    error.getMessage()
                );

                return Mono.just(NetWorkTraffic.onError());
            });
    }

    @Override
    public Mono<ConnectionUsage>
    getConnectionUsage()
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
                    .thenReturn(connectionUsage);
            })
            .onErrorResume((error) -> {
                log.error(
                    "Fetch connection usage indicator occurring error, Caused by: {}",
                    error.getMessage()
                );

                return Mono.just(new ConnectionUsage());
            });
    }

    @Override
    public Mono<InnodbBufferCacheHitRate>
    getInnodbBufferCacheHitRate()
    {
        return
        this.mySQLIndicatorsRepository
            .getInnodbBufferCacheHitRate()
                .flatMap((innodbBufferCacheHitRate) -> {
                    if (innodbBufferCacheHitRate.isValid())
                    {
                        SentIndicator<InnodbBufferCacheHitRate> cacheHitRateIndicator
                            = new SentIndicator<>(
                                LocalDateTime.now(), getDatabaseAddress(),
                                innodbBufferCacheHitRate
                        );

                        return
                        this.indicatorSender
                            .sendIndicator(cacheHitRateIndicator)
                            .thenReturn(innodbBufferCacheHitRate);
                    }
                    else {
                        return Mono.just(innodbBufferCacheHitRate);
                    }
                })
            .onErrorResume((error) -> {
                log.error(
                    "Fetch innodb buffer cache hit rate indicator occurring error, Caused by: {}",
                    error.getMessage()
                );

                return Mono.just(InnodbBufferCacheHitRate.onError());
            });
    }
}