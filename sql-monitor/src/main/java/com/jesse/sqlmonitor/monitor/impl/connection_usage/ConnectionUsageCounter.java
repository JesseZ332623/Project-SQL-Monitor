package com.jesse.sqlmonitor.monitor.impl.connection_usage;

import com.jesse.sqlmonitor.response_body.ConnectionUsage;
import reactor.core.publisher.Mono;

/** 数据库连接使用数据计算器接口。*/
public interface ConnectionUsageCounter
{
    Mono<ConnectionUsage> getConnectionUsage();
}