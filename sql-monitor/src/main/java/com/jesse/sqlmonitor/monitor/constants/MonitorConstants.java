package com.jesse.sqlmonitor.monitor.constants;

/** 指标监控相关常量。*/
final public class MonitorConstants
{
    /** 指标计算最多重试次数。*/
    public static final
    int MAX_RETRIES = 10;

    /** 两次查询间隔的最小时间（单位：毫秒）*/
    public static final
    long MIN_TIME_DIFF_MS = 500L;

    /** 前 3 回的快照结果需要忽略。*/
    public static final
    int IGNORE_SNAPSHOTS = 3;
}