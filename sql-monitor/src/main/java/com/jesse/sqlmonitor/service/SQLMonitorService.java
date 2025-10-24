package com.jesse.sqlmonitor.service;

import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

/** SQL 指标监控程序服务接口。*/
public interface SQLMonitorService
{
    /** 获取数据库的地址和端口号。*/
    Mono<ServerResponse>
    getDatabaseAddress(ServerRequest request);

    /** 获取本数据库 QPS 的服务接口。*/
    Mono<ServerResponse>
    getQPS(ServerRequest request);

    /** 获取服务器接收 / 发送数据量相关信息的服务的接口。*/
    Mono<ServerResponse>
    getNetWorkTraffic(ServerRequest request);

    /** 查询本数据库指定全局状态服务的接口。*/
    Mono<ServerResponse>
    getGlobalStatus(ServerRequest request);

    /** 获取数据库连接使用率相关数据服务的接口。*/
    Mono<ServerResponse>
    getConnectionUsage(ServerRequest request);

    /** 查询所有数据库大小服务的接口。*/
    Mono<ServerResponse>
    getDatabaseSize(ServerRequest request);

    /** 查询 InnoDB 缓存命中率服务的接口。*/
    Mono<ServerResponse>
    getInnodbBufferCacheHitRate(ServerRequest request);

    /** 查询服务器运行时间服务的接口。*/
    Mono<ServerResponse>
    getServerUpTime(ServerRequest request);
}