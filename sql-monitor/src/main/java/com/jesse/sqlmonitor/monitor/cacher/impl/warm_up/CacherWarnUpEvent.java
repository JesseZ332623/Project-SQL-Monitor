package com.jesse.sqlmonitor.monitor.cacher.impl.warm_up;

import com.jesse.sqlmonitor.monitor.constants.IndicatorKeyNames;
import com.jesse.sqlmonitor.response_body.base.ResponseBase;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 缓存预热事件的定义，
 * 按照 Spring 标准需要继承自 {@link ApplicationEvent}。
 */
@Getter
public class CacherWarnUpEvent<T extends ResponseBase<T>>
    extends ApplicationEvent
{
    private final IndicatorKeyNames keyNames; // 缓存数据键名
    private final ResponseBase<T>   data;     // 缓存数据实例
    private final Class<T>          type;     // 缓存数据实际类型令牌

    /**
     * 缓存预热事件构造器。
     *
     * @param source  事件发布者实例引用
     * @param keyName 缓存数据键名
     * @param data    缓存数据实例
     * @param type    缓存数据实际类型令牌
     */
    public CacherWarnUpEvent(
        Object            source,
        IndicatorKeyNames keyName,
        ResponseBase<T>   data,
        Class<T>          type
    )
    {
        super(source);
        this.keyNames = keyName;
        this.data     = data;
        this.type     = type;
    }
}