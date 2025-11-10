package com.jesse.sqlmonitor;

import com.jesse.sqlmonitor.constants.QueryOrder;
import com.jesse.sqlmonitor.indicator_record.repository.MonitorLogRepository;
import com.jesse.sqlmonitor.monitor.MySQLIndicatorsRepository;
import com.jesse.sqlmonitor.monitor.constants.GlobalStatusName;
import com.jesse.sqlmonitor.properties.R2dbcMasterProperties;
import com.jesse.sqlmonitor.response_body.NetWorkTraffic;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
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

    /** 测试：查询指定 IP 指定时间点到现在的指标增长数。*/
    @Test
    public void getIndicatorIncrementByDurationTest()
    {
        this.monitorLogRepository
            .getIndicatorIncrement(
                masterProperties.getHost(),
                LocalDate.now().atStartOfDay())
            .doOnSuccess(System.out::println)
            .block();
    }

    /** 测试：查询并计算指定 IP 指定时间段内的 数据库网络流量 平均值（单位：Kb/s）。*/
    @Test
    public void getAverageNetworkTrafficTest()
    {
        this.monitorLogRepository
            .getAverageNetworkTraffic(
                this.masterProperties.getHost(), LocalDateTime.now())
            .doOnSuccess(System.out::println)
            .block();
    }

    /** 测试：查询数据库的全局状态。*/
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