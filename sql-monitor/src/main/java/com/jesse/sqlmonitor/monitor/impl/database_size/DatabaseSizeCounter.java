package com.jesse.sqlmonitor.monitor.impl.database_size;

import com.jesse.sqlmonitor.constants.QueryOrder;
import com.jesse.sqlmonitor.response_body.DatabaseSize;
import reactor.core.publisher.Mono;

import java.util.Map;

/** 数据库大小计算器接口。*/
public interface DatabaseSizeCounter
{
    Mono<Map<String, DatabaseSize>>
    getDatabaseSizeInfo(String schemaName, QueryOrder queryOrder);
}
