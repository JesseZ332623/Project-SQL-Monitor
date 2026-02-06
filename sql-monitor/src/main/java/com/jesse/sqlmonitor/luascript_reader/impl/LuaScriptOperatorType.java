package com.jesse.sqlmonitor.luascript_reader.impl;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 要读取的 Lua 脚本的类型枚举。*/
@RequiredArgsConstructor
public enum LuaScriptOperatorType
{
    // SOME_TYPES("...")
    /** 测试用。*/
    TEST_SCRIPT("test"),

    /** 缓存操作 Lua 脚本。*/
    INDICATOR_CACHER("indicator-cacher"),

    /** 服务实例 Worker ID 操作脚本。*/
    WORKER_ID_ALLOC("worker-id-allocator"),

    /** 其他 Lua 脚本。*/
    OTHERS("others");

    @Getter
    private final String typeName;
}