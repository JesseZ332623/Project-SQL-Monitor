package com.jesse.sqlmonitor.indicator_record.repository.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jesse.sqlmonitor.constants.QueryOrder;
import com.jesse.sqlmonitor.indicator_record.dto.FetchIndicatorResult;
import com.jesse.sqlmonitor.indicator_record.entity.IndicatorType;
import com.jesse.sqlmonitor.indicator_record.exception.QueryIndicatorFailed;
import com.jesse.sqlmonitor.indicator_record.repository.MonitorLogRepository;
import com.jesse.sqlmonitor.indicator_record.repository.dto.AverageNetworkTraffic;
import com.jesse.sqlmonitor.indicator_record.repository.dto.IndicatorGrowth;
import com.jesse.sqlmonitor.monitor.constants.SizeUnit;
import com.jesse.sqlmonitor.response_body.base.ResponseBase;
import com.jesse.sqlmonitor.response_body.qps_statistics.ExtremeQPS;
import com.jesse.sqlmonitor.response_body.qps_statistics.StandingDeviationQPS;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Objects;

/** 监控日志实体仓储类实现。*/
@Slf4j
@Repository
public class MonitorLogRepositoryImpl implements MonitorLogRepository
{
    /** Jackson 解析器，用于将 JSON 转化成指定的对象实例。*/
    private final ObjectMapper mapper;

    /** 数据库客户端实例。*/
    private final DatabaseClient databaseClient;

    public MonitorLogRepositoryImpl(
        ObjectMapper objectMapper,
        @Qualifier("R2dbcSlaverDatabaseClient")
        DatabaseClient databaseClient
    )
    {
        this.mapper         = objectMapper;
        this.databaseClient = databaseClient;
    }

    @Override
    public Mono<Long>
    deleteOneBatchIndicator(
        @NotNull String serverIP,
        @NotNull LocalDateTime from,
        @NotNull LocalDateTime to,
        long batchSize
    )
    {
        final String deleteSQL
            = """
            DELETE FROM
                monitor_log
            WHERE
                server_ip = INET_ATON(:serverIP)
                AND
                datetime BETWEEN :from AND :to
            LIMIT :batchSize
            """;

        if (batchSize < 0) {
            throw new IllegalArgumentException("Argument batchSize not less then 0");
        }

        return
        this.databaseClient
            .sql(deleteSQL)
            .bind("serverIP",  serverIP)
            .bind("from",      from.atZone(ZoneId.systemDefault()).toInstant())
            .bind("to",        to.atZone(ZoneId.systemDefault()).toInstant())
            .bind("batchSize", batchSize)
            .fetch()
            .rowsUpdated();
    }

    @Override
    public Flux<FetchIndicatorResult>
    fetchIndicator(
        @NotNull Class<? extends ResponseBase<?>> type,
        @NotNull String serverIP,
        @NotNull LocalDateTime from,
        @NotNull LocalDateTime to,
        @NotNull QueryOrder queryOrder,
        long limit, long offset
    )
    {
        /*
         * MySQL 的
         *     INET_NTOA() 用于将 无符号整数 转化为 IPV4 字符串
         *     INET_ATON() 用于将 IPV4 字符串 转化为 无符号整数
         */
        final String querySQL
            = """
              SELECT
                  datetime,
                  indicator,
                  INET_NTOA(server_ip) AS server_ip_str,
                  indicator_type
              FROM
                  monitor_log
              WHERE
                  indicator_type = :indicatorType
                  AND
                  server_ip = INET_ATON(:serverIP)
                  AND
                  datetime BETWEEN :from AND :to
              ORDER BY
                 datetime %s
              LIMIT :limit OFFSET :offset
              """.formatted(queryOrder.name());

        return
        this.databaseClient
            .sql(querySQL)
            .bind("indicatorType", IndicatorType.valueOf(type.getSimpleName()))
            .bind("serverIP",      serverIP)
            .bind("from",          from.atZone(ZoneId.systemDefault()).toInstant())
            .bind("to",            to.atZone(ZoneId.systemDefault()).toInstant())
            .bind("limit",         limit)
            .bind("offset",        offset)
            .fetch()
            .all()
            .map((queryResult) -> {
                try
                {
                    return
                    new FetchIndicatorResult(
                        (LocalDateTime) queryResult.get("datetime"),
                        this.mapper
                            .readValue((String) queryResult.get("indicator"), type),
                        (String) queryResult.get("server_ip_str"),
                        IndicatorType.valueOf((String) queryResult.get("indicator_type"))
                    );
                }
                catch (JsonProcessingException exception)
                {
                    throw new
                    QueryIndicatorFailed(
                        exception.getMessage(), exception
                    );
                }
            });
    }

