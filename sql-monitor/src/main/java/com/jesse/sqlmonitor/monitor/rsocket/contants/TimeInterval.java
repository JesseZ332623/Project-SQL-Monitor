package com.jesse.sqlmonitor.monitor.rsocket.contants;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Duration;

/** 表示指定时间间隔的枚举。*/
@AllArgsConstructor
public enum TimeInterval
{
    _3_SECONDS(Duration.ofSeconds(3L)),
    _5_SECONDS(Duration.ofSeconds(5L)),
    _15_SECONDS(Duration.ofSeconds(15L)),
    _30_SECONDS(Duration.ofSeconds(30L)),
    _60_SECONDS(Duration.ofMinutes(1L));

    @Getter
    private final Duration interval;
}