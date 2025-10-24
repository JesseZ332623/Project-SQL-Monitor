package com.jesse.sqlmonitor.utils;

import io.r2dbc.spi.Row;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/** 项目全局工具类。*/
@NoArgsConstructor(access  = AccessLevel.PRIVATE)
final public class SQLMonitorUtils
{
    /** 尝试对全局状态值做数值转换，失败则使用字符串类型。*/
    public static @Nullable
    Object tryGetNumericValue(String valueStr)
    {
        Object value;

        try {
            value = Long.parseLong(valueStr);
        }
        catch (NumberFormatException exception1)
        {
            try {
                value = Double.parseDouble(valueStr);
            }
            catch (NumberFormatException exception2)
            {
                value = valueStr;
            }
        }

        return value;
    }

    /**
     * 从数据行中提取指定的字段。
     *
     * @param <T>  字段类型
     * @param row  数据行
     * @param name 字段名
     * @param type 字段类型
     *
     * @return 字段值
     */
    public static <T> T
    queryRow(@NotNull Row row, String name, Class<T> type)
    {
        return
        Objects.requireNonNull(row.get(name, type));
    }

    /** 检查 URL 中的参数是否为空。*/
    public static boolean
    isEmptyParam(String param) {
        return Objects.isNull(param) || param.isEmpty();
    }

    /** 安全提取长整型值 */
    public static long extractLongValue(Object data, String key)
    {
        try
        {
            if (data instanceof java.util.Map)
            {
                @SuppressWarnings("unchecked")
                java.util.Map<String, Object> map = (java.util.Map<String, Object>) data;
                Object value = map.get(key);
                if (value instanceof Number) {
                    return ((Number) value).longValue();
                }
            }

            throw new
            IllegalArgumentException("Invalid data format for key: " + key);
        }
        catch (Exception e)
        {
            throw new
            IllegalArgumentException("Failed to extract long value for key: " + key, e);
        }
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

    /** 验证是否为合法的数据库名。*/
    public static String
    isNotValidSchemaName(String schemaName)
    {
        // 只允许字母、数字、下划线
        if (schemaName != null && !schemaName.matches("^[a-zA-Z0-9_]+$"))
        {
            throw new
            IllegalArgumentException(
                String.format(
                    "Schema name: [%s] is invalid! Only (letter / number / underline) allowed!",
                    schemaName
                )
            );
        }

        return schemaName;
    }
}