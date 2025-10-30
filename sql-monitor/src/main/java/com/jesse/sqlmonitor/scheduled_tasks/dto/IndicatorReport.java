package com.jesse.sqlmonitor.scheduled_tasks.dto;

import com.jesse.sqlmonitor.indicator_record.repository.dto.AverageNetworkTraffic;
import com.jesse.sqlmonitor.indicator_record.repository.dto.IndicatorGrowth;
import com.jesse.sqlmonitor.response_body.ConnectionUsage;
import com.jesse.sqlmonitor.response_body.qps_statistics.ExtremeQPS;
import com.jesse.sqlmonitor.response_body.qps_statistics.StandingDeviationQPS;
import lombok.*;

/** 定期数据库指标报告 DTO。*/
@Getter
@ToString
@Builder
@NoArgsConstructor(access  = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class IndicatorReport
{
    /** 指标数据增长量 */
    private IndicatorGrowth indicatorGrowth;

    /** QPS 平均值 */
    private Double averageQPS;

    /** QPS 中位数 */
    private Double medianQPS;

    /** QPS 极值 */
    private ExtremeQPS extremeQPS;

    /** QPS 标准差和负载均衡率 */
    private StandingDeviationQPS standingDeviationQPS;

    /** 数据库网络流量 平均值 */
    private AverageNetworkTraffic averageNetworkTraffic;

    /** 连接使用率 */
    private ConnectionUsage connectionUsage;
}