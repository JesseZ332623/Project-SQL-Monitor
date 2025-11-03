package com.jesse.indicator_receiver.repository;

import com.jesse.indicator_receiver.entity.MonitorLog;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

/** 监控日志实体仓储类。*/
@Slf4j
@Repository
public class MonitorLogRepository // extends R2dbcRepository<MonitorLog, Long>
{
    /** 数据库客户端实例。*/
    private final DatabaseClient databaseClient;

    /** R2DBC 的事务操作器。*/
    private final TransactionalOperator transactionalOperator;

    public MonitorLogRepository(
        @Qualifier("R2dbcSlaverDatabaseClient")
        DatabaseClient databaseClient,
        @Qualifier("R2dbcSlaverTransactionalOperator")
        TransactionalOperator transactionalOperator
    )
    {
        this.databaseClient        = databaseClient;
        this.transactionalOperator = transactionalOperator;
    }

    /**
     * 由于 `monitor_log` 表的主键不是自增的，
     * {@link R2dbcRepository} 的 {@link R2dbcRepository#saveAll(Iterable)} 操作很可能被判定为更新操作，
     * 所以这里手写日志的批量插入，注意整个操作被事务包裹，
     * 因此整个批次的插入必须全部成功，反之全部回滚。
     *
     * @param logs 从 RabbitMQ 消息队列中拉取的监控指标日志
     *
     * @return 总共插入的行数
     */
    public Mono<Long>
    batchInsert(@NotNull List<MonitorLog> logs)
    {
        if (logs.isEmpty()) {
            return Mono.just(0L);
        }

        // 这里我摒弃原先的逐条插入的策略，
        // 直接一口气构造出完整的批量插入 SQL 然后一并发送给数据库
        // 避免频繁的网络往返。
        final StringBuilder insertSQL
            = new StringBuilder(
                  """
                  INSERT INTO
                      monitor_log(log_id, datetime, indicator, server_ip, indicator_type)
                  VALUES
                  """
            );

        List<Object> bindValues = new ArrayList<>();

        for (int index = 0; index < logs.size(); ++index)
        {
            if (index > 0) { insertSQL.append(", "); }

            insertSQL.append("(?, ?, ?, ?, ?)");

            MonitorLog log = logs.get(index);

            bindValues.add(log.getLogId());
            bindValues.add(log.getDatetime());
            bindValues.add(log.getIndicator());
            bindValues.add(log.getServerIP());
            bindValues.add(log.getIndicatorType().name());
        }

        DatabaseClient.GenericExecuteSpec spec
            = this.databaseClient.sql(insertSQL.toString());

        for (int index = 0; index < bindValues.size(); ++index) {
            spec = spec.bind(index, bindValues.get(index));
        }

        return
        this.transactionalOperator
            .transactional(spec.fetch().rowsUpdated())
            .doOnSuccess((updatedRows) ->
                log.info("Successfully insert {} rows of monitor log.", updatedRows))
            .doOnError((error) ->
                log.error(
                    "Batch insert monitor log failed, roll back. Caused by: {}",
                    error.getMessage(), error
                )
            );
    }
}