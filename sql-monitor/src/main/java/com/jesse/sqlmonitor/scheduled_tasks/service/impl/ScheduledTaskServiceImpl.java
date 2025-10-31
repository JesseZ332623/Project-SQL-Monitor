package com.jesse.sqlmonitor.scheduled_tasks.service.impl;

import com.jesse.sqlmonitor.scheduled_tasks.HistoricalIndicatorCleaner;
import com.jesse.sqlmonitor.scheduled_tasks.IntervalIndicatorReporter;
import com.jesse.sqlmonitor.scheduled_tasks.exception.ScheduledTasksException;
import com.jesse.sqlmonitor.scheduled_tasks.service.ScheduledTaskService;
import io.github.jessez332623.reactive_response_builder.ReactiveResponseBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/** 手动执行定时任务服务实现。*/
@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduledTaskServiceImpl implements ScheduledTaskService
{
    /** 定时清历史指标数据的清理器。*/
    private final
    HistoricalIndicatorCleaner historicalIndicatorCleaner;

    /** 定时向运维人员发送指标报告发送器。*/
    private final
    IntervalIndicatorReporter intervalIndicatorReporter;

    @Override
    public Mono<ServerResponse>
    executeCleanIndicatorUtilLastWeek(ServerRequest request)
    {
        return
        this.historicalIndicatorCleaner
            .cleanIndicatorUtilLastWeekManually()
            .subscribeOn(Schedulers.boundedElastic())
            .flatMap((cleanUpRes) ->
                ReactiveResponseBuilder.OK(
                    cleanUpRes,
                    "Executing task CleanIndicatorUtilLastWeek() manually complete."
                ))
            .onErrorResume(
                ScheduledTasksException.class,
                (exception) ->
                    ReactiveResponseBuilder
                        .INTERNAL_SERVER_ERROR(exception.getMessage(), exception)
            );
    }

    @Override
    public Mono<ServerResponse>
    executeSendIntervalIndicatorReport(ServerRequest request)
    {
        return
        this.intervalIndicatorReporter
            .sendIntervalIndicatorReportManually()
            .subscribeOn(Schedulers.boundedElastic())
            .then(
                ReactiveResponseBuilder.OK(
                null,
                "Execute task sendIntervalIndicatorReportManually() complete!")
            .onErrorResume(
                ScheduledTasksException.class,
                (exception) ->
                    ReactiveResponseBuilder
                        .INTERNAL_SERVER_ERROR(exception.getMessage(), exception)
                )
            );
    }
}