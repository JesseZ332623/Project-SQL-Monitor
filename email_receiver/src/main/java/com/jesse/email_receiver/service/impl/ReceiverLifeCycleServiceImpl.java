package com.jesse.email_receiver.service.impl;

import com.jesse.email_receiver.service.ReceiverLifeCycleService;
import io.github.jessez332623.reactive_response_builder.ReactiveResponseBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class ReceiverLifeCycleServiceImpl implements ReceiverLifeCycleService
{
    private final
    ReceiverLifecycleManager receiverLifecycleManager;

    /** 手动启动邮件消费者。*/
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

    /** 手动关闭邮件消费者。*/
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
                    "Stop RabbitMQ email receiver..."
                ))
            .onErrorResume(
                IllegalStateException.class,
                (illegalState) ->
                    ReactiveResponseBuilder
                        .BAD_REQUEST(illegalState.getMessage(), null)
            );
    }

    /** 获取邮件消费者运行状态。*/
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