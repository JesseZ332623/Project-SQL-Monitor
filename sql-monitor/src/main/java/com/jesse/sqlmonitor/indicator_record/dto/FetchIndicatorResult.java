package com.jesse.sqlmonitor.indicator_record.dto;

import com.jesse.sqlmonitor.constants.QueryOrder;
import com.jesse.sqlmonitor.indicator_record.entity.IndicatorType;
import com.jesse.sqlmonitor.indicator_record.repository.MonitorLogRepository;
import com.jesse.sqlmonitor.monitor.cacher.IndicatorCacher;
import com.jesse.sqlmonitor.response_body.base.ResponseBase;
import lombok.*;

import java.time.LocalDateTime;

/**
 * {@link MonitorLogRepository#fetchIndicator(Class, String, LocalDateTime, LocalDateTime, QueryOrder, long, long)}
 * 的执行结果 DTO。
 */
@Getter
@ToString
@EqualsAndHashCode
@RequiredArgsConstructor(access = AccessLevel.PUBLIC)
public class FetchIndicatorResult
{
    /**
     * 指标数据被
     * {@link IndicatorCacher#sendIndicatorToTaskQueue(ResponseBase, Class)}
     * 发送的那一刻所记录的时间点信息。
     */
    private final LocalDateTime   datetime;

    /** 指标数据本体。*/
    private final ResponseBase<?> indicator;

    /** 指标数据所属的数据库 IP。*/
    private final String          serverIP;

    /** 指标数据类型枚举。*/
    private final IndicatorType   indicatorType;
}
