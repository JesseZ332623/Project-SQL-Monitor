package com.jesse.id_allocator.service.impl;

import com.jesse.id_allocator.service.ReplenisherLifecycleService;
import io.github.jessez332623.reactive_response_builder.ReactiveResponseBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

/** IdAutoReplenisher 生命周期管理服务实现。*/
@Service
@RequiredArgsConstructor
public class ReplenisherLifeCycleServiceImpl implements ReplenisherLifecycleService
{
    private final
    ReplenisherLifecycleManager replenisherLifecycleManager;

    /** 手动启动 ID 补货器。*/
    @Override
    public Mono<ServerResponse>
    start(ServerRequest request)
    {
        return
        this.replenisherLifecycleManager
            .startManually()
            .then(
                ReactiveResponseBuilder.OK(
                    null,
                    "Starting ID auto replenisher..."
                ))
            .onErrorResume(
                IllegalStateException.class,
                (illegalState) ->
                    ReactiveResponseBuilder
                        .BAD_REQUEST(illegalState.getMessage(), null)
            );
    }

    /** 手动关闭 ID 补货器。*/
    @Override
    public Mono<ServerResponse>
    stop(ServerRequest request)
    {
        return
        this.replenisherLifecycleManager
            .stopManually()
            .then(
                ReactiveResponseBuilder.OK(
                    null,
                    "Stop ID auto replenisher..."
                ))
            .onErrorResume(
                IllegalStateException.class,
                (illegalState) ->
                    ReactiveResponseBuilder
                        .BAD_REQUEST(illegalState.getMessage(), null)
            );
    }

    /** 获取 ID 补货器的运行状态。*/
    @Override
    public Mono<ServerResponse>
    runStatus(ServerRequest request)
    {
        return
        ReactiveResponseBuilder.OK(
            this.replenisherLifecycleManager.isRunning()
                ? "RUNNING" : "STOPPING",
            null
        );
    }
}