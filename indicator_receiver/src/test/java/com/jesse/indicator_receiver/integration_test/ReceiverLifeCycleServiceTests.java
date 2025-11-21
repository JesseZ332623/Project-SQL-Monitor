package com.jesse.indicator_receiver.integration_test;

import com.jesse.indicator_receiver.route.IndicatorReceiverEndpoints;
import com.jesse.indicator_receiver.service.impl.ReceiverLifeCycleServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.SmartLifecycle;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;

/**
 * 指标数据接收器手动生命周期管理服务
 * {@link ReceiverLifeCycleServiceImpl} 集成测试。</br>
 *
 * 由于该模块使用 {@link SmartLifecycle} 在应用启动时自动开启指标接收器，
 * 所以需要指定测试的顺序，即：状态检测 -> 关闭 -> 启动
 */
@Slf4j
@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ReceiverLifeCycleServiceTests
{
    private final WebTestClient webTestClient;

    public ReceiverLifeCycleServiceTests(
        @Qualifier(value = "indicatorReceiverRouteFunc")
        RouterFunction<ServerResponse> routerFunction)
    {
        webTestClient
            = WebTestClient.bindToRouterFunction(routerFunction)
                .build();
    }
    
    /** 测试：{@link ReceiverLifeCycleServiceImpl#runStatus(ServerRequest)} */
    @Test
    @Order(1)
    public void runStatusTest()
    {
        this.webTestClient
            .get()
            .uri(IndicatorReceiverEndpoints.RUN_STATUS)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.data")
            .value((runStatus) ->
                Assertions.assertThat(runStatus)
                    .isIn("RUNNING", "STOPPING"));

        log.info("Integration Test runStatusTest() PASS!");
    }
    
    /** 测试：{@link ReceiverLifeCycleServiceImpl#stop(ServerRequest)} */
    @Test
    @Order(2)
    public void stopIndicatorReceiverTest()
    {
        this.webTestClient
            .post()
            .uri(IndicatorReceiverEndpoints.STOP_RECEIVER)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.message")
            .value((message) ->
                Assertions.assertThat(message)
                    .isIn("Stop RabbitMQ indicator receiver..."));

        log.info("Integration Test stopIndicatorReceiverTest() PASS!");
    }
    
    /** 测试：{@link ReceiverLifeCycleServiceImpl#start(ServerRequest)} */
    @Test
    @Order(3)
    public void startIndicatorReceiverTest()
    {
        this.webTestClient
            .post()
            .uri(IndicatorReceiverEndpoints.START_RECEIVER)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.message")
            .value((message) ->
                Assertions.assertThat(message)
                    .isIn("Starting RabbitMQ indicator receiver..."));

        log.info("Integration Test startIndicatorReceiverTest() PASS!");
    }
}