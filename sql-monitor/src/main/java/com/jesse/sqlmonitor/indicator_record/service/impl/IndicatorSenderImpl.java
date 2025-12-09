package com.jesse.sqlmonitor.indicator_record.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jesse.sqlmonitor.response_body.SentIndicator;
import com.jesse.sqlmonitor.response_body.base.ResponseBase;
import com.jesse.sqlmonitor.indicator_record.exception.RecordIndicatorFailed;
import com.jesse.sqlmonitor.indicator_record.service.IndicatorSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.rabbitmq.OutboundMessage;
import reactor.rabbitmq.Sender;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

/** 指标数据发送器实现。*/
@Slf4j
@Service
@RequiredArgsConstructor
public class IndicatorSenderImpl implements IndicatorSender
{
    /** 交换机名。*/
    private final static
    String EXCHANGE = "sql-monitor-exchange";

    /** 路由键名。*/
    private final static
    String ROUTING_KEY = "sql.monitor";

    /** RabbitMQ 消息发送器。*/
    private final Sender sender;

    /** Jackson JSON 解析器。*/
    private final ObjectMapper mapper;

    /** 将消息绑定到指定的交换机和路由键。*/
    @Contract("_ -> new")
    private <T extends ResponseBase<T>>
    @NonNull OutboundMessage
    createMessage(@NonNull SentIndicator<T> indicator) throws JsonProcessingException
    {
        return
        new OutboundMessage(
            EXCHANGE, ROUTING_KEY,
            this.mapper
                .writeValueAsString(indicator)
                .getBytes(StandardCharsets.UTF_8)
            );
    }

    /** 向 RabbitMQ 发送指标数据。*/
    @Override
    public <T extends ResponseBase<T>> Mono<Void>
    sendIndicator(@NonNull SentIndicator<T> indicator)
    {
        return
        this.sender
            .send(Mono.fromCallable(() -> this.createMessage(indicator)))
            .then()
            .timeout(Duration.ofSeconds(5L))
            .onErrorMap(
                (exception) ->
                    new RecordIndicatorFailed(
                        exception.getMessage(), exception
                    )
            );
    }
}