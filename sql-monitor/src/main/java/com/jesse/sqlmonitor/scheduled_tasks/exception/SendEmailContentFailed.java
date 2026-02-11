package com.jesse.sqlmonitor.scheduled_tasks.exception;

/** 邮件内容发往消息队列失败时，抛出本异常。*/
public class SendEmailContentFailed extends RuntimeException
{
    public SendEmailContentFailed(String message) {
        super(message);
    }

    public SendEmailContentFailed(String message, Throwable throwable) {
        super(message, throwable);
    }
}
