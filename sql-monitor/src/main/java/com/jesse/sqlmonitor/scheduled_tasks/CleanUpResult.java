package com.jesse.sqlmonitor.scheduled_tasks;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.time.LocalDateTime;

/** 自动清理历史指标数据的结果的 POJO。*/
@Getter
@Builder
@ToString
public class CleanUpResult
{
    private String        serverIp;     // 对哪个 IP 的数据执行了删除？
    private LocalDateTime oneWeekAgo;   // 一个星期前的具体时间点是？
    private Long          effectedRows; // 删掉了多少行指标数据？
}