package com.jesse.sqlmonitor;

import com.jesse.sqlmonitor.constants.QueryOrder;
import com.jesse.sqlmonitor.monitor.MySQLIndicatorsRepository;
import com.jesse.sqlmonitor.monitor.constants.GlobalStatusName;
import com.jesse.sqlmonitor.properties.R2dbcMasterProperties;
import com.jesse.sqlmonitor.response_body.NetWorkTraffic;
import com.jesse.sqlmonitor.indicator_record.repository.MonitorLogRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.time.LocalDateTime;

/** SQL 监控程序测试类。*/
@Slf4j
@SpringBootTest
class SQLMonitorApplicationTests
{
    @Autowired
    private MySQLIndicatorsRepository mySQLIndicatorsRepository;

    @Autowired
    private R2dbcMasterProperties masterProperties;

    @Autowired
    private MonitorLogRepository monitorLogRepository;

    /** 测试：读取某个时间点之前的所有指定类型的监控日志记录。*/
    @Test
    public void fetchIndicatorByTypeTest()
    {
        this.monitorLogRepository
            .fetchIndicator(
                NetWorkTraffic.class,
                masterProperties.getHost(),
                LocalDateTime.now(),
                QueryOrder.DESC)
            .collectList()
            .doOnSuccess((indicators) ->
                indicators.forEach(System.out::println))
            .block();
    }

	@Test
	public void queriesCountTest()
    {
        // 高频的查询并计算 QPS 旨在记录重试次数（目前重试不超过 2 次）
        Flux.interval(Duration.ofMillis(10L))
            .flatMap((tick) ->
                this.mySQLIndicatorsRepository
                    .getQPS())
            .doOnNext((qps) -> log.info("{}", qps))
            .take(100L)
            .blockLast();
	}

    @Test
    public void getGlobalStatusTest()
    {
        final GlobalStatusName[] globalStatusNames
            = GlobalStatusName.values();

        for (var statusName : globalStatusNames)
        {
            this.mySQLIndicatorsRepository
                .getGlobalStatus(statusName)
                .doOnSuccess((res) -> {
                    System.out.printf(
                        "Query global status of: %s\n",
                        statusName.getStatusName()
                    );

                    res.forEach((variableName, value) ->
                        System.out.println(variableName + ": " + value)
                    );
                }).block();
        }
    }
}