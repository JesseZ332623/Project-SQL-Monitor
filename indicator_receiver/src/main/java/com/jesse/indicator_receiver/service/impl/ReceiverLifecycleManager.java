package com.jesse.indicator_receiver.service.impl;

import com.jesse.indicator_receiver.properties.IndicatorReceiverProperties;
import com.jesse.indicator_receiver.service.IndicatorReceive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/** 指标接收器生命周期管理器。*/
@Slf4j
@Component
@RequiredArgsConstructor
public class ReceiverLifecycleManager implements SmartLifecycle
{
    /** 是否正在运行的标志位。*/
    private final AtomicBoolean isRunning
        = new AtomicBoolean(false);

    /**
     * 指标接收器流的订阅句柄
     *（本类通过控制该句柄来间接控制指标接收器的 启动 / 关闭）。
     */
    private final
    AtomicReference<Disposable> indicatorReceiverDisposable
        = new AtomicReference<>();

    /** 来自配置文件的指标接收器相关属性。*/
    private final
    IndicatorReceiverProperties properties;

    /** 指标数据接收器。*/
    private final IndicatorReceive indicatorReceive;

    /**
     * 在手动 启动/关闭 接收器时需要上锁保证状态一致，
     * 监视锁是最轻量，最易读的选择。
     */
    private final Object lifeCycleLock = new Object();

    /** 用于 HTTP 接口的手动启动。*/
    public Mono<Void> startManually()
    {
        return
        Mono.fromRunnable(() -> {
            synchronized (this.lifeCycleLock)
            {
                if (!this.isRunning.compareAndSet(false, true))
                {
                    throw new
                    IllegalStateException(
                       "Indicator receiver already started!"
                    );
                }

                log.info("Starting RabbitMQ indicator receiver...");
                this.indicatorReceive.setRunningFlag(true);
                this.startIndicatorConsumer();
            }
        }).then();
    }

    /** 用于 HTTP 接口的手动停止 */
    public Mono<Void> stopManually()
    {
        return
        Mono.fromRunnable(() -> {
            synchronized (this.lifeCycleLock)
            {
                if (!this.isRunning.compareAndSet(true, false))
                {
                    throw new
                    IllegalStateException("Indicator receiver already stopped!");
                }

                log.info("Stop RabbitMQ indicator receiver...");
                log.info(
                    "Waiting up to {} for current batch to complete...",
                    this.properties.getShutdownDelay()
                );

                this.indicatorReceive.setRunningFlag(false);

                Mono.delay(this.properties.getShutdownDelay())
                    .then(Mono.fromRunnable(this::stopIndicatorConsumer))
                    .subscribeOn(Schedulers.boundedElastic())
                    .subscribe();
            }
        }).then();
    }

    /** 启动指标接收器。({@link SmartLifecycle} 自动调用)*/
    @Override
    public void start()
    {
        synchronized (this.lifeCycleLock)
        {
            if (this.isRunning.compareAndSet(false, true))
            {
                log.info("Auto-Starting RabbitMQ indicator receiver...");

                this.indicatorReceive.setRunningFlag(true);
                this.startIndicatorConsumer();
            }
        }
    }

    /** 停止指标接收器。({@link SmartLifecycle} 自动调用)*/
    @Override
    public void stop()
    {
        synchronized (this.lifeCycleLock)
        {
            if (this.isRunning.compareAndSet(true, false))
            {
                log.info("Auto-Stop RabbitMQ indicator receiver...");
                log.info(
                    "(Auto-Stop) Waiting up to {} for current batch to complete...",
                    this.properties.getShutdownDelay()
                );

                this.indicatorReceive.setRunningFlag(false);

                // 等待可能正在处理的插入操作完成
                try {
                    Thread.sleep(this.properties.getShutdownDelay().toMillis());
                }
                catch (InterruptedException e)
                {
                    Thread.currentThread().interrupt();
                    log.warn("(Auto-Stop) Shutdown wait interrupted.");
                }

                this.stopIndicatorConsumer();
            }
        }
    }

    /** 是否已经启动？*/
    @Override
    public boolean isRunning() {
        return this.isRunning.get();
    }

    /** 确定生命周期，令其较早注入较晚销毁。*/
    @Override
    public int
    getPhase() { return Integer.MAX_VALUE - 100; }

    /** 延迟重订阅指标接收器。*/
    private void restartWithDelay()
    {
        Mono.delay(Duration.ofSeconds(3L))
            .subscribeOn(Schedulers.boundedElastic())
            .subscribe((ignore) -> {
                if (isRunning.get()) {
                    this.startIndicatorConsumer();
                }
            });
    }

    /** 取消订阅指标接收器。*/
    private void stopIndicatorConsumer()
    {
        Disposable disposable
            = this.indicatorReceiverDisposable
                  .getAndSet(null);

        if (disposable != null && !disposable.isDisposed())
        {
            log.info("Disposing indicator consumer subscription...");
            disposable.dispose();
        }
    }

    /** 订阅指标数据消费者方法。*/
    public void startIndicatorConsumer()
    {
        this.indicatorReceiverDisposable.set(
            this.indicatorReceive.receiveIndicator()
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe(
                    null,
                    (error) -> {
                        log.error(
                            "RabbitMQ consumer error, Caused by: {}",
                            error.getMessage(), error
                        );

                        if (isRunning.get())
                        {
                            log.info("Attempting to restart indicator consumer after error...");
                            this.restartWithDelay();
                        }
                    },
                    () -> {
                        if (isRunning.get())
                        {
                            log.warn("indicator consumer completed unexpectedly, restarting...");
                            this.restartWithDelay();
                        }
                        else {
                            log.info("Message processing stopped normally.");
                        }
                    }
                )
        );
    }
}