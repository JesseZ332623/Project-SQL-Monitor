package com.jesse.indicator_receiver.service.impl;

import cn.hutool.core.util.IdUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jesse.indicator_receiver.entity.IndicatorType;
import com.jesse.indicator_receiver.entity.MonitorLog;
import com.jesse.indicator_receiver.properties.IndicatorReceiverProperties;
import com.jesse.indicator_receiver.repository.MonitorLogRepository;
import com.jesse.indicator_receiver.response_body.SentIndicator;
import com.jesse.indicator_receiver.service.IndicatorReceive;
import com.rabbitmq.client.Delivery;
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
import java.util.concurrent.atomic.AtomicBoolean;

import static com.jesse.indicator_receiver.utils.IPv4Converter.ipToLong;
import static com.jesse.indicator_receiver.utils.GenericUtils.extractClassName;

/** 指标数据接收器实现。*/
@Slf4j
@Component
@RequiredArgsConstructor
public class IndicatorReceiverImpl implements IndicatorReceive
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
    private final AtomicBoolean isRunning
        = new AtomicBoolean(false);

    /** 设置是否正在运行的原子标志位。*/
    public void
    setRunningFlag(boolean flag) {
        this.isRunning.set(flag);
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
                    sentIndicator, exception.getMessage()
                );

                delivery.nack(false, false); // 不确认且不重新入队

                return;
            }

            // 只有在 indicator 被正常解析时，才记录日志。
            if (Objects.nonNull(sentIndicatorInstance.getIndicator()))
            {
                // 获取指标的类型信息
                IndicatorType indicatorTypeName
                    = IndicatorType.valueOf(
                    extractClassName(
                        sentIndicatorInstance.getIndicator()
                            .getClass()
                            .getTypeName()
                    )
                );

                // 分割数据库的 IP 地址
                String ipaddress
                    = sentIndicatorInstance.getAddress().split(":")[0];

                // 构造指标日志实体
                MonitorLog monitorLog
                    = MonitorLog.builder()
                    .logId(IdUtil.getSnowflakeNextId())
                    .datetime(sentIndicatorInstance.getLocalDateTime())
                    .serverIP(ipToLong(ipaddress))
                    .indicator(indicatorJSON)
                    .indicatorType(indicatorTypeName)
                    .build();

                monitorLogs.add(monitorLog);        // 保存日志
                successfulDeliveries.add(delivery); // 保存成功的 delivery
            }
        });

        return Tuples.of(monitorLogs, successfulDeliveries);
    }

    /**
     * 将 {@link this#parseDeliveries(List)} 返回的指标列表数据存入数据库，
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
            return Mono.empty();
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

            return Mono.empty();
        }

        /*
         * 将这一批指标日志插入数据库，
         * 如果期间出现错误，这一批次的所有消息都将不在确认并重新入队。
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
        this.monitorLogRepository
            .batchInsert(monitorLogs)
            .timeout(this.properties.getBatchInsertTimeout())
            .doOnSuccess((result) -> {
                successfulDeliveries.forEach((delivery) ->
                    ((AcknowledgableDelivery) delivery).ack(false));

                log.info(
                    "Successfully processed and acknowledged {} indicators",
                    successfulDeliveries.size()
                );
            })
            .doOnError((error) -> {
                successfulDeliveries.forEach((delivery) ->
                    ((AcknowledgableDelivery) delivery).nack(false, true));

                log.error(
                    "Database insert failed, indicators will be redelivered: {}",
                    error.getMessage()
                );
            })
            .onErrorResume((ignore) -> Mono.empty());
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