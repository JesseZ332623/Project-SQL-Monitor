package com.jesse.sqlmonitor.indicator_record.exception;

/** 往数据库写入指标日志出错时最终抛出本异常。*/
public class RecordIndicatorFailed extends RuntimeException
{
    public RecordIndicatorFailed(String message) {
        super(message);
    }
    public RecordIndicatorFailed(String message, Throwable cause) {
        super(message, cause);
    }
}
