package com.jesse.indicator_receiver.integration_test;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.RandomUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jesse.indicator_receiver.entity.IndicatorType;
import com.jesse.indicator_receiver.entity.MonitorLog;
import com.jesse.indicator_receiver.properties.IndicatorReceiverProperties;
import com.jesse.indicator_receiver.repository.MonitorLogRepository;
import com.jesse.indicator_receiver.response_body.ConnectionUsage;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.transaction.reactive.TransactionalOperator;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

/** 监控日志实体仓储类集成测试。*/
@Slf4j
@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class MonitorLogRepositoryTest
{
    /** 来自配置文件的指标接收器相关属性。*/
    @Autowired
    private IndicatorReceiverProperties properties;

    /** Jackson 对象映射器。*/
    @Autowired
    private ObjectMapper objectMapper;

    /** 监控日志实体仓储类。*/
    @Autowired
    private MonitorLogRepository monitorLogRepository;

    /** 数据库客户端实例。*/
    @Autowired
    @Qualifier("R2dbcSlaverDatabaseClient")
    private DatabaseClient databaseClient;

    /** R2DBC 的事务操作器。*/
    @Autowired
    @Qualifier("R2dbcSlaverTransactionalOperator")
    private TransactionalOperator transactionalOperator;

    /**
     * 测试数据存于此处，
     * 注意要使用 static 关键字确保在测试用例间共享。
     */
    private final static
    LinkedList<ArrayList<MonitorLog>> batchOfMonitorLogs
        = new LinkedList<>();

    /** 准备测试数据。*/
    @PostConstruct
    public void makeBatchOfMonitorLogs() throws Exception
    {
        final int  batches   = 100; // 多少批？
        final long eachBatch = this.properties.getBufferSize(); // 每批多少量？

        for (int batchesIndex = 0; batchesIndex < batches; ++batchesIndex)
        {
            batchOfMonitorLogs.add(new ArrayList<>());

            for (long eachBatchIndex = 0; eachBatchIndex < eachBatch; ++eachBatchIndex)
            {
                ArrayList<MonitorLog>
                    monitorLogs = batchOfMonitorLogs.get(batchesIndex);

                MonitorLog monitorLog
                    = MonitorLog.builder()
                        .logId(IdUtil.getSnowflakeNextId())
                        .datetime(LocalDateTime.now())
                        .serverIP(RandomUtil.randomLong(3332702476L, 3332705476L))
                        .indicator(
                            this.objectMapper
                                .writeValueAsString(
                                    ConnectionUsage.builder()
                                        .maxConnections(1000)
                                        .currentConnections(10)
                                        .connectUsagePercent(0.1000)
                                        .build()
                                )
                        )
                        .indicatorType(IndicatorType.ConnectionUsage)
                        .build();

                monitorLogs.add(monitorLog);
            }
        }
    }

    /** 分批插入测试数据。 */
    @Test
    @Order(2)
    public void insertMonitorLog()
    {
        for (ArrayList<MonitorLog> batchOfMonitorLog : batchOfMonitorLogs)
        {
            this.monitorLogRepository
                .batchInsert(batchOfMonitorLog)
                .block();
        }

        log.info("Integration Test insertMonitorLog() PASS!");
    }

    /** 在测试结束后，删除测试指标数据。*/
    @Test
    @Order(3)
    public void deleteTestIndicatorData()
    {
        // 收集所有要删除的 ID
        List<Long> allLogIds
            = batchOfMonitorLogs
                   .stream()
                   .flatMap(List::stream)
                   .map(MonitorLog::getLogId)
                   .collect(Collectors.toList());

        // 分批删除，每批 1000 个
        int  batchSize    = 1000;
        long totalDeleted = 0L;

        for (int i = 0; i < allLogIds.size(); i += batchSize)
        {
            List<Long> batchIds
                = allLogIds.subList(i, Math.min(i + batchSize, allLogIds.size()));

            Long deletedCount
                = this.databaseClient
                      .sql("DELETE FROM sql_monitor.monitor_log WHERE log_id IN (:logIds)")
                      .bind("logIds", batchIds)
                      .fetch()
                      .rowsUpdated()
                      .as(this.transactionalOperator::transactional)
                      .doOnSuccess(rows -> log.debug("Deleted {} rows in batch", rows))
                      .doOnError(error ->
                          log.error("Batch delete failed: {}", error.getMessage(), error))
                      .block();

            totalDeleted += deletedCount != null ? deletedCount : 0L;
        }

        log.info("Cleaned {} rows of test data in total", totalDeleted);

        log.info("Integration Test deleteTestIndicatorData() PASS!");
    }
}