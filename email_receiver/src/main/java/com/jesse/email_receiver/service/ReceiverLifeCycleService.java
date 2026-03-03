package com.jesse.email_receiver.service;

import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

/** 邮件消费者手动生命周期管理服务接口。*/
public interface ReceiverLifeCycleService
{
    /** 手动启动邮件消费者。*/
    Mono<ServerResponse> start(ServerRequest request);

    /** 手动关闭邮件消费者。*/
    Mono<ServerResponse> stop(ServerRequest request);

    /** 获取邮件消费者运行状态。*/
    Mono<ServerResponse> runStatus(ServerRequest request);
}