package com.jesse.sqlmonitor.route.route_function;

import com.jesse.sqlmonitor.indicator_record.service.MonitorLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.jetbrains.annotations.NotNull;
import org.springdoc.core.annotations.RouterOperation;
import org.springdoc.core.annotations.RouterOperations;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

import static com.jesse.sqlmonitor.route.endpoints_config.IndicatorQueryEndpoints.*;

/** 指标统计数据查询服务路由函数配置类。*/
@Configuration
public class IndicatorStatisticsRouteFunctionConfiguration
{
    @Bean
    @RouterOperations({
        @RouterOperation(
            path = MONITOR_LOG_QUERY,
            operation = @Operation(
                operationId = "fetchIndicatorLog",
                summary     = "按条件查询指标日志数据",
                tags        = {"指标日志的条件查询"},
                responses   = {
                    @ApiResponse(responseCode = "200", description = "成功"),
                    @ApiResponse(responseCode = "400", description = "日期参数格式错误，指标类型不存在, IP 格式非法等"),
                    @ApiResponse(responseCode = "500", description = "数据库断连或其他未知错误")
                }
            )
        ),
        @RouterOperation(
            path = QPS_STATISTICS,
            operation = @Operation(
                operationId = "getQPSAverage",
                summary     = "按条件查询 QPS 的相关统计数据",
                tags        = {"QPS 平均值的条件查询"},
                responses   = {
                    @ApiResponse(responseCode = "200", description = "成功"),
                    @ApiResponse(responseCode = "400", description = "统计类型参数，日期参数格式错误，IP 格式非法等"),
                    @ApiResponse(responseCode = "500", description = "数据库断连或其他未知错误")
                }
            )
        )
    })
    public RouterFunction<ServerResponse>
    indicatorStatisticsRouteFunction(@NotNull MonitorLogService monitorLogService)
    {
        return
        RouterFunctions.route()
            .GET(MONITOR_LOG_QUERY, monitorLogService::fetchIndicatorLog)
            .GET(QPS_STATISTICS,    monitorLogService::qpsStatistics)
            .build();
    }
}
