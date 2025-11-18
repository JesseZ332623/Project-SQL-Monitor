package com.jesse.sqlmonitor.properties;

import lombok.Data;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

/** Redis 缓存相关属性。*/
@Data
@ToString
@Component
@ConfigurationProperties(prefix = "app.redis-cache")
public class RedisCacheProperties
{
    /** 所有指标缓存数据的前缀命名空间。*/
    private String keyPrefix;

    /**
     * 缓存的有效期（为前端的最短查询间隔 - 500 毫秒的冗余）
     * 确保正确的触发缓存更新，避免读到旧数据。
     */
    private Duration ttl;
}