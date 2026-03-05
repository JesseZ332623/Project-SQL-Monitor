package com.jesse.id_allocator.service;

import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

/** ID 自动补货器生命周期管理服务接口。*/
public interface ReplenisherLifecycleService
{
    /** 手动启动 ID 自动补货器。*/
    Mono<ServerResponse> start(ServerRequest request);

    /** 手动关闭 ID 自动捕获其器。*/
    Mono<ServerResponse> stop(ServerRequest request);

    /** 获取 ID 自动捕获其器运行状态。*/
    Mono<ServerResponse> runStatus(ServerRequest request);
}