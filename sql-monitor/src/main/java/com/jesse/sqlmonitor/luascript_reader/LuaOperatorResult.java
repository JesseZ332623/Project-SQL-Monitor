package com.jesse.sqlmonitor.luascript_reader;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Lua 脚本的执行结果是一个 JSON 类型，会被映射成该 POJO。*/
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LuaOperatorResult
{
    /** Lua 脚本执行状态。*/
    private String status;
}