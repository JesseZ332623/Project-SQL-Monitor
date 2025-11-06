package com.jesse.indicator_receiver.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 时间配置属性值 -> {@link Duration} 转换器。
 * Spring 自带时间配置属性值的转换，所以这个工具类保留但是不使用。
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final public class DurationPropertiesConverter
{
    private static final
    Pattern DURATION_PATTERN
        = Pattern.compile("^(\\d+)(ms|ns|s|h|d|m)$");

    private static Duration
    selectDuration(Long value, String uint)
    {
        return switch (uint)
        {
            case "ns" -> Duration.ofNanos(value);
            case "ms" -> Duration.ofMillis(value);
            case "s"  -> Duration.ofSeconds(value);
            case "m"  -> Duration.ofMinutes(value);
            case "h"  -> Duration.ofHours(value);
            case "d"  -> Duration.ofDays(value);
            case null, default ->
                throw new
                IllegalArgumentException(
                    String.format("Duration unit %s is invalid!", uint)
                );
        };
    }

    public static Duration
    convert(@NotNull String properties)
    {
        Objects.requireNonNull(
            properties,
            "Duration properties not be null!"
        );

        Matcher matcher
            = DURATION_PATTERN.matcher(properties);

        if (matcher.matches())
        {
            final long number = Long.parseLong(matcher.group(1));
            final String unit = matcher.group(2);

            if (number < 0)
            {
                throw new
                IllegalArgumentException(
                    String.format(
                        "Duration value must be positive! Current: %s",
                        number
                    )
                );
            }

            return
            selectDuration(number, unit);
        }
        else
        {
            throw new
            IllegalArgumentException(
                String.format(
                    "Duration properties %s is invalid!",
                    properties
                )
            );
        }
    }
}