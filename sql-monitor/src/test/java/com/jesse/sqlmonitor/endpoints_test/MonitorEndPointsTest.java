package com.jesse.sqlmonitor.endpoints_test;

import com.jesse.sqlmonitor.monitor.constants.GlobalStatusName;
import com.jesse.sqlmonitor.monitor.constants.MonitorConstants;
import com.jesse.sqlmonitor.monitor.impl.GlobalStatusQuery;
import com.jesse.sqlmonitor.monitor.service.SQLMonitorService;
import com.jesse.sqlmonitor.route.endpoints_config.SQLMonitorEndPoints;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
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
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.jesse.sqlmonitor.route.endpoints_config.SQLMonitorEndPoints.*;
import static com.jesse.sqlmonitor.utils.PrettyJSONPrinter.getPrettyFormatJSON;

/** 对 {@link SQLMonitorEndPoints} 的所有端点进行测试。*/
@Slf4j
@SpringBootTest
public class MonitorEndPointsTest
{
    @Autowired
    private GlobalStatusQuery globalStatusQuery;

    private static WebTestClient webTestClient;

    /** 指标测试请求次数 = 忽略次数 + 1 */
    private final static int
    INDICATOR_TESTS = MonitorConstants.IGNORE_SNAPSHOTS + 1;

    public MonitorEndPointsTest(
        @Qualifier("sqlMonitorRouteFunction")
        RouterFunction<ServerResponse> routerFunction
    )
    {
        webTestClient
            = WebTestClient.bindToRouterFunction(routerFunction)
                           .configureClient()
                           .responseTimeout(Duration.ofMinutes(1L))
                           .build();
    }

    /**
     * 测试 {@link SQLMonitorService#getDatabaseAddress(ServerRequest)}
     * 获取数据库的地址和端口号。
     */
    @Test
    public void getDatabaseAddressTest()
    {
        webTestClient
            .get()
            .uri(SQLMonitorEndPoints.ROOT + BASE_ADDRESS_QUERY)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isOk()
            .expectBody(String.class)
            .value((json) ->
                System.out.println(getPrettyFormatJSON(json)));
    }

    /**
     * 测试 {@link SQLMonitorService#getQPS(ServerRequest)}
     * 获取本数据库 QPS。
     */
    @Test
    public void getQPSTest()
    {
        for (int index = 0; index < INDICATOR_TESTS; ++index)
        {
            webTestClient
                .get()
                .uri(SQLMonitorEndPoints.ROOT + QPS_QUERY)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value((json) ->
                    System.out.println(getPrettyFormatJSON(json)));

            Mono.delay(Duration.ofSeconds(3L)).block();
        }
    }

    /**
     * 测试 {@link SQLMonitorService#getNetWorkTraffic(ServerRequest)}
     * 获取服务器接收 / 发送数据量相关信息的服务的接口。
     */
    @Test
    public void getNetWorkTrafficTest()
    {
        for (int index = 0; index < INDICATOR_TESTS; ++index)
        {
            webTestClient
                .get()
                .uri((uriBuilder) ->
                    uriBuilder.path(SQLMonitorEndPoints.ROOT + NETWORK_TRAFFIC_QUERY)
                        .queryParam("sizeUnit", "KB")
                        .build())
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value((json) ->
                    System.out.println(getPrettyFormatJSON(json)));

            Mono.delay(Duration.ofSeconds(3L)).block();
        }
    }

    /**
     * 测试 {@link SQLMonitorService#getConnectionUsage(ServerRequest)}
     * 获取数据库连接使用率相关数据的接口。
     */
    @Test
    public void getConnectionUsageTest()
    {
        for (int index = 0; index < INDICATOR_TESTS; ++index)
        {
            webTestClient
                .get()
                .uri(SQLMonitorEndPoints.ROOT + CONNECTION_USAGE_QUERY)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value((json) ->
                    System.out.println(getPrettyFormatJSON(json)));

            Mono.delay(Duration.ofSeconds(3L)).block();
        }
    }

    /**
     * 测试 {@link SQLMonitorService#getGlobalStatus(ServerRequest)}
     * 查询本数据库指定全局状态的接口。
     */
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
                .uri((uriBuilder) ->
                    uriBuilder.path(SQLMonitorEndPoints.ROOT + GLOBAL_STATUS_QUERY)
                        .queryParam("statusName", statusName.name())
                        .build()
                )
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value((json) ->
                    System.out.println(getPrettyFormatJSON(json)));
        }
    }

    /**
     * 测试 {@link SQLMonitorService#getDatabaseSize(ServerRequest)}
     * 查询数据库大小服务的接口在参数错误下失败的情况 。
     */
    @Test
    public void getDatabaseSizeFailedTest()
    {
        webTestClient
            .get()
            .uri((uriBuilder) ->
                uriBuilder.path(SQLMonitorEndPoints.ROOT + DATABASE_SIZE_QUERY)
                    .queryParam("schemaName", "0_o")
                    .queryParam("order", "DESC")
                    .build()
            )
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.BAD_REQUEST)
            .expectBody(String.class)
            .value((json) ->
                System.out.println(getPrettyFormatJSON(json)));
    }

    /**
     * 测试 {@link SQLMonitorService#getDatabaseSize(ServerRequest)}
     * 查询数据库大小服务的接口。
     */
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
                    uriBuilder.path(SQLMonitorEndPoints.ROOT + DATABASE_SIZE_QUERY)
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

    /**
     * 测试 {@link SQLMonitorService#getServerUpTime(ServerRequest)}
     * 查询服务器运行时间服务的接口。
     */
    @Test
    public void getServerUpTimeTest()
    {
        webTestClient
            .get()
            .uri(SQLMonitorEndPoints.ROOT + SERVER_UPTIME_QUERY)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isOk()
            .expectBody(String.class)
            .value((json) ->
                System.out.println(getPrettyFormatJSON(json)));
    }

    /**
     * 测试 {@link SQLMonitorService#getInnodbBufferCacheHitRate(ServerRequest)}
     * 查询 InnoDB 缓存命中率服务的接口。
     */
    @Test
    public void getInnodbBufferCacheHitRateTest()
    {
        for (int index = 0; index < INDICATOR_TESTS; ++index)
        {
            webTestClient
                .get()
                .uri(SQLMonitorEndPoints.ROOT + INNODB_BUFFER_CACHE_HIT_RATE_QUERY)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value((json) ->
                    System.out.println(getPrettyFormatJSON(json)));

            Mono.delay(Duration.ofSeconds(3L)).block();
        }
    }
}