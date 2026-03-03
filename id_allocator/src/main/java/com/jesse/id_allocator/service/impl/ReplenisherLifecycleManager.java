package com.jesse.id_allocator.service.impl;

import com.jesse.id_allocator.properties.IdAutoReplenisherProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/** IdAutoReplenisher 生命周期管理器。*/
@Slf4j
@Component
@RequiredArgsConstructor
public class ReplenisherLifecycleManager
    implements SmartLifecycle
{
    /** ID 自动给补货器属性配置类。*/
    private final
    IdAutoReplenisherProperties properties;

    /** ID 自动补货器。*/
    private final IdAutoReplenisher idAutoReplenisher;

    /** ID 自动补货器订阅凭据。*/
    private final
    AtomicReference<Disposable> replenisherDisposable
        = new AtomicReference<>();

    /** 运行标志位。*/
    private final
    AtomicBoolean isRunning = new AtomicBoolean(false);

    /**
     * 在手动 启动/关闭 接收器时需要上锁保证状态一致，
     * 监视锁是最轻量，最易读的选择。
     */
    private final Object lifeCycleLock = new Object();

    @Override
    public boolean isRunning() {
        return this.isRunning.get();
    }

    /**
     * 确定启动顺序，
     * 令其晚于 {@link SnowFlakeWorkerIdAllocator} 启动。
     */
    @Override
    public int
    getPhase() { return Integer.MAX_VALUE - 100; }

    public Mono<Void> startManually()
    {
        synchronized (this.lifeCycleLock)
        {
            if (!this.isRunning.compareAndSet(false, true))
            {
                return Mono.error(
                    new IllegalStateException(
                        "(Http-Request) ID auto replenisher already started!"
                    )
                );
            }

            log.info("(Http-Request) ID auto replenisher...");

            return Mono.fromRunnable(this::subscribeAutoReplenisher);
        }
    }

    public Mono<Void> stopManually()
    {
        synchronized (this.lifeCycleLock)
        {
            if (!this.isRunning.compareAndSet(true, false))
            {
                return Mono.error(
                    new IllegalStateException(
                        "(Http-Request) ID auto replenisher already stopped!"
                    )
                );
            }

            log.info("(Http-Request) Stop ID auto replenisher...");

            return Mono.fromRunnable(this::disponseAutoReplenisher);
        }
    }

    @Override
    public void start()
    {
        synchronized (this.lifeCycleLock)
        {
            if (this.isRunning.compareAndSet(false, true))
            {
                log.info("(Auto-Start) ID auto replenisher...");

                this.subscribeAutoReplenisher();
            }
        }
    }

    @Override
    public void stop()
    {
        synchronized (this.lifeCycleLock)
        {
            if (this.isRunning.compareAndSet(true, false))
            {
                log.info("(Auto-Stop) ID auto replenisher...");

                this.disponseAutoReplenisher();
            }
        }
    }

    private void restartWithDelay()
    {
        Mono.delay(this.properties.getRestartDelay())
            .subscribeOn(Schedulers.boundedElastic())
            .subscribe((ignore) -> {
                if (this.isRunning.get()) {
                    this.subscribeAutoReplenisher();
                }
            });
    }

    /** 取消订阅 ID 自动补货器。*/
    private void disponseAutoReplenisher()
    {
        final Disposable disposable
            = this.replenisherDisposable.getAndSet(null);

        if (Objects.nonNull(disposable) && !disposable.isDisposed())
        {
            log.info("Disposing auto replenisher subscription...");
            disposable.dispose();
        }
    }

    /** 订阅 ID 自动补货器。*/
    private void subscribeAutoReplenisher()
    {
        final Consumer<? super Throwable>
        errorConsumer = (error) -> {
            log.error(
                "ID auto replenisher error, Caused by: {}",
                error.getMessage(), error
            );

            // 出错后尝试重启
            if (this.isRunning.get())
            {
                log.info("Attempting to restart auto replenisher after error...");
                this.restartWithDelay();
            }
        };

        final Runnable completeConsumer = () -> {
            // 意外的关闭，重启
            if (this.isRunning.get())
            {
                log.warn("Auto replenisher completed unexpectedly, restarting...");
                this.restartWithDelay();
            }
            else {
                log.info("Auto replenisher processing stopped normally.");
            }
        };

        final Disposable disposable
            = this.idAutoReplenisher
                  .replenishWithBackpressure()
                  .subscribeOn(Schedulers.boundedElastic())
                  .subscribe(null, errorConsumer, completeConsumer);

        this.replenisherDisposable.set(disposable);
    }
}