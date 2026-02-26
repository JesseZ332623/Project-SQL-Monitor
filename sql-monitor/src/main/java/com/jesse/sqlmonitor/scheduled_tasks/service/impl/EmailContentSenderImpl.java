package com.jesse.sqlmonitor.scheduled_tasks.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jesse.sqlmonitor.scheduled_tasks.exception.SendEmailContentFailed;
import com.jesse.sqlmonitor.scheduled_tasks.service.EmailContentSender;
import io.github.jessez332623.reactive_email_sender.dto.EmailContent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.rabbitmq.OutboundMessage;
import reactor.rabbitmq.Sender;

import java.time.Duration;

/** 邮件内容 {@link EmailContent} 发送器实现。*/
@Slf4j
@Component
@RequiredArgsConstructor
public class EmailContentSenderImpl implements EmailContentSender
{
    /** 交换机名。*/
    private final static
    String EXCHANGE = "email-receiver-exchange";

    /** 路由键名。*/
    private final static
    String ROUTING_KEY = "email.receiver";

    /** RabbitMQ 消息发送器。*/
    private final Sender sender;

    /** Jackson JSON 解析器。*/
    private final ObjectMapper mapper;

    /** 将消息绑定到指定的交换机和路由键。*/
    private @NotNull OutboundMessage
    createMessage(EmailContent content) throws JsonProcessingException
    {
        return new
        OutboundMessage(
            EXCHANGE, ROUTING_KEY,
            // Jackson 默认按照 UTF-8 进行序列化，
            // 可以不需要先转化成字符串
            this.mapper.writeValueAsBytes(content)
        );
    }

    @Override
    public Mono<Void>
    sendEmailContent(@NotNull EmailContent content)
    {
        return
        this.sender
            .send(Mono.fromCallable(() -> this.createMessage(content)))
            .timeout(Duration.ofSeconds(5L))
            .onErrorMap((exception) ->
                new SendEmailContentFailed(
                    "Send email content to message queue failed!",
                    exception
                )
            );
    }
}