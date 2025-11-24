package com.jesse.sqlmonitor.endpoints_test;

import com.jesse.sqlmonitor.monitor.constants.GlobalStatusName;
import com.jesse.sqlmonitor.monitor.impl.GlobalStatusQuery;
import com.jesse.sqlmonitor.route.endpoints_config.SQLMonitorEndPoints;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.jesse.sqlmonitor.utils.PrettyJSONPrinter.getPrettyFormatJSON;

/** 对 {@link SQLMonitorEndPoints} 的所有端点进行测试。*/
@Slf4j
@SpringBootTest
public class MonitorEndPointsTest
{
    @Autowired
    private GlobalStatusQuery globalStatusQuery;

    private static WebTestClient webTestClient;

    public MonitorEndPointsTest(
        @Qualifier("sqlMonitorRouteFunction")
        RouterFunction<ServerResponse> routerFunction
    )
    {
        webTestClient
            = WebTestClient.bindToRouterFunction(routerFunction)
                           .build();
    }

    /** 测试获取数据库的地址和端口号。*/
    @Test
    public void getDatabaseAddressTest()
    {
        webTestClient
            .get()
            .uri(SQLMonitorEndPoints.BASE_ADDRESS_QUERY)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isOk()
            .expectBody(String.class)
            .value((json) ->
                System.out.println(getPrettyFormatJSON(json)));
    }

    /** 测试获取本数据库 QPS。*/
    @Test
    public void getQPSTest()
    {
        webTestClient
            .get()
            .uri(SQLMonitorEndPoints.QPS_QUERY)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isOk()
            .expectBody(String.class)
            .value((json) ->
                System.out.println(getPrettyFormatJSON(json)));
    }

    /** 测试获取服务器接收 / 发送数据量相关信息的服务的接口。*/
    @Test
    public void getNetWorkTrafficTest()
    {
        webTestClient
            .get()
            .uri((uriBuilder) ->
                uriBuilder.path(SQLMonitorEndPoints.NETWORK_TRAFFIC_QUERY)
                          .queryParam("sizeUnit", "KB")
                          .build())
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isOk()
            .expectBody(String.class)
            .value((json) ->
                System.out.println(getPrettyFormatJSON(json)));
    }

    /** 获取数据库连接使用率相关数据的接口。*/
    @Test
    public void getConnectionUsageTest()
    {
        webTestClient
            .get()
            .uri(SQLMonitorEndPoints.CONNECTION_USAGE_QUERY)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isOk()
            .expectBody(String.class)
            .value((json) ->
                System.out.println(getPrettyFormatJSON(json)));
    }

    /** 测试查询本数据库指定全局状态的接口。*/
    @Test
    public void getGlobalStatusTest()
    {
        List<GlobalStatusName>
            globalStatus = Arrays.asList(GlobalStatusName.values());

        Collections.shuffle(globalStatus);

        List<GlobalStatusName> selectedStatus
            = globalStatus.stream().limit(5L).toList();

        for (GlobalStatusName statusName : selectedStatus)
        {
            webTestClient
                .get()
                .uri(SQLMonitorEndPoints.GLOBAL_STATUS_QUERY + "?statusName=" + statusName.name())
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value((json) ->
                    System.out.println(getPrettyFormatJSON(json)));
        }
    }

    /** 测试查询数据库大小服务的接口。*/
    @Test
    public void getDatabaseSizeTest()
    {
        List<String> schemaNames
            = this.globalStatusQuery
                .getAllSchemaName(true)
                .block();

        Assertions.assertNotNull(schemaNames);

        Collections.shuffle(schemaNames);

        // 统计数据库表的大小是一个耗时的操作，
        // 随机的挑几个就行了。
        List<String> selectedNames
            = schemaNames.stream().limit(3L).toList();

        for (String schemaName : selectedNames)
        {
            webTestClient
                .get()
                .uri((uriBuilder) ->
                    uriBuilder.path(SQLMonitorEndPoints.DATABASE_SIZE_QUERY)
                              .queryParam("schemaName", schemaName)
                              .queryParam("order", "DESC")
                              .build()
                )
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value((json) ->
                    System.out.println(getPrettyFormatJSON(json)));
        }
    }

    /** 测试查询服务器运行时间服务的接口。*/
    @Test
    public void getServerUpTimeTest()
    {
        webTestClient
            .get()
            .uri(SQLMonitorEndPoints.SERVER_UPTIME_QUERY)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isOk()
            .expectBody(String.class)
            .value((json) ->
                System.out.println(getPrettyFormatJSON(json)));
    }

    @Test
    public void getInnodbBufferCacheHitRateTest()
    {
        webTestClient
            .get()
            .uri(SQLMonitorEndPoints.INNODB_BUFFER_CACHE_HIT_RATE_QUERY)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isOk()
            .expectBody(String.class)
            .value((json) ->
                System.out.println(getPrettyFormatJSON(json)));
    }
}