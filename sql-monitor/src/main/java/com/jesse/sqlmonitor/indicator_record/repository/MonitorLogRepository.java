package com.jesse.sqlmonitor.indicator_record.repository;

import com.jesse.sqlmonitor.constants.QueryOrder;
import com.jesse.sqlmonitor.indicator_record.dto.FetchIndicatorResult;
import com.jesse.sqlmonitor.indicator_record.repository.dto.AverageNetworkTraffic;
import com.jesse.sqlmonitor.indicator_record.repository.dto.IndicatorGrowth;
import com.jesse.sqlmonitor.response_body.base.ResponseBase;
import com.jesse.sqlmonitor.response_body.qps_statistics.ExtremeQPS;
import com.jesse.sqlmonitor.response_body.qps_statistics.StandingDeviationQPS;
import com.jesse.sqlmonitor.scheduled_tasks.HistoricalIndicatorCleaner;
import org.jetbrains.annotations.NotNull;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

/** 监控日志实体仓储类接口。*/
public interface MonitorLogRepository
{
    /**
     * 删除指定 IP 的下，两个时间点之间的指定批次的指标数据。
     * （被定时任务：{@link HistoricalIndicatorCleaner} 调用）。
     *
     * @param serverIP   数据库服务器 IP
     * @param from       开始日期
     * @param to         截止日期
     * @param batchSize  每个批次的大小（不得小于 0）
     *
     * @return 返回删除的行数（用于最终求和）
     */
    Mono<Long>
    deleteOneBatchIndicator(
        @NotNull String        serverIP,
        @NotNull LocalDateTime from,
        @NotNull LocalDateTime to,
        long batchSize
    );

    /**
     * 读取两个时间点之间指定 IP 下所有指定类型的监控日志记录（按时间排序）。
     *
     * @param type       指标结果类型
     * @param serverIP   被监控的数据库 IP
     * @param from       起始时间点
     * @param to         截止时间点
     * @param queryOrder 由远及近 / 由近及远
     * @param limit      查询条数限制
     * @param offset     查询结果集偏移量
     *
     * @return 发布所有匹配监控日志记录的 {@link Flux}
     */
    Flux<FetchIndicatorResult>
    fetchIndicator(
        @NotNull Class<? extends ResponseBase<?>> type,
        @NotNull String        serverIP,
        @NotNull LocalDateTime from,
        @NotNull LocalDateTime to,
        @NotNull QueryOrder    queryOrder,
        long limit, long offset
    );

    /**
     * 读取两个时间点之间指定 IP 下指定类型的指标数据量。
     *
     * @param type       指标结果类型
     * @param serverIP   被监控的数据库 IP
     * @param from       起始时间点
     * @param to         截止时间点
     */
    Mono<Long>
    getIndicatorCount(
        @NotNull Class<? extends ResponseBase<?>> type,
        @NotNull String        serverIP,
        @NotNull LocalDateTime from,
        @NotNull LocalDateTime to
    );

    /**
     * 查询指定 IP 指定时间点起到现在的指标增长数。
     *
     * @param serverIP   被监控的数据库 IP
     * @param start      开始时间点
     *
     * @return 指标增长数
     */
    Mono<IndicatorGrowth>
    getIndicatorIncrement(
        @NotNull String        serverIP,
        @NotNull LocalDateTime start
    );

    /**
     * 查询并计算指定 IP 指定时间段内的 QPS 平均值。
     *
     * @param serverIP   被监控的数据库 IP
     * @param from       起始时间点
     * @param to         截止时间点
     *
     * @return QPS 平均值，如果返回 -1.00 则表示时间段内无 QPS 指标数据
     */
    Mono<Double>
    getAverageQPS(
        @NotNull String        serverIP,
        @NotNull LocalDateTime from,
        @NotNull LocalDateTime to
    );

    /**
     * 查询并计算指定 IP 指定时间段内的 QPS 中位数。
     *
     * @param serverIP   被监控的数据库 IP
     * @param from       起始时间点
     * @param to         截止时间点
     *
     * @return QPS 中位数，如果返回 -1.00 则表示时间段内无 QPS 指标数据
     */
    Mono<Double>
    getMedianQPS(
        @NotNull String        serverIP,
        @NotNull LocalDateTime from,
        @NotNull LocalDateTime to
    );

    /**
     * 查询并计算指定 IP 指定时间段内的 QPS 极值。
     *
     * @param serverIP   被监控的数据库 IP
     * @param from       起始时间点
     * @param to         截止时间点
     *
     * @return QPS 极值，如果所有字段值皆为 -1.00 则表示时间段内无 QPS 指标数据
     */
    Mono<ExtremeQPS>
    getExtremeQPS(
        @NotNull String        serverIP,
        @NotNull LocalDateTime from,
        @NotNull LocalDateTime to
    );

    /**
     * 查询并计算指定 IP 指定时间段内的 QPS 标准差。
     *
     * @param serverIP   被监控的数据库 IP
     * @param from       起始时间点
     * @param to         截止时间点
     *
     * @return QPS 标准差，如果所有字段值皆为 -1.00 则表示时间段内无 QPS 指标数据
     */
    Mono<StandingDeviationQPS>
    getStandingDeviationQPS(
        @NotNull String        serverIP,
        @NotNull LocalDateTime from,
        @NotNull LocalDateTime to
    );

    /**
     * 查询并计算指定 IP 指定时间段内的 数据库网络流量 平均值（单位：Kb/s）。
     *
     * @param serverIP   被监控的数据库 IP
     * @param from       起始时间点
     * @param to         截止时间点
     *
     * @return 网络吞吐量平均值，
     * 如果所有字段值皆为 -1.00 则表示时间段内无数据库网络流量指标数据
     */
    Mono<AverageNetworkTraffic>
    getAverageNetworkTraffic(
        @NotNull String        serverIP,
        @NotNull LocalDateTime from,
        @NotNull LocalDateTime to
    );
}