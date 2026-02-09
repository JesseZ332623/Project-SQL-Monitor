package com.jesse.sqlmonitor.config.snowflakeworker;

import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.util.IdUtil;
import com.jesse.sqlmonitor.luascript_reader.LuaScriptReader;
import com.jesse.sqlmonitor.luascript_reader.impl.LuaOperatorResult;
import com.jesse.sqlmonitor.luascript_reader.impl.exception.LuaScriptExecuteFailed;
import com.jesse.sqlmonitor.properties.SnowFlakeWorkerAllocatorProps;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.jesse.sqlmonitor.luascript_reader.impl.LuaScriptOperatorType.WORKER_ID_ALLOC;

/**
 * Snowflake Worker ID 分配器实现，
 * 在应用启动时基于 Redis 原子抢占 workerId。
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class SnowFlakeWorkerIdAllocator implements SmartLifecycle
{
    /** 在雪花算法下单台机器最大的实例数（一般是 0 ~ 31）。*/
    private static final int
    MAX_WORKER_ID = (int) Snowflake.MAX_WORKER_ID;

    /** 打乱的候选 workerId 列表。*/
    private static final
    List<Integer> CANDIDATES = SnowFlakeWorkerIdAllocator.shuffleCandidates();

    /** 是否正在运行的标志位。*/
    private final AtomicBoolean isRunning
        = new AtomicBoolean(false);

    /** Snowflake Worker ID 分配器属性配置类。*/
    private final
    SnowFlakeWorkerAllocatorProps properties;

    /** 通用 Redis 模板。*/
    private final
    ReactiveRedisTemplate<String, Object> redisTemplate;

    /** 专门用于执行 Lua 脚本的响应式 Redis 模板。*/
    private final
    ReactiveRedisTemplate<String, LuaOperatorResult> redisLuaTemplate;

    /** Lua 脚本读取器。*/
    private final
    LuaScriptReader scriptReader;

    /** 心跳监测定时任务订阅凭据。*/
    private final
    AtomicReference<Disposable> disposable = new AtomicReference<>();

    /** 本服务实例分配到的 WorkerId。*/
    @Getter
    private volatile Integer assignedWorkerId;

    /** 本服务实例唯一 ID。*/
    @Getter
    private String instanceUUID;

    /** 通过 workerId 构造的雪花算法 ID 生成器。*/
    @Getter
    private Snowflake snowflake;

    /**
     * 获取一个打乱的候选 workerId 列表
     *（打乱的目的是防止惊群）。
     */
    private static List<Integer> shuffleCandidates()
    {
        final List<Integer> candidates
            = IntStream.rangeClosed(0, MAX_WORKER_ID)
                       .boxed()
                       .collect(Collectors.toList());

        Collections.shuffle(candidates);

        return candidates;
    }

    /** 在抢到 workerId 后启动续租任务 */
    private void startRenewTask(String key)
    {
        this.disposable.set(
            Flux.interval(this.properties.getRenewInterval())
                .flatMap((ignore) ->
                    this.scriptReader
                        .read(WORKER_ID_ALLOC, "renewWorkerId.lua")
                        .flatMap((script) -> {
                            final long lease
                                = this.properties.getLease().toMillis();

                            return
                            this.redisLuaTemplate
                                .execute(script, List.of(key), this.instanceUUID, lease)
                                .next()
                                .flatMap((result) -> {
                                    if (result.getStatus().equals("SUCCESS"))
                                    {
                                        log.info("{}", result.getMessage());
                                        return Mono.empty();
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
                        .retryWhen(
                            Retry.backoff(this.properties.getMaxRenewRetries(), this.properties.getBackoffStart())
                                 .filter((exception) ->
                                     exception instanceof LuaScriptExecuteFailed)
                                 .doBeforeRetry((signal) ->
                                     log.warn(
                                         "Renew failed, retry {} / {}, Caused by: {}",
                                         signal.totalRetriesInARow() + 1,
                                         this.properties.getMaxRenewRetries(),
                                         signal.failure().getMessage()
                                    )
                                )
                        )
                )
                .onErrorContinue((exception, object) -> {
                    /*
                     * 本轮续租的所有尝试失败，不要终止这个流，
                     * 等待下一轮的续租，但这不是长宜之计（即我们不能保证 Redis 100% 健康），
                     * 未来可以考虑让本服务重新尝试获取新的 workerId（太复杂），
                     * 现在我需要做的就是狠狠的拷打运维同志，让 Ta 保护好 Redis 服务。
                     */
                    log.warn("Renew workerId failed, will retry next tick.", exception);
                })
                .ignoreElements().then()
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe()
        );
    }

    /** 应用启动时去 Redis 抢 workerId。*/
    private void acquireWorkerId()
    {
        this.instanceUUID = UUID.randomUUID().toString();

        Flux.fromIterable(CANDIDATES)
            .concatMap((workerId) -> {
                final String key
                    = this.properties.getLockKeyPrefix() + workerId;

                return
                this.redisTemplate.opsForValue()
                    .setIfAbsent(key, this.instanceUUID, this.properties.getLease())
                    .filter(Boolean::booleanValue)
                    .doOnNext((acquired) -> {
                        this.assignedWorkerId = workerId;
                        this.snowflake        = IdUtil.getSnowflake(this.assignedWorkerId);

                        log.info(
                            "Service instance {} acquire worker ID: {}",
                            this.instanceUUID, this.assignedWorkerId
                        );

                        this.startRenewTask(key);
                    })
                    .map((acquired) -> workerId);
            })
            .take(1)
            .blockLast(this.properties.getAcquireTimeout());

        if (Objects.isNull(this.assignedWorkerId)) {
            throw new
            IllegalStateException("Could not acquire workerId! All slot has been occupy...");
        }
    }

    /**
     * 服务关闭时结束心跳定时任务，并且释放这个 Worker ID。
     *
     * @param callback Spring 框架提供的关闭例程，务必在清理完业务资源后调用
     */
    private void releaseWorkId(final Runnable callback)
    {
        final String key
            = this.properties.getLockKeyPrefix() + this.assignedWorkerId;

        final Disposable renewTaskDisposable
            = this.disposable.getAndSet(null);

        // 取消心跳任务
        if (Objects.nonNull(renewTaskDisposable) && !renewTaskDisposable.isDisposed()) {
            renewTaskDisposable.dispose();
        }

        this.redisTemplate.delete(key)
            .timeout(Duration.ofSeconds(5L))
            .onErrorResume((exception) -> {
                // 主动释放 workerId 失败了也无妨，它会自然到期，
                // 这里确保错误不传播即可。
                log.warn(
                    "Service instance {} release workerId {} faied...",
                    this.instanceUUID, this.assignedWorkerId
                );

                return Mono.empty();
            })
            .doFinally((signal) -> {
                log.info(
                    "Service instance {} release workerId {} completed (signal: {})",
                    this.instanceUUID, this.assignedWorkerId, signal
                );
                // 调用回调函数，告诉 Spring 我已经完成业务资源的清理
                callback.run();
            })
            .subscribeOn(Schedulers.boundedElastic())
            .subscribe();
    }

    @Override
    public void start()
    {
        if (this.isRunning.compareAndSet(false, true)) {
            this.acquireWorkerId();
        }
    }

    @Override
    public void stop() { /* NOTHING TO DO */ }

    /** Spring 会优先调用这个 stop() 方法。*/
    @Override
    public void stop(@NotNull Runnable callback)
    {
        if (!this.isRunning.compareAndSet(true, false)) {
            callback.run();
            return;
        }

        final Integer workerId = this.assignedWorkerId;

        if (Objects.isNull(workerId)) {
            callback.run();
            return;
        }

        this.releaseWorkId(callback);
    }

    @Override
    public boolean isRunning() {
        return this.isRunning.get();
    }

    /** 令其较晚构建，较早销毁。*/
    @Override
    public int getPhase() {
        return Integer.MAX_VALUE - 100;
    }

    /** 获取下一个 ID。 */
    public long nextId()
    {
        // 防止误用
        if (Objects.isNull(this.snowflake)) {
            throw new
            IllegalStateException("Snowflake not initialized. Worker ID allocation may have failed.");
        }

        return this.snowflake.nextId();
    }
}