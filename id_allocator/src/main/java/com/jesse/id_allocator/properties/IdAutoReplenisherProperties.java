package com.jesse.id_allocator.properties;

import lombok.Data;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

/** ID 自动给补货器属性配置类。*/
@Data
@ToString
@Component
@ConfigurationProperties(prefix = "app.id-replenisher")
public class IdAutoReplenisherProperties
{
    /** ID 列表键名。*/
    private String idListKey;

    /** 期望队列中保持的 ID 数量。*/
    private int targetBuffer;

    /** 第一次补货操作前的延迟时间。*/
    private Duration startDelay;

    /** 补货操作的间隔。*/
    private Duration interval;

    /** 重启 ID 自动补货服务的延迟时间。*/
    private Duration restartDelay;
}