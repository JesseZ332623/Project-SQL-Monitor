package com.jesse.sqlmonitor.indicator_record.repository.dto;

import lombok.*;

import java.time.LocalDateTime;

/** 表示指定 IP 指定时间段内的指标增长数的 DTO。*/
@Getter
@Builder
@ToString
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class IndicatorGrowth
{
    private long          growthDataPoints;
    private LocalDateTime checkTime;
}