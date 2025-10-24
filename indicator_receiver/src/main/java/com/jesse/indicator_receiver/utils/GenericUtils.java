package com.jesse.indicator_receiver.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/** 项目全局工具类。*/
@NoArgsConstructor(access  = AccessLevel.PRIVATE)
final public class GenericUtils
{
    /** 检查 URL 中的参数是否为空。*/
    public static boolean
    isEmptyParam(String param) {
        return Objects.isNull(param) || param.isEmpty();
    }

    /**
     * 从 com.jesse.sqlmonitor.response_body.InnodbBufferCacheHitRate
     * 提取 InnodbBufferCacheHitRate 的字串作为类名。
     */
    public static @NotNull String
    extractClassName(String className)
    {
        if (isEmptyParam(className)) {
            throw new IllegalArgumentException("Class name is empty or null!");
        }

        int lastPointPos = className.lastIndexOf(".");

        if (lastPointPos == -1) {
            throw new IllegalStateException("Invalid class name!");
        }

        return
        className.substring(lastPointPos + 1);
    }
}