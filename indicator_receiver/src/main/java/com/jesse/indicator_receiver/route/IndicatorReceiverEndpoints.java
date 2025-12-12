package com.jesse.indicator_receiver.route;

/** 指标数据接收器服务端点配置类。*/
public class IndicatorReceiverEndpoints
{
    public static final String
    ROOT = "/api/indicator_receiver";

    public static final String
    START_RECEIVER = "/start";

    public static final String
    STOP_RECEIVER  = "/stop";

    public static final String
    RUN_STATUS     = "/run-status";
}