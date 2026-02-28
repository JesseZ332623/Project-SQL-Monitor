package com.jesse.sqlmonitor.constants;

import io.github.jessez332623.reactive_luascript_reader.impl.LuaScriptCatalogue;
import lombok.RequiredArgsConstructor;

/** 要读取的 Lua 脚本的类型枚举。*/
@RequiredArgsConstructor
public enum LuaScriptOperatorType implements LuaScriptCatalogue
{
    /** 测试用。*/
    TEST_SCRIPT("test"),

    /** 缓存操作 Lua 脚本。*/
    INDICATOR_CACHER("indicator-cacher"),

    /** 服务实例 Worker ID 操作脚本。*/
    WORKER_ID_ALLOC("worker-id-allocator"),

    /** 邮件发送相关脚本。*/
    EMAIL_SEND("email-send"),

    /** 其他脚本。*/
    OTHERS("others");

    private final String catalogue;

    @Override
    public String getCatalogue() {
        return this.catalogue;
    }
}