    public Mono<Long>
    getIndicatorCount(
        @NotNull Class<? extends ResponseBase<?>> type,
        @NotNull String        serverIP,
        @NotNull LocalDateTime from,
        @NotNull LocalDateTime to
    )
    {
        final String querySQL
            = """
              SELECT
                  COUNT(*) AS count
              FROM
                  monitor_log
              WHERE
                  indicator_type = :indicatorType
                  AND
                  server_ip = INET_ATON(:serverIP)
                  AND
                  datetime BETWEEN :from AND :to
              """;

        return
        this.databaseClient
            .sql(querySQL)
            .bind("indicatorType", type.getSimpleName())
            .bind("serverIP",      serverIP)
            .bind("from",          from.atZone(ZoneId.systemDefault()).toInstant())
            .bind("to",            to.atZone(ZoneId.systemDefault()).toInstant())
            .fetch()
            .one()
            .map((result) ->
                (Long) result.get("count"));
    }

    @Override
    public Mono<IndicatorGrowth>
    getIndicatorIncrement(
        @NotNull String        serverIP,
        @NotNull LocalDateTime start
    )
    {
        final String indicatorGrowthSQL
            = """
            SELECT
                COUNT(*)    AS growth_data_points,
                NOW()       AS check_time
            FROM
            	sql_monitor.monitor_log
            WHERE
                `server_ip` = INET_ATON(:serverIP)
                AND
            	`datetime` BETWEEN :startPoint AND NOW()
            """;

        return
        this.databaseClient
            .sql(indicatorGrowthSQL)
            .bind("startPoint", start)
            .bind("serverIP", serverIP)
            .fetch()
            .one()
            .map((result) ->
                IndicatorGrowth.builder()
                    .growthDataPoints((Long)   result.get("growth_data_points"))
                    .checkTime((LocalDateTime) result.get("check_time"))
                    .build()
            );
    }

    @Override
    public Mono<Double>
    getAverageQPS(
        @NotNull String        serverIP,
        @NotNull LocalDateTime from,
        @NotNull LocalDateTime to
    )
    {
        final String qpsAverageSQL
            = """
            SELECT
                AVG(`qps_value`) AS average_qps
            FROM
            	sql_monitor.monitor_log
            WHERE
            	`indicator_type` = 'QPSResult'
                AND
                `server_ip` = INET_ATON(:serverIP)
                AND
                `datetime` BETWEEN :from AND :to
            """;

        return
        this.databaseClient
            .sql(qpsAverageSQL)
            .bind("serverIP", serverIP)
            .bind("from", from.atZone(ZoneId.systemDefault()).toInstant())
            .bind("to",   to.atZone(ZoneId.systemDefault()).toInstant())
            .fetch()
            .one()
            .map((average) -> {
                BigDecimal averageQPS = ((BigDecimal) average.get("average_qps"));

                return
                Objects.isNull(averageQPS)
                    ? -1.00
                    : averageQPS.doubleValue();
            });
    }

    @Override
    public Mono<Double>
    getMedianQPS(
        @NotNull String        serverIP,
        @NotNull LocalDateTime from,
        @NotNull LocalDateTime to
    )
    {
        final String qpsMedianSQL
            = """
            SELECT
            	AVG(qps) AS median_qps
            FROM (
                SELECT
            		`qps_value` 							 AS qps,
                    ROW_NUMBER() OVER (ORDER BY `qps_value`) AS row_index,
                    COUNT(*)     OVER () 					 AS total_rows
                FROM
            		sql_monitor.monitor_log
                WHERE
            		`indicator_type` = 'QPSResult'
                    AND
                    `server_ip` = INET_ATON(:serverIP)
                    AND
                    `datetime` BETWEEN :from AND :to
            ) AS sorted
            WHERE
            	row_index IN (FLOOR((total_rows + 1) / 2), FLOOR((total_rows + 2) / 2))
            """;

        return
        this.databaseClient
            .sql(qpsMedianSQL)
            .bind("serverIP", serverIP)
            .bind("from", from.atZone(ZoneId.systemDefault()).toInstant())
            .bind("to",   to.atZone(ZoneId.systemDefault()).toInstant())
            .fetch()
            .one()
            .map((average) -> {
                BigDecimal medianQPS = ((BigDecimal) average.get("median_qps"));

                return
                Objects.isNull(medianQPS)
                    ? -1.00
                    : medianQPS.doubleValue();
            });
    }

