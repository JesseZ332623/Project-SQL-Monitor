package com.jesse.sqlmonitor.monitor;

import com.jesse.sqlmonitor.response_body.*;
import com.jesse.sqlmonitor.monitor.constants.GlobalStatusName;
import com.jesse.sqlmonitor.constants.QueryOrder;
import com.jesse.sqlmonitor.monitor.constants.SizeUnit;
import com.jesse.sqlmonitor.response_body.QPSResult;
import org.jetbrains.annotations.NotNull;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/** MySQL 指标数据查询仓储类接口类。*/
public interface MySQLIndicatorsRepository
{
    /** 获取每秒查询频率（QPS）。*/
    Mono<QPSResult> getQPS();

    /** 获取服务器接收 / 发送数据量相关信息。*/
    Mono<NetWorkTraffic> getNetWorkTraffic(SizeUnit unit);

    /** 查询指定全局状态。*/
    Mono<Map<String, Object>>
    getGlobalStatus(@NotNull GlobalStatusName statusName);

    /** 查询连接使用率。*/
    Mono<ConnectionUsage> getConnectionUsage();

    /** 查询所有数据库名（可选是否包括系统数据库）。*/
    Mono<List<String>>
    getAllSchemaName(boolean includeSysShema);

    /** 查询指定数据库和它的所有表大小（支持按数据表大小排序）。*/
    Mono<Map<String, DatabaseSize>>
    getDatabaseSize(String schemaName, QueryOrder queryOrder);

    /** 查询 InnoDB 缓存命中率。*/
    Mono<InnodbBufferCacheHitRate> getInnodbBufferCacheHitRate();
}