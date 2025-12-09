package com.jesse.sqlmonitor.indicator_record.service;

import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

/** 监控日志数据操作服务接口。*/
public interface MonitorLogService
{
    /**
     * 读取某个时间点之前的指定 IP 下所有指定类型的监控日志记录
     * （按时间排序，支持分页查询）。
     */
    Mono<ServerResponse>
    fetchIndicatorLog(ServerRequest request);

    /** 计算某个时间点之前的指定 IP 下的 QPS 统计数据。*/
    Mono<ServerResponse>
    qpsStatistics(ServerRequest request);
}