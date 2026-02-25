package com.jesse.email_receiver.properties;

import lombok.Data;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

/** 邮件消费服务相关属性类。*/
@Data
@ToString
@Component
@ConfigurationProperties(prefix = "app.email-receiver")
public class EmailReceiverProperties
{
    /** 被消费的邮件队列名。*/
    private String consumQueueName;

    /** 单个消费者最多暂存的未确认消息数量。*/
    private int prefetchCount;

    /** 重启消费者服务的延迟时间。*/
    private Duration restartDelay;

    /** 取消订阅前闭锁的最大延迟时间。*/
    private Duration shutdownDelay;
}