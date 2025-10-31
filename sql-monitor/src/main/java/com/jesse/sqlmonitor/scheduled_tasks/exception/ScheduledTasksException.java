package com.jesse.sqlmonitor.scheduled_tasks.exception;

/** 当手动调用定时任务失败时，最终抛出本异常向上传递。*/
public class ScheduledTasksException extends RuntimeException
{
    public ScheduledTasksException(String message) {
        super(message);
    }

    public ScheduledTasksException(String message, Throwable throwable) {
        super(message, throwable);
    }
}
