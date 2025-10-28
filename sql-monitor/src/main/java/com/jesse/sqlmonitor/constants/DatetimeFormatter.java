package com.jesse.sqlmonitor.constants;

import com.jesse.sqlmonitor.indicator_record.exception.QueryIndicatorFailed;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;

/** 日期格式化器。*/
public class DatetimeFormatter
{
    private static final
    List<DateTimeFormatter> DATETIME_FORMATTERS
        = Arrays.asList(
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),   // 完整格式
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),      // 不含秒
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH"),         // ...
        DateTimeFormatter.ofPattern("yyyy-MM-dd"),
        DateTimeFormatter.ofPattern("yyyy-MM"),
        DateTimeFormatter.ofPattern("yyyy")
    );

    public static @NotNull LocalDateTime
    parseDatetime(String dateTimeStr)
    {
        for (DateTimeFormatter formatter : DATETIME_FORMATTERS)
        {
            try {
                return LocalDateTime.parse(dateTimeStr, formatter);
            }
            catch (DateTimeParseException e) { /* 尝试下一个格式 */ }
        }

        throw new
        QueryIndicatorFailed("Could not parse date time: " + dateTimeStr);
    }
}
