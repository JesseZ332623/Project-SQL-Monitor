package com.jesse.sqlmonitor.endpoints_test;

import com.jesse.sqlmonitor.route.endpoints_config.ScheduledTasksEndpoints;
import com.jesse.sqlmonitor.scheduled_tasks.service.ScheduledTaskService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;

import java.time.Duration;

import static com.jesse.sqlmonitor.utils.PrettyJSONPrinter.getPrettyFormatJSON;

/** 手动执行定时任务端点 {@link ScheduledTasksEndpoints} 测试。*/
@Slf4j
@SpringBootTest
public class ScheduledTasksEndpointsTest
{
    private static WebTestClient webTestClient;

    @Value("${spring.application.name}")
    private String profileName;

    public ScheduledTasksEndpointsTest(
        @Qualifier("scheduledTasksRouterFunction")
        RouterFunction<ServerResponse> routerFunction
    )
    {
        webTestClient
            = WebTestClient.bindToRouterFunction(routerFunction)
                           .configureClient()
                           .responseTimeout(Duration.ofSeconds(10L))
                           .build();
    }

    /**
     * 测试 {@link ScheduledTaskService#executeCleanIndicatorUtilLastWeek(ServerRequest)}
     * 手动的执行历史指标清除操作。
     * （在测试环境配置下执行，不能干扰生产环境）
     */
    @Test
    public void executeSendIntervalIndicatorReportTest()
    {
        if ("sqlmonitor-test".equals(profileName))
        {
            webTestClient
                .post()
                .uri(ScheduledTasksEndpoints.SEND_INDICATOR_REPORT)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value((json) ->
                    System.out.println(getPrettyFormatJSON(json))
                );
        }
    }

    /**
     * 测试 {@link ScheduledTaskService#executeSendIntervalIndicatorReport(ServerRequest)}
     * 手动的执行例行指标数据报告发送的操作。
     */
    @Test
    public void executeCleanIndicatorUtilLastWeekTest()
    {
        webTestClient
            .delete()
            .uri(ScheduledTasksEndpoints.CLEAN_HISTORICAL_INDICATOR)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isOk()
            .expectBody(String.class)
            .value((json) ->
                System.out.println(getPrettyFormatJSON(json))
            );
    }
}