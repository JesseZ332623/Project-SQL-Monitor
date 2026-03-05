package com.jesse.indicator_receiver.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jesse.indicator_receiver.entity.IndicatorType;
import com.jesse.indicator_receiver.entity.MonitorLog;
import com.jesse.indicator_receiver.properties.IndicatorReceiverProperties;
import com.jesse.indicator_receiver.repository.MonitorLogRepository;
import com.jesse.indicator_receiver.response_body.SentIndicator;
import com.jesse.indicator_receiver.service.IndicatorReceiver;
import com.jesse.indicator_receiver.utils.GlobalIdConsumer;
import com.jesse.indicator_receiver.utils.IPv4Converter;
import com.jesse.indicator_receiver.utils.exception.InvalidIPv4Exception;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.rabbitmq.AcknowledgableDelivery;
import reactor.rabbitmq.ConsumeOptions;
import reactor.rabbitmq.Receiver;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/** 指标数据接收器实现。*/
@Slf4j
@Component
@RequiredArgsConstructor
public class IndicatorReceiverImpl implements IndicatorReceiver
{
    /** 全局 ID 消费器。*/
    private final GlobalIdConsumer globalIdConsumer;

    /** 来自配置文件的指标接收器相关属性。*/
    private final
    IndicatorReceiverProperties properties;

    /** 接收指标数据的队列名。*/
    private final static String QUEUE_NAME = "sql-monitor-queue";

    /** RabbitMQ 消息接收器。*/
    private final Receiver receiver;

    /** 监控日志仓储类。*/
    private final MonitorLogRepository monitorLogRepository;

    /** Jackson JSON 解析器。*/
    private final ObjectMapper mapper;

    /**
     * 是否正在运行的原子标志位
     * （由 {@link ReceiverLifecycleManager} 来传递）。
     */
    private final
    AtomicBoolean isRunning = new AtomicBoolean(false);

    /** 记录正在执行的数据库批量操作的操作数量。*/
    private final
    AtomicInteger activeInsertOperations = new AtomicInteger(0);

    /**
     * 用于 {@link this#batchInsertThenACK(Tuple2)} 方法内，
     * 用于等待所有批量插入操作完成，在通知关闭操作。
     */
    @Getter
    private final CountDownLatch countDownLatch = new CountDownLatch(1);

    @Getter
    @AllArgsConstructor
    private static class MonitorLogAndDelivery
    {
        private MonitorLog log;
        private AcknowledgableDelivery delivery;
    }

    /** 设置是否正在运行的原子标志位。*/
    public void
    setRunningFlag(boolean flag)
    {
        this.isRunning.set(flag);

        // 如果设置停止标志，且当前没有活跃插入操作，
        // 我们就立即通知可以关闭。
        if (!flag && this.activeInsertOperations.get() == 0)
        {
            log.info("No active batch insert operation, count down directly.");
            this.countDownLatch.countDown();
        }
    }

    /** 将无效的消息扔进死信队列，并报告原因。*/
    public void
    rejectToDLQ(@NotNull AcknowledgableDelivery delivery, String reason)
    {
        log.warn("Reject message to dead letter queue, reseason: {}", reason);

        delivery.nack(false, false);
    }

    /** 处理解析载荷的过程中遇到的错误。*/
    private void
    handleDeliveryError(AcknowledgableDelivery delivery, Throwable error)
    {
        switch (error)
        {
            case JsonProcessingException ignored ->
                this.rejectToDLQ(delivery, "INVALID_JSON");

            case InvalidIPv4Exception ignored ->
                this.rejectToDLQ(delivery, "INVALID_IPV4_ADDRESS");

            default -> {
                log.error("Unexpected error processing message, will requeue", error);
                delivery.nack(false, true);
            }
        }
    }

    private Tuple2<List<MonitorLog>, List<AcknowledgableDelivery>>
    splitLogsAndDeliveries(List<MonitorLogAndDelivery> pairs)
    {
        if (pairs.isEmpty()) {
            return Tuples.of(List.of(), List.of());
        }

        final List<MonitorLog> logs = new ArrayList<>(pairs.size());
        final List<AcknowledgableDelivery> deliveries = new ArrayList<>(pairs.size());

        for (MonitorLogAndDelivery pair : pairs)
        {
            logs.add(pair.getLog());
            deliveries.add(pair.getDelivery());
        }

        return Tuples.of(logs, deliveries);
    }

    private Mono<MonitorLogAndDelivery>
    processSingleDelivery(AcknowledgableDelivery delivery)
    {
        return
        Mono.fromCallable(() -> {
                // 提取消息载荷
                final String payload = new String(delivery.getBody(), StandardCharsets.UTF_8);
                return Tuples.of(payload, delivery);
            })
            .flatMap(tuple -> {
                final String sentIndicatorJson   = tuple.getT1();
                final AcknowledgableDelivery del = tuple.getT2();

                return
                Mono.fromCallable(() ->
                        this.mapper.readValue(sentIndicatorJson, SentIndicator.class))
                    .filter(sent -> {
                        if (Objects.isNull(sent.getIndicator()))
                        {
                            this.rejectToDLQ(del, "NULL_INDICATOR");
                            return false;
                        }
                        return true;
                    })
                    .flatMap(sent -> {
                        final String indicatorJson;
                        try {
                            indicatorJson = mapper.writeValueAsString(sent.getIndicator());
                        }
                        catch (JsonProcessingException e) {
                            return Mono.error(e);
                        }

                        final IndicatorType type;

                        try {
                            type = IndicatorType.valueOf(sent.getIndicator().getClass().getSimpleName());
                        }
                        catch (IllegalArgumentException e) {
                            return Mono.error(e);
                        }

                        final String ipPart = sent.getAddress().split(":")[0];
                        final long serverIpLong;

                        try {
                            serverIpLong = IPv4Converter.ipToLong(ipPart);
                        }
                        catch (InvalidIPv4Exception e) {
                            return Mono.error(e);
                        }

                        return
                        this.globalIdConsumer.nextId()
                            .map(id ->
                                MonitorLog.builder()
                                    .logId(id)
                                    .messageId(sent.getMessageId())
                                    .datetime(sent.getLocalDateTime())
                                    .serverIP(serverIpLong)
                                    .indicator(indicatorJson)
                                    .indicatorType(type)
                                    .build())
                            .map(log -> new MonitorLogAndDelivery(log, del));
                    });
            });
    }

