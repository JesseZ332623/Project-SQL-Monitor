package com.jesse.indicator_receiver.uint_test;

import cn.hutool.core.util.IdUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jesse.indicator_receiver.entity.IndicatorType;
import com.jesse.indicator_receiver.entity.MonitorLog;
import com.jesse.indicator_receiver.properties.IndicatorReceiverProperties;
import com.jesse.indicator_receiver.repository.MonitorLogRepository;
import com.jesse.indicator_receiver.response_body.ConnectionUsage;
import com.jesse.indicator_receiver.response_body.SentIndicator;
import com.jesse.indicator_receiver.service.impl.IndicatorReceiverImpl;
import com.jesse.indicator_receiver.utils.IPv4Converter;
import com.rabbitmq.client.Delivery;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.rabbitmq.AcknowledgableDelivery;
import reactor.rabbitmq.Receiver;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

/** 指标接收器 {@link IndicatorReceiverImpl} 单元测试类。*/
@Slf4j
@ExtendWith(MockitoExtension.class)
class IndicatorReceiverUnitTests
{
    /** 模拟的 指标接收器相关属性。*/
    @Mock
    private IndicatorReceiverProperties properties;

    /** 模拟的 RabbitMQ 消息接收器。*/
    @Mock
    private Receiver receiver;

    /** 模拟的 监控日志仓储类。*/
    @Mock
    private MonitorLogRepository monitorLogRepository;

    /** 模拟的 Jackson JSON 解析器。 */
    @Mock
    private ObjectMapper objectMapper;

    /** 注入上述模拟的依赖，构建指标数据接收器。*/
    @InjectMocks
    private IndicatorReceiverImpl indicatorReceiver;

    /** 携带有效指标数据的消息载荷。*/
    private AcknowledgableDelivery validDelivery;

    /** 携带无效指标数据的消息载荷。*/
    private AcknowledgableDelivery invalidDelivery;

    /** 携带空指标数据的消息载荷。*/
    private AcknowledgableDelivery nullIndicatorDelivery;

    /** 携带非法 IPv4 地址指标数据的消息载荷。*/
    private AcknowledgableDelivery invalidIPv4SentDelivery;

    /** 有效的 {@link SentIndicator} 映射而来的 JSON。*/
    private String validSentIndicatorJSON;

    /** 无效的、格式非法的 JSON。*/
    private String invalidSentIndicatorJSON;

    /** 携带空指标数据的 JSON。*/
    private String nullSentIndicatorJSON;

    /** 携带非法 IPv4 地址的指标数据 JSON。*/
    private String invalidIPv4SentIndicatorJSON;

    /** 在每个测试方法开始前，填充测试数据。*/
    @BeforeEach
    void setUpTestData()
    {
        this.validSentIndicatorJSON   = "{\"type\":\"SentIndicator\",\"localDateTime\":\"2025-11-20T11:18:55.2319387\",\"address\":\"192.168.1.2\",\"indicator\":{\"type\":\"connectionUsage\",\"maxConnections\":1000,\"currentConnections\":500,\"connectUsagePercent\":50.0}}";
        this.invalidSentIndicatorJSON = "{...INVALID JSON HERE...}";
        this.nullSentIndicatorJSON    = "{\"type\":\"SentIndicator\",\"localDateTime\":\"2025-11-20T10:46:35.0747631\",\"address\":\"192.168.1.2\",\"indicator\": null}";
        this.invalidIPv4SentIndicatorJSON
            = "{\"type\":\"SentIndicator\",\"localDateTime\":\"2025-11-20T11:18:55.2319387\",\"address\":\"aaa.bbb.ccc.ddd\",\"indicator\":{\"type\":\"connectionUsage\",\"maxConnections\":1000,\"currentConnections\":500,\"connectUsagePercent\":50.0}}";

        this.validDelivery           = this.createMockDelivery(this.validSentIndicatorJSON);
        this.invalidDelivery         = this.createMockDelivery(this.invalidSentIndicatorJSON);
        this.nullIndicatorDelivery   = this.createMockDelivery(this.nullSentIndicatorJSON);
        this.invalidIPv4SentDelivery = this.createMockDelivery(this.invalidIPv4SentIndicatorJSON);

        indicatorReceiver.setRunningFlag(true);
    }

