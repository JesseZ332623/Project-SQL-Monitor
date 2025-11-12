package com.jesse.sqlmonitor.indicator_record.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jesse.sqlmonitor.response_body.SentIndicator;
import com.jesse.sqlmonitor.response_body.base.ResponseBase;
import com.jesse.sqlmonitor.indicator_record.exception.RecordIndicatorFailed;
import com.jesse.sqlmonitor.indicator_record.service.IndicatorSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    private final static String EXCHANGE = "sql-monitor-exchange";

    /** 路由键名。*/
    private final static String ROUTING_KEY = "sql.monitor";

    /** RabbitMQ 消息发送器。*/
    private final Sender sender;

    /** Jackson JSON 解析器。*/
    private final ObjectMapper mapper;

    /** 向 RabbitMQ 发送指标数据。*/
    public <T extends ResponseBase<T>>
    Mono<Void> sendIndicator(SentIndicator<T> indicator)
    {
        return
        Mono.fromCallable(() -> {
            byte[] indicatorBytes
                = this.mapper
                      .writeValueAsString(indicator)
                      .getBytes(StandardCharsets.UTF_8);

            return new
            OutboundMessage(EXCHANGE, ROUTING_KEY, indicatorBytes);
        })
        .flatMap((message) ->
            this.sender
                .send(Mono.just(message))
                .then())
        .timeout(Duration.ofSeconds(5L))
        .onErrorResume((exception) ->
            Mono.error(
                new RecordIndicatorFailed(
                    exception.getMessage(), exception
                )
            )
        );
    }
}