package com.jesse.sqlmonitor.scheduled_tasks.dto;

import lombok.*;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/** 自动清理历史指标数据的结果的 DTO。*/
@Data
@ToString
@NoArgsConstructor(access  = AccessLevel.PUBLIC)
@AllArgsConstructor(access = AccessLevel.PUBLIC)
public class CleanUpResult
{
    private String        serverIp;     // 对哪个 IP 的数据执行了删除？
    private LocalDateTime oneWeekAgo;   // 一个星期前的具体时间点是？
    private AtomicLong    totalDeleted; // 共计删掉了多少行数据？
    private AtomicInteger batchCount;   // 分几个批次删除的？

    /** 初始化一个批量删除结果。*/
    @Contract("_, _ -> new")
    public static @NotNull CleanUpResult
    init(String serverIp, LocalDateTime oneWeekAgo)
    {
        return new
        CleanUpResult(
            serverIp, oneWeekAgo,
            new AtomicLong(0L),
            new AtomicInteger(0)
        );
    }
}