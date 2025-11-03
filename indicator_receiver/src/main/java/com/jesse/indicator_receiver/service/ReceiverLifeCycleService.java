package com.jesse.indicator_receiver.service;

import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

/** 指标数据接收器手动生命周期管理服务接口。*/
public interface ReceiverLifeCycleService
{
    /** 获取指标数据消费者运行状态。*/
    Mono<ServerResponse> start(ServerRequest request);

    /** 手动启动指标数据消费者。*/
    Mono<ServerResponse> stop(ServerRequest request);

    /** 手动关闭指标数据消费者。*/
    Mono<ServerResponse> runStatus(ServerRequest request);
}