package com.jesse.sqlmonitor.monitor.impl.connection_usage.impl;

import com.jesse.sqlmonitor.monitor.impl.connection_usage.ConnectionUsageCounter;
import com.jesse.sqlmonitor.response_body.ConnectionUsage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import static com.jesse.sqlmonitor.utils.SQLMonitorUtils.queryRow;

/** 数据库连接使用数据计算器实现。*/
@Slf4j
@Component
@RequiredArgsConstructor
public class ConnectionUsageCounterImpl
    implements ConnectionUsageCounter
{
    /** 数据库客户端实例的引用。*/
    private final DatabaseClient databaseClient;

    public Mono<ConnectionUsage> getConnectionUsage()
    {
        final String querySQL = """
            SELECT
                VARIABLE_VALUE as max_connections,
                (SELECT VARIABLE_VALUE FROM performance_schema.global_status WHERE VARIABLE_NAME = 'THREADS_CONNECTED') as current_connections,
                (SELECT VARIABLE_VALUE FROM performance_schema.global_status WHERE VARIABLE_NAME = 'THREADS_CONNECTED') / VARIABLE_VALUE * 100 as connection_usage_percent
            FROM
                performance_schema.global_variables
            WHERE
                VARIABLE_NAME = 'MAX_CONNECTIONS'
            """;

        return
            this.databaseClient
                .sql(querySQL)
                .map((row, metadata) -> {
                    final int maxConnections
                        = Integer.parseInt(queryRow(row, "max_connections", String.class));
                    final int currentConnections
                        = Integer.parseInt(queryRow(row, "current_connections", String.class));
                    final double usagePercent
                        = queryRow(row, "connection_usage_percent", Double.class);

                    return
                        ConnectionUsage.builder()
                            .maxConnections(maxConnections)
                            .currentConnections(currentConnections)
                            .connectUsagePercent(usagePercent)
                            .build();
                })
                .one();
    }
}