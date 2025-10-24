package com.jesse.sqlmonitor.indicator_record.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jesse.sqlmonitor.constants.QueryOrder;
import com.jesse.sqlmonitor.indicator_record.entity.IndicatorType;
import com.jesse.sqlmonitor.indicator_record.exception.QueryIndicatorFailed;
import com.jesse.sqlmonitor.response_body.qps.ExtremeQPS;
import com.jesse.sqlmonitor.response_body.qps.StandingDeviationQPS;
import com.jesse.sqlmonitor.response_body.base.ResponseBase;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.ZoneId;

import static com.jesse.sqlmonitor.utils.SQLMonitorUtils.extractClassName;

/** 监控日志实体仓储类。*/
@Slf4j
@Repository
public class MonitorLogRepository // extends R2dbcRepository<MonitorLog, Long>
{
    /** Jackson 解析器，用于将 JSON 转化成指定的对象实例。*/
    private final ObjectMapper mapper;

    /** 数据库客户端实例。*/
    private final DatabaseClient databaseClient;

    public MonitorLogRepository(
        ObjectMapper objectMapper,
        @Qualifier("R2dbcSlaverDatabaseClient")
        DatabaseClient databaseClient
    )
    {
        this.mapper         = objectMapper;
        this.databaseClient = databaseClient;
    }

    /**
     * 读取某个时间点之前的指定 IP 下所有指定类型的监控日志记录（按时间排序）。
     *
     * @param type       指标结果类型
     * @param serverIP   被监控的数据库 IP
     * @param until      截止时间点
     * @param queryOrder 由远及近 / 由近及远
     *
     * @return 发布所有匹配监控日志记录的 {@link Flux}
     */
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

    public Mono<Double>
    getAverageQPS(String serverIP, LocalDateTime until)
    {
        final String qpsAverageSQL
            = """
            SELECT
                AVG(JSON_EXTRACT(`indicator`, '$.qps')) AS average_qps
            FROM
            	sql_monitor.monitor_log
            WHERE
            	`indicator_type` = 'QPSResult'
                AND
                INET_NTOA(`server_ip`) = :serverIP
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
            .map((average) ->
                ((Double) average.get("average_qps"))
            );
    }

    public Mono<Double>
    getMedianQPS(String serverIP, LocalDateTime until)
    {
        final String qpsMedianSQL
            = """
            SELECT
            	AVG(qps) AS median_qps
            FROM (
                SELECT
            		JSON_EXTRACT(`indicator`, '$.qps') 							    AS qps,
                    ROW_NUMBER() OVER (ORDER BY JSON_EXTRACT(`indicator`, '$.qps')) AS row_index,
                    COUNT(*)     OVER () 										    AS total_rows
                FROM
            		sql_monitor.monitor_log
                WHERE
            		`indicator_type` = 'QPSResult'
                    AND
                    INET_NTOA(`server_ip`) = :serverIP
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
            .map((average) ->
                ((Double) average.get("median_qps"))
            );
    }

    public Mono<ExtremeQPS>
    getExtremeQPS(String serverIP, LocalDateTime until)
    {
        final String qpsExtremeSQL
            = """
            SELECT
                MAX(JSON_EXTRACT(`indicator`, '$.qps')) AS max_qps,
                MIN(JSON_EXTRACT(`indicator`, '$.qps')) AS min_qps
            FROM
            	sql_monitor.monitor_log
            WHERE
            	`indicator_type` = 'QPSResult'
                AND
                INET_NTOA(`server_ip`) = :serverIP
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
            .map((result) ->
                ExtremeQPS.builder()
                    .max(Double.parseDouble((String) result.get("max_qps")))
                    .min(Double.parseDouble((String) result.get("min_qps")))
                    .build()
            );
    }

    public Mono<StandingDeviationQPS>
    getStandingDeviationQPS(String serverIP, LocalDateTime until)
    {
        final String qpsStddevSQL
            = """
            SELECT
                STDDEV_POP(JSON_EXTRACT(`indicator`, '$.qps')) AS QPS_stddev,
                AVG(JSON_EXTRACT(`indicator`, '$.qps'))        AS QPS_avg,
                COUNT(*)                                       AS data_points
            FROM
            	sql_monitor.monitor_log
            WHERE
            	`indicator_type` = 'QPSResult'
                AND
                INET_NTOA(`server_ip`) = :serverIP
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
                final double stddev        = (Double) result.get("QPS_stddev");
                final double avg           = (Double) result.get("QPS_avg");
                final double loadStability = stddev / avg;
                final long   dataPoints    = (Long) result.get("data_points");

                return
                StandingDeviationQPS.builder()
                    .stddev(stddev)
                    .loadStability(loadStability)
                    .dataPoints(dataPoints)
                    .build();
            });
    }
}