    /** 构建一个模拟的可确认消息载荷。*/
    private @NotNull AcknowledgableDelivery
    createMockDelivery(@NotNull String messageBody)
    {
        AcknowledgableDelivery delivery
            = Mockito.mock(AcknowledgableDelivery.class);

        // 构建载荷实例的行为
        // 在调用 delivery.getBody() 的时候返回 messageBody 的字节数组
        Mockito.lenient()
               .when(delivery.getBody())
               .thenReturn(messageBody.getBytes(StandardCharsets.UTF_8));

        return delivery;
    }

    /**
     * 测试 {@link IndicatorReceiverImpl#parseDeliveries(List)} 方法
     * 解析 JSON 成功的情况。
     */
    @Test
    void parseDeliveriesShouldReturnMonitorLogsWhenValidJSON() throws Exception
    {
        // 构建一个测试用指标数据
        SentIndicator<ConnectionUsage>
            sentIndicator = new SentIndicator<>(
                LocalDateTime.now(),
                "192.168.1.2",
                ConnectionUsage.builder()
                    .maxConnections(1000)
                    .currentConnections(500)
                    .connectUsagePercent(50.00)
                    .build()
        );

        // 测试用的 ConnectionUsage 示例映射成的 JSON 如下：
        final String validIndicatorJSON
            = "{\"type\":\"connectionUsage\",\"maxConnections\":1000,\"currentConnections\":500,\"connectUsagePercent\":50.0}";

        // 模拟 objectMapper.readValue() 的行为
        Mockito.when(
            this.objectMapper
                .readValue(this.validSentIndicatorJSON, SentIndicator.class))
            .thenReturn(sentIndicator);

        // 模拟 objectMapper.writeValueAsString() 的行为
        Mockito.when(
            this.objectMapper
                .writeValueAsString(sentIndicator.getIndicator()))
            .thenReturn(validIndicatorJSON);

        List<AcknowledgableDelivery> deliveries
            = List.of(this.validDelivery);

        Tuple2<List<MonitorLog>, List<Delivery>>
            result = this.callPrivateParseDeliveries(deliveries);

        // 断言 parseDeliveries() 返回
        // 1 个监控指标实体和 1 个未确认的消息载荷实体
        Assertions.assertFalse(result.getT1().isEmpty());
        Assertions.assertEquals(1, result.getT1().size());
        Assertions.assertEquals(1, result.getT2().size());

        MonitorLog monitorLog = result.getT1().getFirst();

        // 断言 parseDeliveries() 返回的
        // 监控指标实体的数据与预期的一致
        Assertions.assertEquals(IndicatorType.ConnectionUsage, monitorLog.getIndicatorType());
        Assertions.assertEquals(IPv4Converter.ipToLong("192.168.1.2"), monitorLog.getServerIP());
        Assertions.assertEquals(validIndicatorJSON, monitorLog.getIndicator());

        log.info("Unit Test parseDeliveriesShouldReturnMonitorLogsWhenValidJSON() PASS!");
    }

    /**
     * 测试 {@link IndicatorReceiverImpl#parseDeliveries(List)} 方法
     * 解析到非法 JSON 数据的情况。
     */
    @Test
    void parseDeliveriesShouldRejectsToDLQWhenInvalidJSON() throws Exception
    {
        // 模拟 readValue() 遇到非法 JSON 抛出异常
        Mockito.when(
            this.objectMapper
                .readValue(this.invalidSentIndicatorJSON, SentIndicator.class))
            .thenThrow(new JsonProcessingException("Invalid JSON!") {});

        List<AcknowledgableDelivery> deliveries
            = List.of(this.invalidDelivery);

        Tuple2<List<MonitorLog>, List<Delivery>>
            result = this.callPrivateParseDeliveries(deliveries);

        // 断言 parseDeliveries() 返回
        // 空的监控指标实体列表和空的未确认消息载荷列表
        Assertions.assertTrue(result.getT1().isEmpty());
        Assertions.assertTrue(result.getT2().isEmpty());

        // 验证消息是否被拒绝并移入死信队列
        Mockito.verify(this.invalidDelivery)
               .nack(false, false);

        log.info("Unit Test parseDeliveriesShouldRejectsToDLQWhenInvalidJSON() PASS!");
    }

