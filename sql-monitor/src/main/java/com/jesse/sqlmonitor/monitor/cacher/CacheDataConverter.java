package com.jesse.sqlmonitor.monitor.cacher;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jesse.sqlmonitor.response_body.base.ResponseBase;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import reactor.core.publisher.Mono;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 缓存指标数据转换器。</br>
 * {@literal (Map <=> <T extends ResponseBase<T>>)}
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final public class CacheDataConverter
{
    /**
     * 安全的类型转换操作，
     * 从基类 {@link ResponseBase} 转化成它指定的子类，并包装为 {@link Mono}。
     *
     * @param indicator 指标数据实例（此时为基类）
     * @param type      要转化成的子类类型令牌
     *
     * @return 转化后的子类型
     *
     * @throws ClassCastException
     * 如果类型令牌呈示的类型不是 {@literal ResponseBase<T>} 的子类时仍进行转换，则抛出本异常
     */
    @SuppressWarnings("unchecked")
    public static <T extends ResponseBase<T>>
    @NotNull Mono<T>
    safeCast(@NotNull ResponseBase<?> indicator, @NotNull Class<T> type)
    {
        if (type.isInstance(indicator)) {
            return Mono.just((T) indicator);
        }

        return
        Mono.error(
            new ClassCastException(
                "Can not cast" + indicator.getClass() + "to" + type + "!"
            )
        );
    }

    /**
     * 将指定类型的指标结果对象转换成 {@link LinkedHashMap} 方便存入 Redis，
     * 使用 LinkedHashMap 是为了保证字段之间的顺序不变。
     *
     * @param indicatorBase 指标数据实例（此时为基类）
     * @param type          要转化成的子类类型令牌
     * @param objectMapper  外部传入的 Jackson 对象映射器
     *
     * @return 转换完成的，发射 {@link LinkedHashMap} 类型的 {@link Mono}
     */
    @SuppressWarnings("unchecked")
    public static <T extends ResponseBase<T>>
    @NotNull Mono<Map<String, Object>>
    makeCacheDataFromIndicator(
        @NotNull ResponseBase<T> indicatorBase,
        @NotNull Class<T>        type,
        @NotNull ObjectMapper    objectMapper
    )
    {

        return
        CacheDataConverter.safeCast(indicatorBase, type)
            .map((indicator) ->
                objectMapper.convertValue(indicator, LinkedHashMap.class));
    }

    /**
     * 将 Redis 中的指标数据 Map 转化成对应的指标数据实例。
     *
     * @param indicatorMap 指标数据映射表
     * @param type          要转化成的子类类型令牌
     * @param objectMapper  外部传入的 Jackson 对象映射器
     *
     * @return 转换完成的，发射 {@link ResponseBase<T>} 的子类的 {@link Mono}
     */
    public static <T extends ResponseBase<T>>
    @NotNull Mono<T> restoreIndicatorMapToInstance(
        @NotNull Map<String, Object> indicatorMap,
        @NotNull Class<T>            type,
        @NotNull ObjectMapper        objectMapper
    )
    {
        /*
         * objectMapper.convertValue() 并不是一步到位的，
         * 内部估计也是要转化成 JSON 然后再转化成 Map，
         * 所以类型信息需要我们补上。
         *
         * type.isAnnotationPresent(JsonTypeInfo.class)
         * 调用检查这个类型或者父类型是否被传入的注解类注解
         */
        if (type.isAnnotationPresent(JsonTypeInfo.class))
        {
            // 反射大法，
            // 拿到这个类型或者父类型的 @JsonTypeInfo 注解的 property 属性值。
            String property
                = type.getAnnotation(JsonTypeInfo.class).property();

            // 添加类型信息，示例：<String: "type", String: "QPSResult">
            indicatorMap.put(property, type.getSimpleName());
        }

        return
        Mono.just(objectMapper.convertValue(indicatorMap, type));
    }
}