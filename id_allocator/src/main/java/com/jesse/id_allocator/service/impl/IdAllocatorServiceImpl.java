package com.jesse.id_allocator.service.impl;

import com.jesse.id_allocator.service.IdAllocatorService;
import io.github.jessez332623.reactive_response_builder.ReactiveResponseBuilder;
import io.github.jessez332623.reactive_response_builder.utils.URLParamPrase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

/** Snowflake ID 直接生成服务实现。*/
@Slf4j
@Service
@RequiredArgsConstructor
public class IdAllocatorServiceImpl implements IdAllocatorService
{
    /** Snowflake Worker ID 分配器实现。*/
    private final SnowFlakeWorkerIdAllocator allocator;

    @Override
    public Mono<ServerResponse> nextId(ServerRequest request)
    {
        return
        Mono.fromCallable(this.allocator::nextId)
            .flatMap((id) ->
                ReactiveResponseBuilder.OK(id, null))
            .doOnSuccess((ignore) ->
                log.info("Generate an ID directory."))
            .onErrorResume((exception) ->
                ReactiveResponseBuilder.INTERNAL_SERVER_ERROR(
                    "Get next snowflake id failed!",
                    exception
                )
            );
    }

    @Override
    public Mono<ServerResponse> nextBatchIds(ServerRequest request)
    {
        return
        URLParamPrase.praseRequestParam(request, "size")
            .map(Integer::parseInt)
            .filter((batchSize) -> batchSize < 100000)
            .map(this.allocator::nextBatchIds)
            .flatMap((ids) ->
                ReactiveResponseBuilder.OK(ids, null))
            .doOnSuccess((ignore) ->
                log.info("Generate batch ids directory."))
            .onErrorResume(
                NumberFormatException.class,
                (exception) -> {
                    log.info("", exception);

                    return
                    ReactiveResponseBuilder.BAD_REQUEST(
                        "Param [size] must be integer!",
                        exception
                    );
                })
            .onErrorResume(
                IllegalArgumentException.class,
                (exception) -> {
                    log.info("", exception);

                    return
                    ReactiveResponseBuilder.BAD_REQUEST(
                        exception.getMessage(),
                        exception
                    );
                })
            .onErrorResume((exception) ->
                ReactiveResponseBuilder.INTERNAL_SERVER_ERROR(
                    "Get next batch snowflake ids failed!",
                    exception
                )
            );
    }
}