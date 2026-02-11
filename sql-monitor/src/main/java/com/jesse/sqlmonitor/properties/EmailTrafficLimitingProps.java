package com.jesse.sqlmonitor.properties;

import lombok.Data;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

/** 邮件发送限流相关属性类。*/
@Data
@ToString
@Component
@ConfigurationProperties(prefix = "app.email-traffic-limiting")
public class EmailTrafficLimitingProps
{
    /** 令牌桶容量。*/
    private Integer burst;

    /** 填充令牌操作的时间间隔。*/
    private Duration fillDuration;

    /** 每个 fill-duration 填充的令牌数量。*/
    private Integer fillTokens;
}