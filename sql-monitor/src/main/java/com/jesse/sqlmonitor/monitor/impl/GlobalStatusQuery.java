package com.jesse.sqlmonitor.monitor.impl;

import com.jesse.sqlmonitor.monitor.constants.GlobalStatusName;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.HashMap;
import java.util.Map;

import static com.jesse.sqlmonitor.utils.SQLMonitorUtils.tryGetNumericValue;

/** 数据库全局状态查询器。*/
@Slf4j
@Component
public class GlobalStatusQuery
{
    /** 数据库客户端实例的引用。*/
    private final DatabaseClient databaseClient;

    public GlobalStatusQuery(
        @Qualifier("R2dbcMasterDatabaseClient")
        DatabaseClient databaseClient
    )
    {
        this.databaseClient = databaseClient;
    }

    /** 查询本数据库指定全局状态。*/
    public Mono<Map<String, Object>>
    getGlobalStatus(@NotNull GlobalStatusName statusName)
    {
        final String querySQL = "SHOW GLOBAL STATUS LIKE ?";

        return
        this.databaseClient
            .sql(querySQL)
            .bind(0, statusName.getStatusName())
            .fetch()
            .all()
            .collectList()
            .map((rows) -> {
                Map<String, Object> queryResult = new HashMap<>();

                for (Map<String, Object> row : rows)
                {
                    queryResult.put(
                        (String) row.get("Variable_name"),
                        tryGetNumericValue((String) row.get("Value"))
                    );
                }

                return queryResult;
            })
            .doOnError((e) -> log.error("{}", e.getMessage(), e))
            .subscribeOn(Schedulers.boundedElastic());
    }
}