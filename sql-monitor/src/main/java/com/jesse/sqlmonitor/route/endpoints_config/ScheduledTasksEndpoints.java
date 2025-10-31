package com.jesse.sqlmonitor.route.endpoints_config;

/** 手动执行定时任务端点配置类。*/
public class ScheduledTasksEndpoints
{
    private static final String ROOT = "/api/scheduled-task";

    public static final String
    SEND_INDICATOR_REPORT = ROOT + "/indicator-report";

    public static final String
    CLEAN_HISTORICAL_INDICATOR = ROOT + "/historical-indicator";
}