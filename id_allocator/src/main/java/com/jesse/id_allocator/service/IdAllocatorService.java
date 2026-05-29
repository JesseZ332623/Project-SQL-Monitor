package com.jesse.id_allocator.service;

import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

/** Snowflake ID 直接生成服务接口。*/
public interface IdAllocatorService
{
    /** 直接生成下一个 ID。*/
    Mono<ServerResponse> nextId(ServerRequest request);

    /** 生成指定数量的一批 ID。*/
    Mono<ServerResponse> nextBatchIds(ServerRequest request);
}
