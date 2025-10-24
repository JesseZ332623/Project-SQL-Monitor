package com.jesse.indicator_receiver.route;

import com.jesse.indicator_receiver.service.ReceiverLifeCycleService;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

import static com.jesse.indicator_receiver.route.IndicatorReceiverEndpoints.*;

/** 指标数据接收器服务路由函数配置。*/
@Configuration
public class IndicatorReceiverRouteFuncConfig
{
    @Bean
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