package com.jesse.sqlmonitor.route.route_function;

import com.jesse.sqlmonitor.luascript_reader.LuaScriptReader;
import com.jesse.sqlmonitor.route.endpoints_config.LuaScriptReaderEndpoints;
import com.jesse.sqlmonitor.route.route_function.filter.MonitoringFilter;
import io.github.jessez332623.reactive_response_builder.ReactiveResponseBuilder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springdoc.core.annotations.RouterOperation;
import org.springdoc.core.annotations.RouterOperations;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.*;
import reactor.core.publisher.Mono;

import static com.jesse.sqlmonitor.route.endpoints_config.LuaScriptReaderEndpoints.CACHE_CLEAR;

/** Lua 脚本读取器路由函数配置类。*/
@Configuration
@RequiredArgsConstructor
public class LuaScriptReaderRouteFunctionConfig
{
    private final LuaScriptReader luaScriptReader;

    private @NotNull Mono<ServerResponse>
    cleanCache(ServerRequest request)
    {
        return
        this.luaScriptReader.cleanCache()
            .flatMap((scripts) ->
                ReactiveResponseBuilder.OK(
                    scripts, "Clear cache of lua script complete!")
            );
    }

    @Bean(name = "luaScriptReaderRouteFunction")
    @RouterOperations({
        @RouterOperation(
            path = LuaScriptReaderEndpoints.ROOT + CACHE_CLEAR,
            operation = @Operation(
                operationId = "cleanCache",
                summary     = "手动清理本服务中缓存的 Lua 脚本实例",
                tags        = {"缓存 Lua 脚本实例清理"},
                responses   = {
                    @ApiResponse(
                        responseCode = "200",
                        description  = "清理完成"
                    )
                }
            )
        )
    })
    public RouterFunction<ServerResponse>
    luaScriptReaderRouteFunction()
    {
        return
        RouterFunctions.nest(
            RequestPredicates.path(LuaScriptReaderEndpoints.ROOT)
                .and(RequestPredicates.accept(MediaType.APPLICATION_JSON)),
            RouterFunctions.route()
                .DELETE(CACHE_CLEAR, this::cleanCache)
                .filter(MonitoringFilter::doFilter)
                .build()
        );
    }
}