package com.jesse.sqlmonitor.monitor.rsocket.service;

import com.jesse.sqlmonitor.monitor.constants.SizeUnit;
import com.jesse.sqlmonitor.response_body.ConnectionUsage;
import com.jesse.sqlmonitor.response_body.InnodbBufferCacheHitRate;
import com.jesse.sqlmonitor.response_body.NetWorkTraffic;
import com.jesse.sqlmonitor.response_body.QPSResult;
import reactor.core.publisher.Mono;

/** SQL 指标监视流服务接口。*/
public interface SQLMonitorStreamService
{
    /** 获取 QPS 指标服务接口。*/
    Mono<QPSResult> getQPSIndicator();

    /** 获取数据库服务器网络流量接口。*/
    Mono<NetWorkTraffic>
    getNetWorkTraffic(SizeUnit sizeUnit);

    /** 获取数据库连接使用率相关数据服务的接口。*/
    Mono<ConnectionUsage> getConnectionUsage();

    /** 查询 InnoDB 缓存命中率服务的接口。*/
    Mono<InnodbBufferCacheHitRate> getInnodbBufferCacheHitRate();
}