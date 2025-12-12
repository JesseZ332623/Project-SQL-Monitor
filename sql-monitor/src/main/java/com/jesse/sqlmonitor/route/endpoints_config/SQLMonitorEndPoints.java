package com.jesse.sqlmonitor.route.endpoints_config;

/** SQL 指标监控程序端点配置。*/
public class SQLMonitorEndPoints
{
    public static final
    String ROOT                     = "/api/sql-monitor";

    public static final
    String BASE_ADDRESS_QUERY       = "/base-address";

    public static final
    String QPS_QUERY                = "/qps";

    public static final
    String NETWORK_TRAFFIC_QUERY    = "/network-traffic";

    public static final
    String GLOBAL_STATUS_QUERY      = "/global-status";

    public static final
    String CONNECTION_USAGE_QUERY   = "/connection-usage";

    public static final
    String ALL_SCHEMA_NAME_QUERY    = "/all-schema-name";

    public static final
    String DATABASE_SIZE_QUERY      = "/database-size";

    public static final
    String SERVER_UPTIME_QUERY      = "/running-time";

    public static final
    String INNODB_BUFFER_CACHE_HIT_RATE_QUERY
        = "/cache-hit-rate";
}