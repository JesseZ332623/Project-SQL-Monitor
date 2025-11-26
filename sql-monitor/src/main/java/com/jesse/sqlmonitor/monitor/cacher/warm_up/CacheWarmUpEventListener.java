package com.jesse.sqlmonitor.monitor.cacher.warm_up;

import com.jesse.sqlmonitor.monitor.cacher.IndicatorCacher;
import com.jesse.sqlmonitor.monitor.constants.IndicatorKeyNames;
import com.jesse.sqlmonitor.response_body.base.ResponseBase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Objects;

/** 指标预热事件监听器。*/
@Slf4j
@Component
@RequiredArgsConstructor
public class CacheWarmUpEventListener
{
    /** 指标数据缓存器实现。*/
    private final IndicatorCacher indicatorCacher;

    /**
     * 当检查到 {@link CacherWarnUpEvent} 实例被创建时
     *（即调用 {@link CacherWarmUpEventPublisher#publishWarnUpEvent(IndicatorKeyNames, ResponseBase, Class)}）
     * 尝试往缓存写入数据
     */
    @EventListener
    public <T extends ResponseBase<T>> void
    handleCacheWarnUp(@NotNull CacherWarnUpEvent<T> event)
    {
        if (Objects.nonNull(event.getData()))
        {
            ResponseBase<T> data = event.getData();

            this.indicatorCacher
                .cacheIndicatorData(
                    event.getKeyNames(), data, event.getType())
                .subscribe(
                    null,
                    (exception) ->
                        log.warn(
                            "Cache warn up failed for data: {}, Caused by: {}",
                            event.getKeyNames().name(),
                            exception.getMessage()
                        )
                );
        }
    }
}