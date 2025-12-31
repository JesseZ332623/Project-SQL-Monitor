package com.jesse.sqlmonitor.monitor.cacher.impl.warm_up.health;

import com.jesse.sqlmonitor.monitor.cacher.impl.warm_up.CacherWarmUpEventPublisher;
import com.jesse.sqlmonitor.properties.RedisHealthCheckProperties;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/** Redis 健康检查器（全自动化）。*/
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisHealthChecker implements ApplicationListener<ContextClosedEvent>
{
    /** Redis 服务的健康状态。*/
    private final
    AtomicBoolean redisHealth = new AtomicBoolean(false);

    /** 检查器是否正在关闭的标志位。*/
    private final
    AtomicBoolean isShuttingDown = new AtomicBoolean(false);

    /** 响应式通用 Redis 模板。*/
    private final
    ReactiveRedisTemplate<String, Object> redisTemplate;

    /** 缓存预热事件发布器。*/
    private final
    CacherWarmUpEventPublisher warmUpEventPublisher;

    /** Redis 健康检查属性。*/
    private final
    RedisHealthCheckProperties healthCheckProperties;

    /** 健康检查流订阅凭据。*/
    private Disposable disposable = null;

    /** 唯一的公共方法，返回 Redis 的健康状态。*/
    public boolean isHealthy() {
        return this.redisHealth.get();
    }

    /**
     * 在检查器构建完毕后订阅 checkHealth() 进行周期性检查。
     *（在应用启动的时候会立即执行一次）
     */
    @PostConstruct
    private void startHealthCheck()
    {
        log.info("Starting redis health check...");

        this.disposable
            = Flux.interval(Duration.ZERO, this.healthCheckProperties.getCheckInterval())
                  // 过滤掉关闭期间的 TICK
                  .filter((tick) -> !this.isShuttingDown.get())
                  .flatMap((tick) -> this.checkHealth())
                  .subscribe();
    }

    /**
     * 在 Spring 关闭上下文的时候执行取消订阅，
     * 相比起使用 @PreDestroy 更符合标准。
     */
    @Override
    public void
    onApplicationEvent(@NotNull ContextClosedEvent event)
    {
        log.info("Application context closed, stoping health checks...");

        this.isShuttingDown.set(true);

        if (Objects.nonNull(this.disposable) && !this.disposable.isDisposed())
        {
            this.disposable.dispose();
            this.disposable = null;
        }
    }

    /** 经典的 PING PONG 健康状态检查。*/
    private @NotNull Mono<Boolean>
    checkHealth()
    {
        // 驳回所有尝试在关闭期间的调用
        if (this.isShuttingDown.get()) {
            return Mono.empty();
        }

        return
        this.redisTemplate
            .getConnectionFactory()
            .getReactiveConnection()
            .ping()
            .timeout(this.healthCheckProperties.getPingTimeout())
            .doOnSuccess((pong) -> {
                if (this.redisHealth.compareAndSet(false, true))
                {
                    log.info("{} Health check passed, redis is back online.", pong);
                    this.warmUpEventPublisher
                        .onRedisStatusChange(true);
                }
            })
            .thenReturn(true)
            .onErrorResume((exception) -> {
                if (this.redisHealth.compareAndSet(true, false))
                {
                    log.warn("Redis check failed! Caused by: {}", exception.getMessage());
                    this.warmUpEventPublisher
                        .onRedisStatusChange(false);
                }

                return Mono.just(false);
            });
    }
}