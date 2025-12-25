package com.jesse.sqlmonitor.properties;

import com.jesse.sqlmonitor.scheduled_tasks.HistoricalIndicatorCleaner;
import lombok.*;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 历史指标数据删除任务相关属性类，
 * 给 {@link HistoricalIndicatorCleaner} 用。
 */
@Data
@ToString
@Component
@RequiredArgsConstructor
@ConfigurationProperties(prefix = "app.historical-indicator-cleaner")
public class HistoricalIndicatorCleanerProps
{
    /** 总删除条数阈值，超过该阈值要向运维发送邮件。*/
    private Long totalDeleteLimit;

    /** 一个批次的数据条数。*/
    private Long batchSize;

    /** 批量删除超时时间段阈值。*/
    private Duration batchDeleteTimeout;
}