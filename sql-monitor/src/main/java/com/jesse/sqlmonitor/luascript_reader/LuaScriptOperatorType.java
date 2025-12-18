package com.jesse.sqlmonitor.luascript_reader;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 要读取的 Lua 脚本的类型枚举。*/
@RequiredArgsConstructor
public enum LuaScriptOperatorType
{
    // SOME_TYPES("...")
    /** 测试用。*/
    TEST_SCRIPT("test");

    @Getter
    private final String typeName;
}