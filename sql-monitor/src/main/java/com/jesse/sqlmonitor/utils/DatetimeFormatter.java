package com.jesse.sqlmonitor.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** 日期格式化器。*/
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final public class DatetimeFormatter
{
    private static final
    Map<String, List<DateTimeFormatter>> DATETIME_FORMATTERS
        = Map.of(
            "datetime", Arrays.asList(
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"),
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),   // 完整格式
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),      // 不含秒
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH")),        // ...
            "date", Arrays.asList(
                DateTimeFormatter.ofPattern("yyyy-MM-dd"),
                DateTimeFormatter.ofPattern("yyyy-MM"),
                DateTimeFormatter.ofPattern("yyyy")
            )
    );

    /** 获取当前时间字符串（明确使用本地时区，精确到毫秒级）。*/
    public static @NotNull String NOW()
    {
        return
        LocalDateTime.now(ZoneId.systemDefault())
            .format(DATETIME_FORMATTERS.get("datetime").getFirst());
    }

    /**
     * 将传入字符串解析成 {@link LocalDateTime}，
     * 如果只有日期而没有时间的，时间部分全部补零。
     *
     * @throws IllegalArgumentException 遭遇所有预设格式之外的字符串时抛出
     */
    public static @NotNull LocalDateTime
    parseDatetime(String dateTimeStr)
    {
        if (Objects.isNull(dateTimeStr) || dateTimeStr.isEmpty())
        {
            throw new
            IllegalArgumentException("Could not parse null or empty string...");
        }

        String trimDatetimeStr = dateTimeStr.trim();

        for (DateTimeFormatter dateTimeFormatter : DATETIME_FORMATTERS.get("datetime"))
        {
            try {
                return LocalDateTime.parse(trimDatetimeStr, dateTimeFormatter);
            } catch (DateTimeParseException e) { /* 尝试下一个格式 */ }
        }

        for (DateTimeFormatter dateFormatter : DATETIME_FORMATTERS.get("date"))
        {
            try {
                return
                LocalDate.parse(trimDatetimeStr, dateFormatter)
                         .atStartOfDay();
            } catch (DateTimeParseException e) { /* 尝试下一个格式 */ }
        }

        throw new
        IllegalArgumentException("Could not parse: " + trimDatetimeStr);
    }
}