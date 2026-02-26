package com.jesse.email_receiver.route;

/** 邮件接收发送器端点配置类。*/
public class EmailSenderEndpoints
{
    public static final String
    ROOT = "/api/email_receiver";

    public static final String
    START_RECEIVER = "/start";

    public static final String
    STOP_RECEIVER  = "/stop";

    public static final String
    RUN_STATUS     = "/run-status";
}