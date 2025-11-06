package com.jesse.indicator_receiver.entity;

/** 数据库指标类型枚举。*/
public enum IndicatorType
{
    /** 连接使用指标类型。 */
    ConnectionUsage,

    /** 库/表 大小指标类型。（似乎暂时没有用到）*/
    DatabaseSize,

    /** InnoDB 缓存命中率指标类型。*/
    InnodbBufferCacheHitRate,

    /** 网络流量指标类型。*/
    NetWorkTraffic,

    /** QPS 指标类型。*/
    QPSResult
}