package com.jesse.sqlmonitor.route.route_function;

import com.jesse.sqlmonitor.scheduled_tasks.service.ScheduledTaskService;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

import static com.jesse.sqlmonitor.route.endpoints_config.ScheduledTasksEndpoints.CLEAN_HISTORICAL_INDICATOR;
import static com.jesse.sqlmonitor.route.endpoints_config.ScheduledTasksEndpoints.SEND_INDICATOR_REPORT;

/** 手动执行定时任务路由函数配置类。*/
@Configuration
@RequiredArgsConstructor
public class ScheduledTasksRouterFunctionConfiguration
{
    @Bean
    public RouterFunction<ServerResponse>
    scheduledTasksRouterFunction(
        @NotNull
        ScheduledTaskService scheduledTaskService
    )
    {
        return
        RouterFunctions.route()
            .POST(SEND_INDICATOR_REPORT,        scheduledTaskService::executeSendIntervalIndicatorReport)
            .DELETE(CLEAN_HISTORICAL_INDICATOR, scheduledTaskService::executeCleanIndicatorUtilLastWeek)
            .build();
    }
}