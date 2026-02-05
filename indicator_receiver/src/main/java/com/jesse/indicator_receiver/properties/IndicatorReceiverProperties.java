package com.jesse.indicator_receiver.properties;

import lombok.Data;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

/** 来自配置文件的指标接收器相关属性。*/
@Data
@ToString
@Component
@ConfigurationProperties(prefix = "app.indicator-receiver")
public class IndicatorReceiverProperties
{
    /** 单个消费者最多暂存的未确认消息数量。*/
    private int prefetchCount;

    /** 指标数据缓存数量。*/
    private int bufferSize;

    /** 若超过这个时间段直接刷新缓冲区。*/
    private Duration bufferTimeout;

    /** 批量插入操作时间限制。*/
    private Duration batchInsertTimeout;

    /** 取消订阅前的延迟时间。*/
    private Duration shutdownDelay;
}