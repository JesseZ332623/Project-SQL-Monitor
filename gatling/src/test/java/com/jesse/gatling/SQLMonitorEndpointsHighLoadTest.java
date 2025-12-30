package com.jesse.gatling;

import cn.hutool.core.net.url.UrlBuilder;
import com.jesse.gatling.endpoints.SQLMonitorEndPoints;
import io.gatling.javaapi.core.CoreDsl;
import io.gatling.javaapi.core.OpenInjectionStep;
import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.core.Simulation;
import io.gatling.javaapi.http.HttpDsl;
import io.gatling.javaapi.http.HttpProtocolBuilder;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;

/** SQL 指标监控程序核心端点 Gatling 压力测试。*/
public class SQLMonitorEndpointsHighLoadTest extends Simulation
{
    private final static
    String HOST = "https://localhost:11451";

    private final static
    Duration PAUSE_DURATION = Duration.ofMillis(100L);

    private final static
    Duration SIMULATION_MAX_DURATION = Duration.ofMinutes(10L);

    private static String
    sqlMonitorURL(
        final SQLMonitorEndPoints endPoint,
        @Nullable
        final Map<String, Object> parameters)
    {
        UrlBuilder builder
            = UrlBuilder.ofHttp(HOST, StandardCharsets.UTF_8)
                .addPath(SQLMonitorEndPoints.concat(endPoint));

        if (Objects.nonNull(parameters)) {
            parameters.forEach(builder::addQuery);
        }

        return builder.build();
    }

    public SQLMonitorEndpointsHighLoadTest()
    {
        final HttpProtocolBuilder httpProtocol
            = HttpDsl.http.connectionHeader("application/json");

        final ScenarioBuilder scenario
            = CoreDsl.scenario("Query indicator data from database.")
                     .exec(
                        HttpDsl.http("Query QPS indicator data")
                               .get(sqlMonitorURL(SQLMonitorEndPoints.QPS_QUERY, null))
                               .check(HttpDsl.status().is(200)),
                        HttpDsl.http("Query network traffic indicator data")
                               .get(sqlMonitorURL(SQLMonitorEndPoints.NETWORK_TRAFFIC_QUERY, Map.of("sizeUnit", "KB")))
                               .check(HttpDsl.status().is(200)),
                        HttpDsl.http("Query connection usage indicator data")
                               .get(sqlMonitorURL(SQLMonitorEndPoints.CONNECTION_USAGE_QUERY, null))
                               .check(HttpDsl.status().is(200)),
                        HttpDsl.http("Query buffer hit rate indicator data")
                               .get(sqlMonitorURL(SQLMonitorEndPoints.INNODB_BUFFER_CACHE_HIT_RATE_QUERY, null))
                               .check(HttpDsl.status().is(200))
                     )
                     .pause(PAUSE_DURATION);

        /*
         * 注入策如下：
         *
         * 0 秒      atOnceUsers(100)              立刻创建 100 用户执行操作
         *
         * 0 ~ 50 秒 rampUsers(5000).during(50s)   在 50 秒内线性的创建用户直至 5000 个
         *                                        （每秒 100 个）
         *
         * 50 ~ 350 秒 constantUsersPerSec(30)     每秒恒定增加 50 个用户，持续 300 秒
         *             .during(300s)
         *
         * 350 ~ 470 秒 rampUsersPerSec(50)        每秒回落 50 个用户，持续 120 秒，
         *              .to(10).during(120)        最终用户数会回落到 10 个
         *
         * 最大执行时间设为 10 分钟，避免测试跑飞。
         */
        this.setUp(
            scenario.injectOpen(
                OpenInjectionStep.atOnceUsers(100),
                CoreDsl.rampUsers(5000)
                       .during(Duration.ofSeconds(50)),
                CoreDsl.constantUsersPerSec(50)
                       .during(300),
                CoreDsl.rampUsersPerSec(50.00)
                       .to(10.00)
                       .during(120)
            )
        ).protocols(httpProtocol).maxDuration(SIMULATION_MAX_DURATION);
    }
}