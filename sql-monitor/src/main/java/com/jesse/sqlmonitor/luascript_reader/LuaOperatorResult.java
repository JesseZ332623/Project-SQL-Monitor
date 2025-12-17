package com.jesse.sqlmonitor.luascript_reader;

import lombok.*;

/** Lua 脚本的执行结果是一个 JSON 类型，会被映射成该 POJO。*/
@Getter
@ToString
@RequiredArgsConstructor(access = AccessLevel.PUBLIC)
public class LuaOperatorResult
{
    /** Lua 脚本执行状态。*/
    private final String status;

    /** Lua 脚本执行完成时的时间戳。*/
    private final long timestamp;
}