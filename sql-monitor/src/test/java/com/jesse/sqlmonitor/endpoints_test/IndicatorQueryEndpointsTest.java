package com.jesse.sqlmonitor.endpoints_test;

import com.jesse.sqlmonitor.indicator_record.entity.IndicatorType;
import com.jesse.sqlmonitor.indicator_record.service.MonitorLogService;
import com.jesse.sqlmonitor.indicator_record.service.constants.QPSStatisticsType;
import com.jesse.sqlmonitor.properties.R2dbcMasterProperties;
import com.jesse.sqlmonitor.route.endpoints_config.IndicatorQueryEndpoints;
import com.jesse.sqlmonitor.utils.DatetimeFormatter;
import com.jesse.sqlmonitor.utils.PrettyJSONPrinter;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;

import java.util.Arrays;
import java.util.List;

/** 指标统计数据查询端点 {@link IndicatorQueryEndpoints} 测试。*/
@Slf4j
@SpringBootTest
public class IndicatorQueryEndpointsTest
{
    private static WebTestClient webTestClient;

    @Autowired
    private R2dbcMasterProperties masterProperties;

    public IndicatorQueryEndpointsTest(
        @Qualifier("indicatorStatisticsRouteFunction")
        RouterFunction<ServerResponse> routerFunction
    )
    {
        webTestClient
            = WebTestClient.bindToRouterFunction(routerFunction)
                           .build();
    }

    /**
     * 测试 {@link MonitorLogService#fetchIndicatorLog(ServerRequest)}
     * 因参数错误而失败的情况。
     */
    @Test
    public void fetchIndicatorLogFailedTest()
    {
        webTestClient
            .get()
            .uri((urlBuilder) ->
                urlBuilder.path(IndicatorQueryEndpoints.MONITOR_LOG_QUERY)
                    .queryParam("indicator-type", "x_X")
                    .queryParam("server-ip", "0_x")
                    .queryParam("until", "O_o")
                    .queryParam("order", "?_x")
                    .build()
            )
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.BAD_REQUEST)
            .expectBody(String.class)
            .value((responseJson) ->
                System.out.println(
                    PrettyJSONPrinter.getPrettyFormatJSON(responseJson)
                )
            );
    }

    /**
     * 测试 {@link MonitorLogService#fetchIndicatorLog(ServerRequest)}
     * 所有成功的情况。
     */
    @Test
    public void fetchIndicatorLogTest()
    {
        final List<String> indicatorTypes
            = Arrays.stream(IndicatorType.values())
                    .map(IndicatorType::name)
                    .toList();

        final String currentTime = DatetimeFormatter.NOW();

        for (String type : indicatorTypes)
        {
            webTestClient
                .get()
                .uri((urlBuilder) ->
                    urlBuilder.path(IndicatorQueryEndpoints.MONITOR_LOG_QUERY)
                        .queryParam("indicator-type", type)
                        .queryParam("server-ip", masterProperties.getHost())
                        .queryParam("until", currentTime)
                        .queryParam("order", "DESC")
                        .build()
                )
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value((responseJson) ->
                    System.out.println(
                        PrettyJSONPrinter.getPrettyFormatJSON(responseJson)
                    )
                );
        }
    }

    /**
     * 测试 {@link MonitorLogService#qpsStatistics(ServerRequest)}
     * 因为参数错误而失败的情况。
     */
    @Test
    public void qpsStatisticsFailedTest()
    {
        webTestClient
            .get()
            .uri((urlBuilder) ->
                urlBuilder.path(IndicatorQueryEndpoints.MONITOR_LOG_QUERY)
                    .queryParam("indicator-type", "?_?")
                    .queryParam("server-ip", "x_X")
                    .queryParam("until", "!_?")
                    .queryParam("order", "O_o")
                    .build()
            )
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.BAD_REQUEST)
            .expectBody(String.class)
            .value((responseJson) ->
                System.out.println(
                    PrettyJSONPrinter.getPrettyFormatJSON(responseJson)
                )
            );
    }

    /**
     * 测试 {@link MonitorLogService#qpsStatistics(ServerRequest)}
     * 所有成功的情况。
     */
    @Test
    public void qpsStatisticsTest()
    {
        final List<String> statisticsTypes
            = Arrays.stream(QPSStatisticsType.values())
                    .map(QPSStatisticsType::name)
                    .toList();

        final String currentTime = DatetimeFormatter.NOW();

        for (String type : statisticsTypes)
        {
            webTestClient
                .get()
                .uri((uriBuilder) ->
                    uriBuilder.path(IndicatorQueryEndpoints.QPS_STATISTICS)
                              .queryParam("type", type)
                              .queryParam("server-ip", this.masterProperties.getHost())
                              .queryParam("until", currentTime)
                              .build())
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value((responseJson) ->
                    System.out.println(
                        PrettyJSONPrinter.getPrettyFormatJSON(responseJson)
                    )
                );
        }
    }
}