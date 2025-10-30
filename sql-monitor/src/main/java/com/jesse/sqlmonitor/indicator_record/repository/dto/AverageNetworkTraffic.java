package com.jesse.sqlmonitor.indicator_record.repository.dto;

import com.jesse.sqlmonitor.monitor.constants.SizeUnit;
import lombok.*;

/** 表示指定 IP 指定时间段内的 数据库网络流量 平均值的 DTO。*/
@Getter
@Builder
@ToString
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class AverageNetworkTraffic
{
    private Double   averageSent;
    private Double   averageReceived;
    private SizeUnit unit;
}