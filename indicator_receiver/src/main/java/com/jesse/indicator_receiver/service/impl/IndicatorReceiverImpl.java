package com.jesse.indicator_receiver.service.impl;

import cn.hutool.core.util.IdUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jesse.indicator_receiver.entity.IndicatorType;
import com.jesse.indicator_receiver.entity.MonitorLog;
import com.jesse.indicator_receiver.properties.IndicatorReceiverProperties;
import com.jesse.indicator_receiver.repository.MonitorLogRepository;
import com.jesse.indicator_receiver.response_body.SentIndicator;
import com.jesse.indicator_receiver.service.IndicatorReceiver;
import com.jesse.indicator_receiver.utils.IPv4Converter;
import com.jesse.indicator_receiver.utils.exception.InvalidIPv4Exception;
import com.rabbitmq.client.Delivery;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.rabbitmq.AcknowledgableDelivery;
import reactor.rabbitmq.Receiver;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/** 指标数据接收器实现。*/
@Slf4j
@Component
@RequiredArgsConstructor
public class IndicatorReceiverImpl implements IndicatorReceiver
{
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
    private @NotNull Tuple2<List<MonitorLog>, List<Delivery>>
    parseDeliveries(@NotNull List<AcknowledgableDelivery> deliveries)
    {
        List<MonitorLog> monitorLogs          = new ArrayList<>();
        List<Delivery>   successfulDeliveries = new ArrayList<>();

        deliveries.forEach((delivery) -> {

            // 提取消息载荷
            String sentIndicator
                = new String(delivery.getBody(), StandardCharsets.UTF_8);

            SentIndicator<?> sentIndicatorInstance;
            String indicatorJSON;

            try
            {
                // 解析从消息队列中读取的 JSON
                sentIndicatorInstance
                    = this.mapper.readValue(sentIndicator, SentIndicator.class);

                // 对于消息载荷实例中指标数据为空的情况，不确认且移入死信队列
                if (Objects.isNull(sentIndicatorInstance.getIndicator()))
                {
                    log.warn(
                        "Received message with null indicator, " +
                        "message will be discarded: {}", sentIndicator
                    );

                    this.rejectToDLQ(delivery, "NULL_INDICATOR");
                    return;
                }

                // 再将内部的实体信息转化成 JSON 字符串
                indicatorJSON
                    = this.mapper
                          .writeValueAsString(sentIndicatorInstance.getIndicator());
            }
            catch (JsonProcessingException exception)
            {
                /*
                 * 如果出现 JSON 解析失败的异常
                 *（比如直接从 RabbitMQ 前端控制台向这个队列发送无关消息），
                 * 不要抛出异常，应该记录错误并丢弃该消息，确保消费者一直在监听这个队列。
                 */
                log.error(
                    "Could not prase JSON {} caused by: {}",
                    (sentIndicator.length() < 64)
                        ? sentIndicator : sentIndicator.substring(0,  64) + "...",
                    exception.getMessage()
                );

                // 不确认且移入死信队列
                this.rejectToDLQ(delivery, "INVALID_MESSAGE");
                return;
            }

            // 正常的处理流程
            try
            {
                // 获取指标的类型信息
                IndicatorType indicatorTypeName
                    = IndicatorType.valueOf(
                        sentIndicatorInstance.getIndicator()
                            .getClass()
                            .getSimpleName()
                );

                // 分割数据库的 IP 地址
                String ipaddress
                    = sentIndicatorInstance.getAddress().split(":")[0];

                // 构造监控指标实体
                MonitorLog monitorLog
                    = MonitorLog.builder()
                        .logId(IdUtil.getSnowflakeNextId())
                        .datetime(sentIndicatorInstance.getLocalDateTime())
                        .serverIP(IPv4Converter.ipToLong(ipaddress))
                        .indicator(indicatorJSON)
                        .indicatorType(indicatorTypeName)
                        .build();

                monitorLogs.add(monitorLog);        // 保存日志
                successfulDeliveries.add(delivery); // 保存成功的 delivery
            }
            catch (Throwable exception)
            {
                if (exception instanceof InvalidIPv4Exception)
                {
                    // 对于 IP 地址非法的消息，
                    // 由于没法做统计，也移入死信队列
                    log.error("{}", exception.getMessage());
                    this.rejectToDLQ(delivery, "INVALID_IPV4_ADDRESS");
                }

                // 对于其他业务逻辑处理失败的消息载荷，
                // 可能是暂时性错误不确认并重新归队
                log.error(
                    "Business logic processing failed, message will be redelivered. " +
                    "Caused by: {}",
                    exception.getMessage(), exception
                );

                delivery.nack(false, true);
            }
        });

        return Tuples.of(monitorLogs, successfulDeliveries);
    }

    /**
     * 将 {@link IndicatorReceiverImpl#parseDeliveries(List)} 返回的指标列表数据存入数据库，
     * 并视情况确认这个批次的所有消息。
     */
    private Mono<Long>
    batchInsertThenACK(
        @NotNull
        Tuple2<List<MonitorLog>, List<Delivery>> parsed
    )
    {
        final List<MonitorLog> monitorLogs          = parsed.getT1();
        final List<Delivery>   successfulDeliveries = parsed.getT2();

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
                ((AcknowledgableDelivery) delivery).nack(false, true)
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
                            ((AcknowledgableDelivery) delivery).ack(false));

                        log.info(
                            "Successfully processed and acknowledged {} indicators.",
                            successfulDeliveries.size()
                        );
                    })
                    .doOnError((error) -> {
                        successfulDeliveries.forEach((delivery) ->
                            ((AcknowledgableDelivery) delivery).nack(false, true));

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
        return
        this.receiver
            .consumeManualAck(QUEUE_NAME)
            .bufferTimeout(this.properties.getBufferSize(), this.properties.getBufferTimeout())
            .map(this::parseDeliveries)
            .flatMap(this::batchInsertThenACK)
            .then();
    }
}