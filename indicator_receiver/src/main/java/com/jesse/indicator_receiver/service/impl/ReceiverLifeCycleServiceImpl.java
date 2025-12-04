package com.jesse.indicator_receiver.service.impl;

import com.jesse.indicator_receiver.service.ReceiverLifeCycleService;
import io.github.jessez332623.reactive_response_builder.ReactiveResponseBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

/** 指标数据接收器手动生命周期管理服务实现。*/
@Service
@RequiredArgsConstructor
public class ReceiverLifeCycleServiceImpl implements ReceiverLifeCycleService
{
    private final
    ReceiverLifecycleManager receiverLifecycleManager;

    @Override
    public Mono<ServerResponse>
    start(ServerRequest request)
    {
        return
        this.receiverLifecycleManager
            .startManually()
            .then(
                ReactiveResponseBuilder.OK(
                    null,
                    "Starting RabbitMQ indicator receiver..."
                ))
            .onErrorResume(
                IllegalStateException.class,
                (illegalState) ->
                    ReactiveResponseBuilder
                        .BAD_REQUEST(illegalState.getMessage(), null)
            );
    }

    @Override
    public Mono<ServerResponse>
    stop(ServerRequest request)
    {
        return
        this.receiverLifecycleManager
            .stopManually()
            .then(
                ReactiveResponseBuilder.OK(
                    null,
                    "Stop RabbitMQ indicator receiver..."
                ))
            .onErrorResume(
                IllegalStateException.class,
                (illegalState) ->
                    ReactiveResponseBuilder
                        .BAD_REQUEST(illegalState.getMessage(), null)
            );
    }

    @Override
    public Mono<ServerResponse>
    runStatus(ServerRequest request)
    {
        return
        ReactiveResponseBuilder.OK(
            this.receiverLifecycleManager.isRunning()
                ? "RUNNING" : "STOPPING",
            null
        );
    }
}