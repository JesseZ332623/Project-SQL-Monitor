package com.jesse.sqlmonitor.route.endpoints_config;

/** 手动执行定时任务端点配置类。*/
public class ScheduledTasksEndpoints
{
    public static final
    String ROOT                       = "/api/scheduled-task";

    public static final
    String SEND_INDICATOR_REPORT      = "/indicator-report";

    public static final
    String CLEAN_HISTORICAL_INDICATOR = "/historical-indicator";
}