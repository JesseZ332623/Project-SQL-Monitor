package com.jesse.sqlmonitor.route.endpoints_config;

/** SQL 指标监控程序端点配置。*/
public class SQLMonitorEndPoints
{
    private static final String ROOT = "/api/sql-monitor";

    public static final
    String BASE_ADDRESS_QUERY = ROOT + "/base-address";

    public static final
    String QPS_QUERY = ROOT + "/qps";

    public static final
    String NETWORK_TRAFFIC_QUERY = ROOT + "/network-traffic";

    public static final
    String GLOBAL_STATUS_QUERY = ROOT + "/global-status";

    public static final
    String CONNECTION_USAGE_QUERY = ROOT + "/connection-usage";

    public static final
    String DATABASE_SIZE_QUERY = ROOT + "/database-size";

    public static final
    String INNODB_BUFFER_CACHE_HIT_RATE_QUERY
        = ROOT + "/cache-hit-rate";

    public static final
    String SERVER_UPTIME_QUERY = ROOT + "/running-time";
}