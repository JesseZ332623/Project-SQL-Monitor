package com.jesse.indicator_receiver.route.filter;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.web.reactive.function.server.HandlerFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

/** 简单的路由函数过滤器，用于记录监视 请求 -> 响应花费的时间。*/
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final public class MonitoringFilter
{
    public static @NonNull Mono<ServerResponse>
    doFilter(
        ServerRequest                   request,
        HandlerFunction<ServerResponse> next
    )
    {
        final long start         = System.currentTimeMillis();
        final String requestPath = request.path();

        return
        Mono.defer(() -> {
            log.info(
                "Starting monitoring request: {} {}",
                request.method(), requestPath
            );

           return
           next.handle(request)
               .doOnSuccess((response) ->
                   log.info(
                       "Monitoring request {} complete, cost {} ms, code: {}",
                       requestPath,
                       System.currentTimeMillis() - start,
                       response.statusCode()
                   )
               );
        });
    }
}