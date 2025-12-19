package com.jesse.sqlmonitor.route.route_function;

import com.jesse.sqlmonitor.route.endpoints_config.ScheduledTasksEndpoints;
import com.jesse.sqlmonitor.route.route_function.filter.MonitoringFilter;
import com.jesse.sqlmonitor.scheduled_tasks.service.ScheduledTaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springdoc.core.annotations.RouterOperation;
import org.springdoc.core.annotations.RouterOperations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.RequestPredicates;
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

    @Bean(name = "scheduledTasksRouterFunction")
    @RouterOperations({
        @RouterOperation(
            path = ScheduledTasksEndpoints.ROOT + SEND_INDICATOR_REPORT,
            operation = @Operation(
                operationId = "executeSendIntervalIndicatorReport",
                summary     = "手动的执行例行指标数据报告发送的操作",
                tags        = {"手动发送例行指标数据报告"},
                responses   = {
                    @ApiResponse(
                        responseCode = "200",
                        description  = "指标报告邮件发送成功"),
                    @ApiResponse(
                        responseCode = "500",
                        description  = "邮件发送失败，数据库断连等其他未知错误"
                    )
                }
            )
        ),
        @RouterOperation(
            path = ScheduledTasksEndpoints.ROOT + CLEAN_HISTORICAL_INDICATOR,
            operation = @Operation(
                operationId = "executeCleanIndicatorUtilLastWeek",
                summary     = "手动的执行历史指标清除操作",
                tags        = {"手动的执行历史指标清除操作"},
                responses   = {
                    @ApiResponse(
                        responseCode = "200",
                        description  = "成功清除上星期之前的历史指标"),
                    @ApiResponse(
                        responseCode = "500",
                        description  = "数据库操作失败，或者其他未知错误"
                    )
                }
            )
        )
    })
    public RouterFunction<ServerResponse>
    scheduledTasksRouterFunction(
        @NotNull @Autowired
        final ScheduledTaskService scheduledTaskService
    )
    {
        return
        RouterFunctions.nest(
            RequestPredicates.path(ScheduledTasksEndpoints.ROOT)
                .and(RequestPredicates.accept(MediaType.APPLICATION_JSON)),
            RouterFunctions.route()
                .POST(SEND_INDICATOR_REPORT,        scheduledTaskService::executeSendIntervalIndicatorReport)
                .DELETE(CLEAN_HISTORICAL_INDICATOR, scheduledTaskService::executeCleanIndicatorUtilLastWeek)
                .filter(MonitoringFilter::doFilter)
                .build()
        );
    }
}