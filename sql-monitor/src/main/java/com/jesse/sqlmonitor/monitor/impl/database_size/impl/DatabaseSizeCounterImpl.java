package com.jesse.sqlmonitor.monitor.impl.database_size.impl;

import com.jesse.sqlmonitor.constants.QueryOrder;
import com.jesse.sqlmonitor.monitor.impl.database_size.DatabaseSizeCounter;
import com.jesse.sqlmonitor.response_body.DatabaseSize;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** 数据库大小计算器实现。*/
@Slf4j
@Component
@RequiredArgsConstructor
public class DatabaseSizeCounterImpl implements DatabaseSizeCounter
{
    /** 数据库客户端实例的引用。*/
    private final DatabaseClient databaseClient;

    private @NotNull Mono<Map<String, Double>>
    getTableSizeBySchemaName(String schemaName, @NotNull QueryOrder queryOrder)
    {
        final String tableSizeQuery = """
            SELECT
            	table_name,
                ROUND(((data_length + index_length) / 1024 / 1024), 4) AS Size_in_MB
            FROM
            	information_schema.TABLES
            WHERE
            	table_schema = :schemaName
            ORDER BY
            	Size_in_MB %s
            """;

        return
        this.databaseClient
            .sql(String.format(tableSizeQuery, queryOrder.name()))
            .bind("schemaName", schemaName)
            .fetch()
            .all()
            .collectList()
            .map((rows) -> {
                Map<String, Double> queryResult = new LinkedHashMap<>();

                for (Map<String, Object> row : rows)
                {
                    String tableName = (String) row.get("table_name");
                    Double sizeInMB
                        = Objects.isNull(row.get("Size_in_MB"))
                            ? 0.00
                            : ((BigDecimal) row.get("Size_in_MB")).doubleValue();

                    queryResult.put(tableName, sizeInMB);
                }

                return queryResult;
            });
    }

    private @NotNull Mono<Map<String, DatabaseSize>>
    getSchemaSizeByName(String schemaName, QueryOrder queryOrder)
    {
        final String databaseSizeQuerySQL = """
            SELECT
                table_schema 											AS database_name,
                SUM(data_length + index_length) 				        AS size_bytes,
                ROUND(SUM(data_length + index_length) / 1024 / 1024, 8) AS size_mbytes
            FROM
            	information_schema.tables
            WHERE
                table_schema = :schemaName
            GROUP BY
            	table_schema
            ORDER BY size_bytes %s
            """.formatted(queryOrder);

        return
        this.databaseClient
            .sql(databaseSizeQuerySQL)
            .bind("schemaName", schemaName)
            .fetch()
            .all()
            .flatMap((row) -> {

                String dbName     = (String) row.get("database_name");
                long sizeBytes    = ((BigDecimal) row.get("size_bytes")).longValue();
                double sizeMBytes = ((BigDecimal) row.get("size_mbytes")).doubleValue();

                return
                this.getTableSizeBySchemaName(dbName, queryOrder)
                    .map((tableSizes) -> {
                        DatabaseSize databaseSize
                            = DatabaseSize.builder()
                                .sizeBytes(sizeBytes)
                                .sizeMBytes(sizeMBytes)
                                .tableSizes(tableSizes)
                                .build();

                        return Map.entry(dbName, databaseSize);
                    });
                })
            .collectList()
            .map((entries) -> {
                Map<String, DatabaseSize> queryResult = new LinkedHashMap<>();

                for (Map.Entry<String, DatabaseSize> entry :  entries) {
                    queryResult.put(entry.getKey(), entry.getValue());
                }

                return queryResult;
            });
    }

    public Mono<Map<String, DatabaseSize>>
    getDatabaseSizeInfo(String schemaName, QueryOrder queryOrder)
    {
        return
        this.getSchemaSizeByName(schemaName, queryOrder)
            .subscribeOn(Schedulers.boundedElastic());
    }
}