    /**
     * 测试 {@link IndicatorReceiverImpl#parseDeliveries(List)} 方法
     * 遇到指标数据属性为空时的情况。
     */
    @Test
    void parseDeliveriesShouldRejectToDLQWhenNullIndicatorJSON() throws Exception
    {
        SentIndicator<ConnectionUsage>
            withNullIndicator = new SentIndicator<>(
                LocalDateTime.now(),
            "192.1681.1.2",
            null
        );

        Mockito.when(
                this.objectMapper
                    .readValue(this.nullSentIndicatorJSON, SentIndicator.class))
            .thenReturn(withNullIndicator);

        List<AcknowledgableDelivery> deliveries
            = List.of(this.nullIndicatorDelivery);

        Tuple2<List<MonitorLog>, List<Delivery>>
            result = this.callPrivateParseDeliveries(deliveries);

        // 断言 parseDeliveries() 返回
        // 空的监控指标实体列表和空的未确认消息载荷列表
        Assertions.assertTrue(result.getT1().isEmpty());
        Assertions.assertTrue(result.getT2().isEmpty());

        // 验证消息是否被拒绝并移入死信队列
        Mockito.verify(this.nullIndicatorDelivery)
            .nack(false, false);

        log.info("Unit Test parseDeliveriesShouldRejectToDLQWhenNullIndicatorJSON() PASS!");
    }

    @Test
    void parseDeliveriesShouldRejectToDLQWhenInvalidIPv4IndicatorJSON() throws Exception
    {
        SentIndicator<ConnectionUsage>
            withInvalidIPIndicator = new SentIndicator<>(
            LocalDateTime.now(),
            "aaa.bbb.ccc.ddd",
            ConnectionUsage.builder()
                .maxConnections(1000)
                .currentConnections(500)
                .connectUsagePercent(50.00)
                .build()
        );

        Mockito.when(
            this.objectMapper
                .readValue(this.invalidIPv4SentIndicatorJSON, SentIndicator.class))
            .thenReturn(withInvalidIPIndicator);

        List<AcknowledgableDelivery> deliveries
            = List.of(this.invalidIPv4SentDelivery);

        Tuple2<List<MonitorLog>, List<Delivery>>
            result = this.callPrivateParseDeliveries(deliveries);

        // 断言 parseDeliveries() 返回
        // 空的监控指标实体列表和空的未确认消息载荷列表
        Assertions.assertTrue(result.getT1().isEmpty());
        Assertions.assertTrue(result.getT2().isEmpty());

        // 验证消息是否被拒绝并移入死信队列
        Mockito.verify(this.invalidIPv4SentDelivery)
            .nack(false, false);

        log.info("Unit Test parseDeliveriesShouldRejectToDLQWhenInvalidIPv4IndicatorJSON() PASS!");
    }

    @Test
    void batchInsertThenACKShouldAckMessagesWhenInsertSuccess()
    {
        List<MonitorLog> monitorLogs
            = List.of(
                MonitorLog.builder()
                    .logId(IdUtil.getSnowflakeNextId())
                    .datetime(LocalDateTime.now())
                    .serverIP(IPv4Converter.ipToLong("192.168.1.2"))
                    .indicator("{\"type\":\"connectionUsage\",\"maxConnections\":1000,\"currentConnections\":500,\"connectUsagePercent\":50.0}")
                    .build()
        );

        List<Delivery> deliveries
            = List.of(this.validDelivery);

        Tuple2<List<MonitorLog>, List<Delivery>> parsed
            = Tuples.of(monitorLogs, deliveries);

        Mockito.when(
            this.monitorLogRepository
                .batchInsert(monitorLogs))
            .thenReturn(Mono.just(1L));

        long insertAmount = this.callPrivateBatchInsertThenACK(parsed);

        Assertions.assertEquals(1L, insertAmount);
        Mockito.verify(this.validDelivery).ack(false);
        Mockito.verify(this.monitorLogRepository).batchInsert(monitorLogs);

        log.info("Unit Test batchInsertThenACKShouldAckMessagesWhenInsertSuccess() PASS!");
    }

