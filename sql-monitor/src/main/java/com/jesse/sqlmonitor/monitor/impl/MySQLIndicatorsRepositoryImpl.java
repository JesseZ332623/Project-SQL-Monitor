package com.jesse.sqlmonitor.monitor.impl;

import com.jesse.sqlmonitor.monitor.cacher.IndicatorCacher;
import com.jesse.sqlmonitor.monitor.impl.connection_usage.ConnectionUsageCounter;
import com.jesse.sqlmonitor.monitor.impl.database_size.DatabaseSizeCounter;
import com.jesse.sqlmonitor.monitor.impl.innodb_cache_hit.InnoDBCacheHitCounter;
import com.jesse.sqlmonitor.monitor.impl.network_traffic.NetWorkTrafficCounter;
import com.jesse.sqlmonitor.monitor.impl.qps.QPSCounter;
import com.jesse.sqlmonitor.response_body.*;
import com.jesse.sqlmonitor.monitor.constants.GlobalStatusName;
import com.jesse.sqlmonitor.monitor.MySQLIndicatorsRepository;
import com.jesse.sqlmonitor.constants.QueryOrder;
import com.jesse.sqlmonitor.monitor.constants.SizeUnit;
import com.jesse.sqlmonitor.response_body.QPSResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.util.Map;

import static com.jesse.sqlmonitor.monitor.constants.IndicatorKeyNames.*;

/** MySQL 指标数据查询仓储类实现。*/
@Slf4j
@Repository
@RequiredArgsConstructor
public class MySQLIndicatorsRepositoryImpl implements MySQLIndicatorsRepository
{
    /** 全局状态查询器。*/
    private final GlobalStatusQuery globalStatusQuery;

    /** 数据库、数据表大小计算器。*/
    private final DatabaseSizeCounter databaseSizeCounter;

    /** QPS 计算器。*/
    private final QPSCounter qpsCounter;

    /** 数据库网络流量统计器。*/
    private final NetWorkTrafficCounter netWorkTrafficCounter;

    /** InnoDB 缓存命中率计算器。*/
    private final InnoDBCacheHitCounter innoDBCacheHitCounter;

    /** 数据库连接使用率计算器。*/
    private final ConnectionUsageCounter connectionUsageCounter;

    /** 指标数据缓存器。*/
    private final IndicatorCacher indicatorCacher;

    /** 查询所有数据库大小（支持按数据库大小排序）。*/
    @Override
    public Mono<Map<String, DatabaseSize>>
    getDatabaseSize(String schemaName, QueryOrder queryOrder)
    {
        return
        this.databaseSizeCounter
            .getDatabaseSizeInfo(schemaName, queryOrder);
    }

    /** 查询本数据库指定全局状态。*/
    @Override
    public Mono<Map<String, Object>>
    getGlobalStatus(@NotNull GlobalStatusName statusName)
    {
        return
        this.globalStatusQuery
            .getGlobalStatus(statusName);
    }

    /** 获取每秒查询频率（QPS）。*/
    @Override
    public Mono<QPSResult> getQPS()
    {
        return
        this.indicatorCacher
            .getIndicatorCacheWithLock(
                QPSResultKey,
                this.qpsCounter.calculateQPS(), QPSResult.class
            );
    }

    /** 获取服务器接收 / 发送数据量相关信息。*/
    @Override
    public Mono<NetWorkTraffic>
    getNetWorkTraffic(SizeUnit unit)
    {
        return
        this.indicatorCacher
            .getIndicatorCacheWithLock(
                NetWorkTrafficKey,
                this.netWorkTrafficCounter.calculateNetWorkTraffic(unit),
                NetWorkTraffic.class
            );
    }

    /** 查询连接使用率。*/
    @Override
    public Mono<ConnectionUsage> getConnectionUsage()
    {
        return
        this.indicatorCacher
            .getIndicatorCacheWithLock(
                ConnectionUsageKey,
                this.connectionUsageCounter.getConnectionUsage(),
                ConnectionUsage.class
            );
    }

    /** 查询 InnoDB 缓存命中率。*/
    @Override
    public Mono<InnodbBufferCacheHitRate>
    getInnodbBufferCacheHitRate()
    {
        return
        this.indicatorCacher
            .getIndicatorCacheWithLock(
                InnodbBufferCacheHitRateKey,
                this.innoDBCacheHitCounter.calculateBufferCacheHitRate(),
                InnodbBufferCacheHitRate.class
            );
    }
}