    /**
     * 解析从 RabbitMQ 队列消费的指标数据，
     * 将其转换成 {@link MonitorLog} 存入列表后返回，
     * 此外还需要保存所有载荷消息有效的 {@link AcknowledgableDelivery} 为一个列表，
     * 用于后续手动的消息确认 (Manual Acknowledgement)。
     *
     * @param deliveries 从 RabbitMQ 队列消费的指标数据
     *
     * @return 由监控指标实体列表和表示有效载荷信息的 Delivery 组成的元组。
     */
    private @NotNull Mono<Tuple2<List<MonitorLog>, List<AcknowledgableDelivery>>>
    parseDeliveries(@NotNull List<AcknowledgableDelivery> deliveries)
    {
        if (deliveries.isEmpty()) {
            return Mono.just(Tuples.of(List.of(), List.of()));
        }

        return
        Flux.fromIterable(deliveries)
            .concatMap(delivery ->
                this.processSingleDelivery(delivery)
                    .onErrorResume(error -> {
                        this.handleDeliveryError(delivery, error);
                        return Mono.empty();
                    })
            )
            .collectList()
            .map(this::splitLogsAndDeliveries)
            .defaultIfEmpty(Tuples.of(List.of(), List.of()));
    }

    /**
     * 将 {@link IndicatorReceiverImpl#parseDeliveries(List)} 返回的指标列表数据存入数据库，
     * 并视情况确认这个批次的所有消息。
     */
    private Mono<Long>
    batchInsertThenACK(
        @NotNull
        Tuple2<List<MonitorLog>, List<AcknowledgableDelivery>> parsed
    )
    {
        final List<MonitorLog> monitorLogs          = parsed.getT1();
        final List<AcknowledgableDelivery>   successfulDeliveries = parsed.getT2();

        // 若这一批载荷中没有任何有效的消息，就不麻烦数据库了。
        if (monitorLogs.isEmpty()) {
            return Mono.just(0L);
        }

        // 若在插入数据库前服务被关闭，
        // 这一批次的指标便不会被确认且全部重新归队。
        if (!this.isRunning.get())
        {
            log.info(
                "Server is shutting down, rejecting batch of {} indicators.",
                successfulDeliveries.size()
            );

            successfulDeliveries.forEach((delivery) ->
                delivery.nack(false, true)
            );

            return Mono.just(0L);
        }

        /*
         * 将这一批指标日志插入数据库，
         * 如果期间出现错误，这一批次的所有消息都将不确认并重新入队，
         * 同时由于数据库插入操作整体是事务性的，这次操作也会回滚。
         *
         * delivery.nack(multiple, requeue); 有两个标志位，语义如下：
         *
         * <multiple> 是否批量确认
         *     false 拒绝当前的这一条消息
         *     true  拒绝当前的消息和所有比它更早的未确认消息
         *
         * <requeue> 是否重新入队
         */
        return
        Mono.fromRunnable(this.activeInsertOperations::incrementAndGet)
            .then(
                this.monitorLogRepository
                    .batchInsert(monitorLogs)
                    .timeout(this.properties.getBatchInsertTimeout())
                    .doOnSuccess((result) -> {
                        successfulDeliveries.forEach((delivery) ->
                            delivery.ack(false));

                        log.info(
                            "Successfully processed and acknowledged {} indicators.",
                            successfulDeliveries.size()
                        );
                    })
                    .doOnError((error) -> {
                        successfulDeliveries.forEach((delivery) ->
                            delivery.nack(false, true));

                        log.error(
                            "Database insert failed, indicators will be redelivered, Caused by: {}",
                            error.getMessage()
                        );
                    })
                    .onErrorResume((ignore) -> Mono.just(0L))
                    .doFinally((signal) -> {
                        // 只有在插入操作计数为 0 且指标消费者处于关闭期间时，
                        // 才将 countDownLatch 的计数归零，向外部发送所有插入操作完成的信号
                        if (this.activeInsertOperations.decrementAndGet() == 0 && !this.isRunning.get()) {
                            this.countDownLatch.countDown();
                        }
                    })
            );
    }

    /** 从队列中消费指标数据，存入数据库。*/
    @Override
    public Mono<Void> receiveIndicator()
    {
        final ConsumeOptions consumeOptions
            = new ConsumeOptions().qos(this.properties.getPrefetchCount());

        return
        this.receiver
            .consumeManualAck(QUEUE_NAME, consumeOptions)
            .bufferTimeout(this.properties.getBufferSize(), this.properties.getBufferTimeout())
            .flatMap(this::parseDeliveries)
            .flatMap(this::batchInsertThenACK)
            .then();
    }
}