    @Test
    void batchInsertThenACKShouldAckMessagesWhenInsertFailed()
    {
        List<MonitorLog> monitorLogs
            = List.of(
            MonitorLog.builder()
                .logId(IdUtil.getSnowflakeNextId())
                .datetime(LocalDateTime.now())
                .serverIP(IPv4Converter.ipToLong("192.168.1.2"))
                .indicator("{\"type\":\"connectionUsage\",\"maxConnections\":1000,\"currentConnections\":500,\"connectUsagePercent\":50.0}")
                .build()
        );

        List<Delivery> deliveries
            = List.of(this.validDelivery);

        Tuple2<List<MonitorLog>, List<Delivery>> parsed
            = Tuples.of(monitorLogs, deliveries);

        Mockito.when(
            this.monitorLogRepository
                .batchInsert(monitorLogs))
            .thenReturn(
                Mono.error(
                    new RuntimeException("Database connection closed...")
                )
            );

        long insertAmount = this.callPrivateBatchInsertThenACK(parsed);

        Assertions.assertEquals(0L, insertAmount);
        Mockito.verify(this.validDelivery, Mockito.times(1))
               .nack(false, true);
    }

    @Test
    void batchInsertThenACKShouldAckMessagesWhenNoMessages()
    {
        List<MonitorLog> monitorLogs = List.of();
        List<Delivery> deliveries    = List.of();

        Tuple2<List<MonitorLog>, List<Delivery>> parsed
            = Tuples.of(monitorLogs, deliveries);

        long insertAmount = this.callPrivateBatchInsertThenACK(parsed);

        Assertions.assertEquals(0L, insertAmount);

        log.info("Unit Test batchInsertThenACKShouldAckMessagesWhenNoMessages() PASS!");
    }

    @Test
    void batchInsertThenACKShouldAckMessagesWhenShutdown()
    {
        List<MonitorLog> monitorLogs
            = List.of(
            MonitorLog.builder()
                .logId(IdUtil.getSnowflakeNextId())
                .datetime(LocalDateTime.now())
                .serverIP(IPv4Converter.ipToLong("192.168.1.2"))
                .indicator("{\"type\":\"connectionUsage\",\"maxConnections\":1000,\"currentConnections\":500,\"connectUsagePercent\":50.0}")
                .build()
        );

        List<Delivery> deliveries
            = List.of(this.validDelivery);

        Tuple2<List<MonitorLog>, List<Delivery>> parsed
            = Tuples.of(monitorLogs, deliveries);

        this.indicatorReceiver.setRunningFlag(false);

        long insertAmount = this.callPrivateBatchInsertThenACK(parsed);

        Assertions.assertEquals(0L, insertAmount);
        Mockito.verify(this.validDelivery).nack(false, true);
    }

    /**
     * 辅助方法：
     * 通过反射调用私有方法 {@link IndicatorReceiverImpl#parseDeliveries(List)}
     */
    @SuppressWarnings("unchecked")
    private Tuple2<List<MonitorLog>, List<Delivery>>
    callPrivateParseDeliveries(List<AcknowledgableDelivery> deliveries)
    {
        try
        {
            Method method = IndicatorReceiverImpl.class.getDeclaredMethod("parseDeliveries", List.class);
            method.setAccessible(true);

            return
            (Tuple2<List<MonitorLog>, List<Delivery>>)
            method.invoke(this.indicatorReceiver, deliveries);
        }
        catch (Exception e) {
            throw new RuntimeException("Failed to invoke parseDeliveries", e);
        }
    }

    /**
     * 辅助方法：
     * 通过反射调用私有方法 {@link IndicatorReceiverImpl#batchInsertThenACK(Tuple2)}
     */
    @SuppressWarnings("unchecked")
    private Long
    callPrivateBatchInsertThenACK(
        Tuple2<List<MonitorLog>, List<Delivery>> parsedData
    )
    {
        try
        {
            Method method = IndicatorReceiverImpl.class.getDeclaredMethod("batchInsertThenACK", Tuple2.class);
            method.setAccessible(true);

            return
            ((Mono<Long>) method.invoke(indicatorReceiver, parsedData)).block();
        }
        catch (Exception e) {
            throw new RuntimeException("Failed to invoke batchInsertThenACK", e);
        }
    }
}