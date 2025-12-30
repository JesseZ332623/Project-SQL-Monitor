package com.jesse.gatling.endpoints;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;

/** SQL 指标监控程序端点配置。*/
@RequiredArgsConstructor
public enum SQLMonitorEndPoints
{
    ROOT("/api/sql-monitor"),
    QPS_QUERY("/qps"),
    NETWORK_TRAFFIC_QUERY("/network-traffic"),
    CONNECTION_USAGE_QUERY("/connection-usage"),
    INNODB_BUFFER_CACHE_HIT_RATE_QUERY("/cache-hit-rate");

    public static @NonNull String
    concat(@NonNull SQLMonitorEndPoints endPoint)
    {
        if (endPoint.equals(ROOT))
        {
            throw new IllegalArgumentException(
                "Parmeter endPoint not be ROOT!"
            );
        }

        return ROOT.getEndpoint() + endPoint.getEndpoint();
    }

    @Getter
    private final String endpoint;
}