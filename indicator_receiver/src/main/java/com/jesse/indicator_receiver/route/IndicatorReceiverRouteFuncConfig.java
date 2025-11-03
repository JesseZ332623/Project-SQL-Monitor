package com.jesse.indicator_receiver.route;

import com.jesse.indicator_receiver.service.ReceiverLifeCycleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.jetbrains.annotations.NotNull;
import org.springdoc.core.annotations.RouterOperation;
import org.springdoc.core.annotations.RouterOperations;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

import static com.jesse.indicator_receiver.route.IndicatorReceiverEndpoints.*;

/** 指标数据接收器服务路由函数配置。*/
@Configuration
public class IndicatorReceiverRouteFuncConfig
{
    @Bean
    @RouterOperations({
        @RouterOperation(
            path      = RUN_STATUS,
            method    = { RequestMethod.GET },
            operation = @Operation(
                operationId = "runStatus",
                summary     = "获取指标数据消费者运行状态",
                tags        = {"指标数据运行状态获取"},
                responses   = {
                    @ApiResponse(
                        responseCode = "200",
                        description  = "指标数据运行状态（RUNNING 或者 STOPPING）"
                    )
                }
            )
        ),
        @RouterOperation(
            path      = START_RECEIVER,
            method    = { RequestMethod.POST },
            operation = @Operation(
                operationId = "start",
                summary     = "手动启动指标数据消费者",
                tags        = {"手动启动指标数据消费者"},
                responses   =  {
                    @ApiResponse(
                        responseCode = "200",
                        description  = "成功启动指标消费者"
                    ),
                    @ApiResponse(
                        responseCode = "400",
                        description  = "重复启动指标数据消费者"
                    )
                }
            )
        ),
        @RouterOperation(
            path      = STOP_RECEIVER,
            method    = { RequestMethod.POST },
            operation = @Operation(
                operationId = "stop",
                summary     = "手动关闭指标数据消费者",
                tags        = {"手动关闭指标数据消费者"},
                responses   =  {
                    @ApiResponse(
                        responseCode = "200",
                        description  = "成功关闭指标消费者"
                    ),
                    @ApiResponse(
                        responseCode = "400",
                        description  = "重复关闭指标数据消费者"
                    )
                }
            )
        )
    })
    public RouterFunction<ServerResponse>
    indicatorReceiverRouteFunc(
        @NotNull
        ReceiverLifeCycleService receiverLifeCycleService
    )
    {
        return
        RouterFunctions.route()
            .GET(RUN_STATUS,      receiverLifeCycleService::runStatus)
            .POST(START_RECEIVER, receiverLifeCycleService::start)
            .POST(STOP_RECEIVER,  receiverLifeCycleService::stop)
            .build();
    }
}