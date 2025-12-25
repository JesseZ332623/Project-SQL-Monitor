package com.jesse.sqlmonitor.scheduled_tasks.constants;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 由于本模块定时任务的 自动 / 手动 执行逻辑极为相似，
 * 考虑使用本枚举来表述定时任务的执行者。
 */
@RequiredArgsConstructor
public enum TaskExecuter
{
    /** 由 Corn 表达式驱动，指定时间点自动调用。*/
    AUTO_TASK("(Auto-Task)"),

    /** 由前端发起 Http 请求，手动调用 */
    HTTP_REQUEST("(Http-Request)");

    @Getter
    private final String executer;
}