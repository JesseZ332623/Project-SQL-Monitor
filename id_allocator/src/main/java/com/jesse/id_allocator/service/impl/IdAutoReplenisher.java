package com.jesse.id_allocator.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jesse.id_allocator.pojo.AutoReplenishmentMetrix;
import com.jesse.id_allocator.properties.IdAutoReplenisherProperties;
import io.github.jessez332623.reactive_luascript_reader.LuaScriptReader;
import io.github.jessez332623.reactive_luascript_reader.impl.LuaOperatorResult;
import io.github.jessez332623.reactive_luascript_reader.impl.exception.LuaScriptExecuteFailed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.stream.Stream;

import static com.jesse.id_allocator.contsants.LuaScriptOperatorType.ID_AUTO_REPLENISHER;

/** ID 自动补货器。*/
@Slf4j
@Component
@RequiredArgsConstructor
public class IdAutoReplenisher
{
    /** 基于雪花算法的 ID 分配器。*/
    private final
    SnowFlakeWorkerIdAllocator allocator;

    /** 专门用于执行 Lua 脚本的响应式 Redis 模板。*/
    private final
    ReactiveRedisTemplate<String, LuaOperatorResult> redisLuaTemplate;

    /** Lua 脚本读取器。*/
    private final
    LuaScriptReader scriptReader;

    /** Jackson 对象映射器。*/
    private final ObjectMapper mapper;

    /** ID 自动给补货器属性配置类。*/
    private final
    IdAutoReplenisherProperties properties;

    /**
     * 每隔指定的时间执行一次 ID 补货操作的响应式流，
     * {@link ReplenisherLifecycleManager} 负责管理这个流的生命周期。
     */
    public Mono<Void> replenishWithBackpressure()
    {
        final String   idListKey    = this.properties.getIdListKey();
        final int      targetBuffer = this.properties.getTargetBuffer();
        final Duration interval     = this.properties.getInterval();
        final Duration startDelay   = this.properties.getStartDelay();

        return
        Flux.interval(startDelay, interval)
            .flatMap((tick) ->
                this.scriptReader
                    .read(ID_AUTO_REPLENISHER, "idAutoReplenisher.lua")
                    .flatMap((script) -> {
                        final List<String> batchIds
                            = this.allocator.nextBatchIds(targetBuffer);

                        final Object[] args
                            = Stream.concat(Stream.of(targetBuffer), batchIds.stream())
                                    .toArray(Object[]::new);

                        return
                        this.redisLuaTemplate
                            .execute(script, List.of(idListKey), args)
                            .next()
                            .flatMap((result) -> {
                                if (!"UNKNOWN_ERROR".equals(result.getStatus()))
                                {
                                    try
                                    {
                                        log.info(
                                            "[{}] {} ({})",
                                            result.getStatus(), result.getMessage(),
                                            AutoReplenishmentMetrix.fromJson(this.mapper, result.getData())
                                        );

                                        return Mono.empty();
                                    }
                                    catch (JsonProcessingException exception) {
                                        return Mono.error(exception);
                                    }
                                }
                                else
                                {
                                    return
                                    Mono.error(
                                        new LuaScriptExecuteFailed(
                                            result.getStatus(), result.getMessage(),
                                            result.getTimestamp()
                                        )
                                    );
                                }
                            });
                        })
                ).then();
    }
}