    @Override
    public Mono<ExtremeQPS>
    getExtremeQPS(
        @NotNull String        serverIP,
        @NotNull LocalDateTime from,
        @NotNull LocalDateTime to
    )
    {
        final String qpsExtremeSQL
            = """
            SELECT
                MAX(`qps_value`) AS max_qps,
                MIN(`qps_value`) AS min_qps
            FROM
            	sql_monitor.monitor_log
            WHERE
            	`indicator_type` = 'QPSResult'
                AND
                `server_ip` = INET_ATON(:serverIP)
                AND
                `datetime` BETWEEN :from AND :to
            """;

        return
        this.databaseClient
            .sql(qpsExtremeSQL)
            .bind("serverIP", serverIP)
            .bind("from", from.atZone(ZoneId.systemDefault()).toInstant())
            .bind("to",   to.atZone(ZoneId.systemDefault()).toInstant())
            .fetch()
            .one()
            .map((result) -> {
                BigDecimal maxQPS = ((BigDecimal) result.get("max_qps"));
                BigDecimal minQPS = ((BigDecimal) result.get("min_qps"));

                return
                ExtremeQPS.builder()
                    .max((Objects.isNull(maxQPS)) ? -1.00 : maxQPS.doubleValue())
                    .min((Objects.isNull(minQPS)) ? -1.00 : minQPS.doubleValue())
                    .build();
            });
    }

    @Override
    public Mono<StandingDeviationQPS>
    getStandingDeviationQPS(
        @NotNull String        serverIP,
        @NotNull LocalDateTime from,
        @NotNull LocalDateTime to
    )
    {
        final String qpsStddevSQL
            = """
            SELECT
                STDDEV_POP(`qps_value`) AS QPS_stddev,
                AVG(`qps_value`)        AS QPS_avg,
                COUNT(*)                AS data_points
            FROM
            	sql_monitor.monitor_log
            WHERE
            	`indicator_type` = 'QPSResult'
                AND
                `server_ip` = INET_ATON(:serverIP)
                AND
                `datetime` BETWEEN :from AND :to
            """;

        return
        this.databaseClient
            .sql(qpsStddevSQL)
            .bind("serverIP", serverIP)
            .bind("from", from.atZone(ZoneId.systemDefault()).toInstant())
            .bind("to",   to.atZone(ZoneId.systemDefault()).toInstant())
            .fetch()
            .one()
            .map((result) -> {
                final double stddev
                    = Objects.isNull(result.get("QPS_stddev"))
                        ? -1.00 : (Double) result.get("QPS_stddev");

                final double avg
                    = Objects.isNull(result.get("QPS_avg"))
                        ? -1.00 : ((BigDecimal) result.get("QPS_avg")).doubleValue();

                final double loadStability
                    = (stddev == -1.00 || avg == -1.00) ? -1.00 : stddev / avg;

                final long dataPoints
                    = (Long) result.get("data_points");

                return
                StandingDeviationQPS.builder()
                    .stddev(stddev)
                    .loadStability(loadStability)
                    .dataPoints(dataPoints)
                    .build();
            });
    }

    @Override
    public Mono<AverageNetworkTraffic>
    getAverageNetworkTraffic(
        @NotNull String        serverIP,
        @NotNull LocalDateTime from,
        @NotNull LocalDateTime to
    )
    {
        final String networkTrafficAverageSQL
            = """
            SELECT
                AVG(`sentPerSec`)    AS average_sent,
                AVG(`receivePerSec`) AS average_receive
            FROM
            	sql_monitor.monitor_log
            WHERE
            	`indicator_type` = 'NetWorkTraffic'
                AND
                `server_ip` = INET_ATON(:serverIP)
                AND
                `datetime` BETWEEN :from AND :to
            """;

        return
        this.databaseClient
            .sql(networkTrafficAverageSQL)
            .bind("serverIP", serverIP)
            .bind("from", from.atZone(ZoneId.systemDefault()).toInstant())
            .bind("to",   to.atZone(ZoneId.systemDefault()).toInstant())
            .fetch()
            .one()
            .map((averageTraffic) -> {
                BigDecimal averageSent
                    = (BigDecimal) averageTraffic.get("average_sent");

                BigDecimal averageReceive
                    = (BigDecimal) averageTraffic.get("average_receive");

                return
                AverageNetworkTraffic.builder()
                    .averageSent(
                        (Objects.isNull(averageSent))
                            ? -1.00 : averageSent.doubleValue())
                    .averageReceived(
                        (Objects.isNull(averageReceive))
                            ? -1.00 : averageReceive.doubleValue())
                    .unit(SizeUnit.KB)
                    .build();
            });
    }
}