package com.jesse.id_allocator.route;

import com.jesse.id_allocator.route.filter.MonitoringFilter;
import com.jesse.id_allocator.service.ReplenisherLifecycleService;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

import static com.jesse.id_allocator.route.IdAllocatorEndpoints.*;

/** ID 分配器服务路由函数配置类。*/
@Configuration
public class IdAllocatorRouteFunctionConfiguration
{
    @Bean
    public RouterFunction<ServerResponse>
    indicatorReceiverRouteFunc(
        @NotNull @Autowired
        final ReplenisherLifecycleService receiverLifeCycleService
    )
    {
        return
        RouterFunctions.nest(
            RequestPredicates.path(IdAllocatorEndpoints.ROOT)
                .and(RequestPredicates.accept(MediaType.APPLICATION_JSON)),
            RouterFunctions.route()
                .GET(RUN_STATUS,       receiverLifeCycleService::runStatus)
                .POST(START_ALLOCATOR, receiverLifeCycleService::start)
                .POST(STOP_ALLOCATOR,  receiverLifeCycleService::stop)
                .filter(MonitoringFilter::doFilter)
                .build()
        );
    }
}