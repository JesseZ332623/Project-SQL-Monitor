package com.jesse.sqlmonitor.monitor.constants;

/** 指标监控相关常量。*/
public class Constants
{
    /** 指标计算最多重试次数。*/
    public static final
    int MAX_RETRY_TIMES = 10;

    /** 两次查询间隔的最小时间（单位：毫秒）*/
    public static final
    long MIN_TIME_DIFF_MS = 10L;
}