package com.jesse.email_receiver.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jesse.email_receiver.properties.EmailReceiverProperties;
import com.jesse.email_receiver.service.EmailReceiverService;
import io.github.jessez332623.reactive_email_sender.ReactiveEmailSender;
import io.github.jessez332623.reactive_email_sender.dto.EmailContent;
import io.github.jessez332623.reactive_email_sender.exception.EmailException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.rabbitmq.AcknowledgableDelivery;
import reactor.rabbitmq.ConsumeOptions;
import reactor.rabbitmq.Receiver;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/** 异步邮件接收发送器实现。*/
@Slf4j
@Component
@RequiredArgsConstructor
public class EmailReceiverServiceImpl implements EmailReceiverService
{
    /** 邮件接收器相关属性。*/
    private final
    EmailReceiverProperties emailReceiverProperties;

    /** 响应式邮件发送器。*/
    private final ReactiveEmailSender emailSender;

    /** RabbitMQ 消息接收器。*/
    private final Receiver receiver;

    /** Jackson JSON 解析器。*/
    private final ObjectMapper objectMapper;

    /** 是否正在运行的标志位
     *（由 {@link ReceiverLifecycleManager} 来传递）。
     */
    private final
    AtomicBoolean isRunning = new AtomicBoolean(false);

    /** 记录正在执行的邮件发送操作的数量。*/
    private final
    AtomicInteger activeSendOperations = new AtomicInteger(0);

    /** 负责优雅关闭的闭锁。*/
    @Qualifier(value = "shutdown-countdown-latch")
    private final CountDownLatch countDownLatch;

    /** {@link Receiver} 的参数配置。*/
    private ConsumeOptions consumeOptions;

    /** 初始化消费参数。*/
    @PostConstruct
    private void initConsumOptions()
    {
        final int prefetchCount
            = this.emailReceiverProperties.getPrefetchCount();

        this.consumeOptions
            = new ConsumeOptions().qos(prefetchCount);
    }

    @Override
    public void setRunningFlag(boolean flag)
    {
        this.isRunning.set(flag);

        // 如果在关闭期间没有任何活跃的发送任务，直接放行
        if (!flag && this.activeSendOperations.get() == 0)
        {
            log.info("No active send operation, count down directly.");
            this.countDownLatch.countDown();
        }
    }

    /** 将无效的消息扔进死信队列，并报告原因。*/
    private void
    rejectToDLQ(@NotNull AcknowledgableDelivery delivery, String reason)
    {
        log.warn("Reject message to dead letter queue, reseason: {}", reason);

        delivery.nack(false, false);
    }

    /** 解析从队列中消费的消息载荷。*/
    private @NotNull Mono<Tuple2<EmailContent, AcknowledgableDelivery>>
    parseDeliveries(@NotNull AcknowledgableDelivery delivery)
    {
        final String emailContentJSON
            = new String(delivery.getBody(), StandardCharsets.UTF_8);

        return
        Mono.fromCallable(() ->
            this.objectMapper
                .readValue(emailContentJSON, EmailContent.class))
        .map((emailContent) -> Tuples.of(emailContent, delivery))
        .onErrorResume(JsonProcessingException.class, (exception) -> {
            /*
             * 如果出现 JSON 解析失败的异常
             *（比如直接从 RabbitMQ 前端控制台向这个队列发送无关消息），
             * 不要抛出异常，应该记录错误并丢弃该消息，确保消费者一直在监听这个队列。
             */
            log.error(
                "Could not prase JSON {} caused by: {}",
                (emailContentJSON.length() < 64)
                    ? emailContentJSON : emailContentJSON.substring(0,  64) + "...",
                exception.getMessage()
            );

            // 不确认且移入死信队列
            this.rejectToDLQ(delivery, "INVALID_MESSAGE");

            return Mono.empty();
        });
    }

    /** 正式发送邮件。*/
    private Mono<Void>
    sendEmail(
        @NotNull
        Tuple2<EmailContent, AcknowledgableDelivery> parsed
    )
    {
        final EmailContent           email    = parsed.getT1();
        final AcknowledgableDelivery delivery = parsed.getT2();

        // 若在发送前服务就被关闭，这封邮件将不被确认且归队。
        if (!this.isRunning.get())
        {
            log.info(
                "Server is shutting down, " +
                "rejecting email which send to {} and subject is: {}",
                email.getTo(), email.getSubject()
            );

            delivery.nack(false, true);
            return Mono.empty();
        }

        return
        Mono.fromRunnable(this.activeSendOperations::incrementAndGet)
            .then(this.emailSender.sendEmail(email))
            .doOnSuccess((ignore) -> {
                log.info("Send email to {} complete!", email.getTo());
                // 确认这条消息
                delivery.ack(false);
            })
            .doOnError((exception) -> {
                log.error("{}", exception.getMessage(), exception);

                if (exception instanceof EmailException emailException)
                {
                    switch (emailException.getErrorType())
                    {
                        // 网络问题，重试多次后仍然无效的，归队
                        case NETWORK_ISSUE ->
                            delivery.nack(false, true);

                        // 发送人邮件格式非法，丢进死信队列
                        case INVALID_CONTENT ->
                            this.rejectToDLQ(delivery, "INVALID_MAIL_ADDRESS");
                    }
                }
                else
                {
                    // 至于其他异常，也考虑丢进死信队列
                    this.rejectToDLQ(
                        delivery,
                        "UNEXPECTED_SEDN_ERROR" + exception.getClass().getSimpleName()
                    );
                }
            })
            // “忽略” 错误，别让消费者挂掉然后反复重启
            .onErrorResume((ignore) -> Mono.empty())
            .doFinally((signal) -> {
                // 只有在插入操作计数为 0 且指标消费者处于关闭期间时，
                // 才将 countDownLatch 的计数归零，向外部发送所有插入操作完成的信号
                if (this.activeSendOperations.decrementAndGet() == 0 && !this.isRunning.get()) {
                    this.countDownLatch.countDown();
                }
            });
    }


    @Override
    public Mono<Void> receiveEmail()
    {
        final String queueName
            = this.emailReceiverProperties.getConsumQueueName();

        return
        this.receiver
            .consumeManualAck(queueName, this.consumeOptions)
            // .bufferTimeout(size, timeout)
            // 现在邮件量不大，暂时不设置缓冲区，立刻处理并发送即可。
            .flatMap(this::parseDeliveries)
            .flatMap(this::sendEmail)
            .then();
    }
}