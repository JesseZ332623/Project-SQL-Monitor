package com.jesse.sqlmonitor.scheduled_tasks.service;

import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

/** 手动执行某些定时任务的接口。*/
public interface ScheduledTaskService
{
    /** 手动的执行历史指标清除操作。*/
    Mono<ServerResponse>
    executeCleanIndicatorUtilLastWeek(ServerRequest request);

    /** 手动的执行例行指标数据报告发送的操作。*/
    Mono<ServerResponse>
    executeSendIntervalIndicatorReport(ServerRequest request);
}