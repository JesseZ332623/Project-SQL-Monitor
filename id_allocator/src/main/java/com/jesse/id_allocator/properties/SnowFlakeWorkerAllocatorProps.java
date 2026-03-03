package com.jesse.id_allocator.properties;

import lombok.Data;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

/** Snowflake Worker ID 分配器属性配置类。*/
@Data
@ToString
@Component
@ConfigurationProperties(prefix = "app.snow-worker-id-alloc")
public class SnowFlakeWorkerAllocatorProps
{
    /** Worker ID 键前缀。*/
    private String lockKeyPrefix;

    /** 获取 Wroker ID 的时间限制，超过这个时间服务就启动失败。*/
    private Duration acquireTimeout;

    /** Worker ID 租赁时间。*/
    private Duration lease;

    /** Worker ID 续租时间间隔（建议为 lease 的三分之一左右）。*/
    private Duration renewInterval;

    /** 续租最大尝试次数。*/
    private Integer maxRenewRetries;

    /** 续租尝试指数退避起始间隔。*/
    private Duration backoffStart;
}