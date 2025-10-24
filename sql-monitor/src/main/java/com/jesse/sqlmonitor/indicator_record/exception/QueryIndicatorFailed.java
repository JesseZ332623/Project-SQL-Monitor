package com.jesse.sqlmonitor.indicator_record.exception;

/** 当查询指标失败时最终抛出本异常。*/
public class QueryIndicatorFailed extends RuntimeException
{
    public QueryIndicatorFailed(String message) {
        super(message);
    }
    public QueryIndicatorFailed(String message, Throwable cause) {
        super(message, cause);
    }
}
