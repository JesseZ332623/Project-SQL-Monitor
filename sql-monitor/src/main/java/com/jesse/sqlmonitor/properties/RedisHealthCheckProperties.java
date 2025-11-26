package com.jesse.sqlmonitor.properties;

import lombok.Data;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

/** Redis 健康检查属性类。*/
@Data
@ToString
@Component
@ConfigurationProperties(prefix = "app.redis-health-check")
public class RedisHealthCheckProperties
{
    /** 健康检查间隔。*/
    private Duration checkInterval;

    /** PING 操作的超时时间。*/
    private Duration pingTimeout;
}