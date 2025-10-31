package com.jesse.sqlmonitor.indicator_record.repository;

import com.jesse.sqlmonitor.constants.QueryOrder;
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
     * 删除指定 IP 的下，until 时间点之前的指定批次的指标数据。
     * （被定时任务：{@link HistoricalIndicatorCleaner} 调用）。
     *
     * @param serverIP   数据库服务器 IP
     * @param until      截止日期
     * @param batchSize  每个批次的大小
     *
     * @return 返回删除的行数（用于最终求和）
     */
    Mono<Long>
    deleteOneBatchIndicator(String serverIP, LocalDateTime until, long batchSize);

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
    Flux<? extends ResponseBase<?>>
    fetchIndicator(
        @NotNull Class<? extends ResponseBase<?>> type,
        String serverIP,
        @NotNull LocalDateTime until,
        @NotNull QueryOrder queryOrder
    );

    /** 查询指定 IP 指定时间点到现在的指标增长数。*/
    Mono<IndicatorGrowth>
    getIndicatorIncrement(String serverIP, LocalDateTime start);

    /** 查询并计算指定 IP 指定时间段内的 QPS 平均值。*/
    Mono<Double>
    getAverageQPS(String serverIP, LocalDateTime until);

    /** 查询并计算指定 IP 指定时间段内的 QPS 中位数。*/
    Mono<Double>
    getMedianQPS(String serverIP, LocalDateTime until);

    /** 查询并计算指定 IP 指定时间段内的 QPS 极值。*/
    Mono<ExtremeQPS>
    getExtremeQPS(String serverIP, LocalDateTime until);

    /** 查询并计算指定 IP 指定时间段内的 QPS 标准差。*/
    Mono<StandingDeviationQPS>
    getStandingDeviationQPS(String serverIP, LocalDateTime until);

    /** 查询并计算指定 IP 指定时间段内的 数据库网络流量 平均值（单位：Kb/s）。*/
    Mono<AverageNetworkTraffic>
    getAverageNetworkTraffic(String serverIP, LocalDateTime until);
}