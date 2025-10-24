package com.jesse.indicator_receiver.service;

import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

/** 指标数据接收器手动生命周期管理服务接口。*/
public interface ReceiverLifeCycleService
{
    Mono<ServerResponse> start(ServerRequest request);
    Mono<ServerResponse> stop(ServerRequest request);
    Mono<ServerResponse> runStatus(ServerRequest request);
}