package com.jesse.sqlmonitor.monitor.constants;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 数据库指标类型令牌枚举。*/
@RequiredArgsConstructor
public enum IndicatorKeyNames
{
    /** 连接使用指标缓存键名。 */
    ConnectionUsageKey("connection-usage"),

    /** InnoDB 缓存命中率指标缓存名。*/
    InnodbBufferCacheHitRateKey("innodb-buffer-cache-rate"),

    /** 网络流量指标缓存名。*/
    NetWorkTrafficKey("network-traffic"),

    /** QPS 指标缓存名。*/
    QPSResultKey("qps");

    @Getter
    private final String keyName;
}