package com.jesse.sqlmonitor.monitor.cacher.warm_up;

import com.jesse.sqlmonitor.monitor.constants.IndicatorKeyNames;
import com.jesse.sqlmonitor.response_body.base.ResponseBase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** 缓存预热事件发布器。*/
@Slf4j
@Component
@RequiredArgsConstructor
public class CacherWarmUpEventPublisher
{
    /** Spring 框架的事件发布器。*/
    private final
    ApplicationEventPublisher eventPublisher;

    /** 某个类型的缓存数据 -> 预热状态 哈希表。*/
    private final
    Map<IndicatorKeyNames, Boolean>
        warnUpStatus = new ConcurrentHashMap<>();

    /** Redis 服务的健康状态。*/
    private volatile boolean redisHealth = true;

    /** 发布一个缓存预热事件。*/
    public <T extends ResponseBase<T>> void
    publishWarnUpEvent(
        IndicatorKeyNames keyNames,
        ResponseBase<T> data, Class<T> type)
    {
        if (!redisHealth)
        {
            log.debug(
                "Redis is unhealthy, skipping warn-up for: {}",
                keyNames.name()
            );

            return;
        }

        this.eventPublisher.publishEvent(
            new CacherWarnUpEvent<>(keyNames, data, type)
        );
    }

    /** 标记某一类缓存数据预热完成。*/
    public void
    markAsWarnUp(IndicatorKeyNames keyNames)
    {
        if (redisHealth)
        {
            if (this.warnUpStatus.containsKey(keyNames) && this.warnUpStatus.get(keyNames).equals(true)) {
                return;
            }

            this.warnUpStatus.put(keyNames, true);
            log.info("Cache marked as warmed up for key: [{}]", keyNames.name());
        }
    }

    /** 检查某一类缓存数据是否预热完成。*/
    public boolean
    isWarmUp(IndicatorKeyNames keyNames)
    {
        // 如果 Redis 不健康，直接返回 false，即强制走数据库
        if (!redisHealth) {
            return false;
        }

        return
        this.warnUpStatus.getOrDefault(keyNames, false);
    }

    /** 若 Redis 的健康状态变化，所有预热状态需要被清空。*/
    public void
    onRedisStatusChange(boolean isHealthy)
    {
        boolean previousStatus = this.redisHealth;
        this.redisHealth       = isHealthy;

        // Redis 服务从健康到不健康
        if (previousStatus && !isHealthy)
        {
            log.warn("Redis became unhealthy, resetting all warm-up status.");
            this.warnUpStatus.clear();
        }
        // Redis 服务从不健康到健康
        else if(!previousStatus && isHealthy)
        {
            log.info("Redis recovered, resetting warm-up status for fresh start.");
            this.warnUpStatus.clear();
        }
    }
}