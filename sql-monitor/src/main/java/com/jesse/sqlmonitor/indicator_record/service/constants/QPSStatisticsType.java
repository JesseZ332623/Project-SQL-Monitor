package com.jesse.sqlmonitor.indicator_record.service.constants;

/** QPS 统计数据类型。*/
public enum QPSStatisticsType
{
    AVERAGE,            // QPS 平均值
    MEDIAN_VALUE,       // QPS 中位数
    EXTREME_VALUE,      // QPS 极值
    STANDARD_DEVIATION  // QPS 标准差
}