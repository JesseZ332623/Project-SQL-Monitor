package com.jesse.email_receiver.service.impl;

import com.jesse.email_receiver.properties.EmailReceiverProperties;
import com.jesse.email_receiver.service.EmailReceiverService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/** 邮件消费者生命周期管理器。*/
@Slf4j
@Component
@RequiredArgsConstructor
public class ReceiverLifecycleManager implements SmartLifecycle
{
    /** 异步邮件接收发送器。*/
    private final
    EmailReceiverService emailReceiverService;

    /** 邮件消费服务相关属性类。*/
    private final
    EmailReceiverProperties emailReceiverProperties;

    /** 是否正在运行的标志位。*/
    private final AtomicBoolean isRunning
        = new AtomicBoolean(false);

    /**
     * 指标接收器流的订阅句柄
     *（本类通过控制该句柄来间接控制指标接收器的 启动 / 关闭）。
     */
    private final
    AtomicReference<Disposable> emailReceiverDisposable
        = new AtomicReference<>();

    /** 负责优雅关闭的闭锁。*/
    @Qualifier(value = "shutdown-countdown-latch")
    private final CountDownLatch countDownLatch;

    /**
     * 在手动 启动/关闭 接收器时需要上锁保证状态一致，
     * 监视锁是最轻量，最易读的选择。
     */
    private final Object lifeCycleLock = new Object();

    /** HTTP 请求手动启动邮件接收器。*/
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
                        "(Http-Request) Email receiver already started!"
                    );
                }

                log.info("(Http-Request) RabbitMQ email receiver...");
                this.emailReceiverService.setRunningFlag(true);
                this.startEmailConsumer();
            }
        }).then();
    }

    /** HTTP 请求手动关闭邮件接收器。*/
    public Mono<Void> stopManually()
    {
        return
        Mono.fromRunnable(() -> {
            synchronized (this.lifeCycleLock)
            {
                if (!this.isRunning.compareAndSet(true, false))
                {
                    throw new
                    IllegalStateException(
                        "(Http-Request) Email receiver already stopped!"
                    );
                }

                log.info("(Http-Request) Stop RabbitMQ email receiver...");

                this.waitToComplete("(Http-Request)");

                Mono.fromRunnable(this::stopEmailConsumer)
                    .subscribe();
            }
        }).then();
    }

    /** 启动邮件接收器。({@link SmartLifecycle} 自动调用) */
    @Override
    public void start()
    {
        synchronized (this.lifeCycleLock)
        {
            if (this.isRunning.compareAndSet(false, true))
            {
                log.info("(Auto-Starting) RabbitMQ email receiver...");

                this.emailReceiverService.setRunningFlag(true);
                this.startEmailConsumer();
            }
        }
    }

    /** 停止邮件接收器。({@link SmartLifecycle} 自动调用) */
    @Override
    public void stop()
    {
        synchronized (this.lifeCycleLock)
        {
            if (this.isRunning.compareAndSet(true, false))
            {
                log.info("(Auto-Stop) RabbitMQ email receiver...");

                this.waitToComplete("(Auto-Stop)");

                this.stopEmailConsumer();
            }
        }
    }

    /** 获取运行状态。*/
    @Override
    public boolean isRunning() {
        return this.isRunning.get();
    }

    /** 确定生命周期，令其较早注入较晚销毁。*/
    @Override
    public int
    getPhase() {
        return Integer.MAX_VALUE - 100;
    }

    private void
    waitToComplete(String callerName)
    {
        log.info(
            "{} Waiting up to {} for current email to send complete...",
            callerName, this.emailReceiverProperties.getShutdownDelay()
        );

        this.emailReceiverService.setRunningFlag(false);

        try
        {
            final long waitingTimes
                = this.emailReceiverProperties.getShutdownDelay().toMillis();

            // 等待当前活跃的邮件发送任务完成
            final boolean completed
                = this.countDownLatch
                      .await(waitingTimes, TimeUnit.MILLISECONDS);

            // 若超过时间限制发送操作仍然未完成，只能强制放行了。
            if (!completed)
            {
                log.warn(
                    "{} Wait email to send complete timeout (over {} ms), force termination.",
                    callerName, waitingTimes
                );
            }
        }
        catch (InterruptedException interrupted)
        {
            Thread.currentThread().interrupt();
            log.warn(
                "(Http-Request) Shutdown wait interrupted. Caused by: {}",
                interrupted.getMessage()
            );
        }
    }

    /** 延迟重订阅邮件接收器。*/
    private void restartWithDelay()
    {
        Mono.delay(this.emailReceiverProperties.getRestartDelay())
            .subscribeOn(Schedulers.boundedElastic())
            .subscribe((ignore) -> {
                if (this.isRunning.get()) {
                    this.startEmailConsumer();
                }
            });
    }

    /** 取消订阅邮件接收器。*/
    private void stopEmailConsumer()
    {
        final Disposable disposable
            = this.emailReceiverDisposable.getAndSet(null);

        if (Objects.nonNull(disposable) && !disposable.isDisposed())
        {
            log.info("Disposing email consumer subscription...");
            disposable.dispose();
        }
    }

    /** 订阅邮件接收器。*/
    public void startEmailConsumer()
    {
        final Consumer<? super Throwable>
        errorConsumer = (error) -> {
            log.error(
                "RabbitMQ consumer error, Caused by: {}",
                error.getMessage(), error
            );

            if (isRunning.get())
            {
                log.info("Attempting to restart email consumer after error...");
                this.restartWithDelay();
            }
        };

        final Runnable completeConsumer = () -> {
            // 意外的关闭，重启
            if (this.isRunning.get())
            {
                log.warn("Email consumer completed unexpectedly, restarting...");
                this.restartWithDelay();
            }
            else {
                log.info("Email consumer processing stopped normally.");
            }
        };

        final Disposable disposable
            = this.emailReceiverService
                  .receiveEmail()
                  .subscribeOn(Schedulers.boundedElastic())
                  .subscribe(null, errorConsumer, completeConsumer);

        this.emailReceiverDisposable.set(disposable);
    }
}
