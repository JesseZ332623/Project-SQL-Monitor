package com.jesse.sqlmonitor.monitor.cacher.warm_up;

import com.jesse.sqlmonitor.monitor.constants.IndicatorKeyNames;
import com.jesse.sqlmonitor.response_body.base.ResponseBase;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 缓存预热事件的定义。*/
@Getter
@RequiredArgsConstructor
public class CacherWarnUpEvent<T extends ResponseBase<T>>
{
    private final IndicatorKeyNames keyNames; // 缓存数据键名
    private final ResponseBase<T>   data;     // 缓存数据实例
    private final Class<T>          type;     // 缓存数据实际类型令牌
}