package com.jesse.sqlmonitor.indicator_record.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jesse.sqlmonitor.constants.QueryOrder;
import com.jesse.sqlmonitor.indicator_record.entity.IndicatorType;
import com.jesse.sqlmonitor.indicator_record.exception.QueryIndicatorFailed;
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
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Objects;

import static com.jesse.sqlmonitor.utils.SQLMonitorUtils.extractClassName;

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
    deleteOneBatchIndicator(String serverIP, @NotNull LocalDateTime until, long batchSize)
    {
        final String deleteSQL
            = """
            DELETE FROM
                monitor_log
            WHERE
                server_ip = INET_ATON(:serverIP)
                AND
                datetime <= :until
            ORDER BY log_id DESC
            LIMIT :batchSize
            """;

        return
        this.databaseClient
            .sql(deleteSQL)
            .bind("serverIP",  serverIP)
            .bind("until",     until.atZone(ZoneId.systemDefault()).toInstant())
            .bind("batchSize",  batchSize)
            .fetch()
            .rowsUpdated();
    }

    @Override
    public Flux<? extends ResponseBase<?>>
    fetchIndicator(
        @NotNull Class<? extends ResponseBase<?>> type,
        String serverIP,
        @NotNull LocalDateTime until,
        @NotNull QueryOrder queryOrder
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
                  datetime <= :until
              ORDER BY
                 datetime %s
              """.formatted(queryOrder.name());

        return
        this.databaseClient
            .sql(querySQL)
            .bind("indicatorType", IndicatorType.valueOf(extractClassName(type.getTypeName())))
            .bind("serverIP", serverIP)
            .bind("until", until.atZone(ZoneId.systemDefault()).toInstant())
            .fetch()
            .all()
            .map((queryResult) -> {

                String indicatorJSON
                    = (String) queryResult.get("indicator");

                ResponseBase<?> indicator;
                try
                {
                    indicator
                        = this.mapper
                              .readValue(indicatorJSON, type);
                }
                catch (JsonProcessingException exception)
                {
                    throw new
                    QueryIndicatorFailed(
                        exception.getMessage(), exception
                    );
                }

                return indicator;
            });
    }

    @Override
    public Mono<IndicatorGrowth>
    getIndicatorIncrement(String serverIP, @NotNull LocalDateTime start)
    {
        final String indicatorGrowthSQL
            = """
            SELECT
                COUNT(*)    AS growth_data_points,
                NOW()       AS check_time
            FROM
            	sql_monitor.monitor_log
            WHERE
            	`datetime` >= :startPoint
            	AND `datetime` <= NOW()
                AND `server_ip` = INET_ATON(:serverIP);
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
    getAverageQPS(String serverIP, LocalDateTime until)
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
                `datetime` <= :until
            """;

        return
        this.databaseClient
            .sql(qpsAverageSQL)
            .bind("serverIP", serverIP)
            .bind("until", until)
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
    getMedianQPS(String serverIP, LocalDateTime until)
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
                    `datetime` <= :until
            ) AS sorted
            WHERE
            	row_index IN (FLOOR((total_rows + 1) / 2), FLOOR((total_rows + 2) / 2))
            """;

        return
        this.databaseClient
            .sql(qpsMedianSQL)
            .bind("serverIP", serverIP)
            .bind("until", until)
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
    getExtremeQPS(String serverIP, LocalDateTime until)
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
                `datetime` <= :until
            """;

        return
        this.databaseClient
            .sql(qpsExtremeSQL)
            .bind("serverIP", serverIP)
            .bind("until", until)
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
    getStandingDeviationQPS(String serverIP, LocalDateTime until)
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
                `datetime` <= :until
            """;

        return
        this.databaseClient
            .sql(qpsStddevSQL)
            .bind("serverIP", serverIP)
            .bind("until", until)
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

                final long dataPoints    = (Long) result.get("data_points");

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
    getAverageNetworkTraffic(String serverIP, LocalDateTime until)
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
                `datetime` <= :until
            """;

        return
        this.databaseClient
            .sql(networkTrafficAverageSQL)
            .bind("serverIP", serverIP)
            .bind("until", until)
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