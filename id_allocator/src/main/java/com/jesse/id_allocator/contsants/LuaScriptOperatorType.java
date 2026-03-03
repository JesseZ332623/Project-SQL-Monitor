package com.jesse.id_allocator.contsants;

import io.github.jessez332623.reactive_luascript_reader.impl.LuaScriptCatalogue;
import lombok.RequiredArgsConstructor;

/** 要读取的 Lua 脚本的类型枚举。*/
@RequiredArgsConstructor
public enum LuaScriptOperatorType implements LuaScriptCatalogue
{
    /** 服务实例 Worker ID 操作脚本。*/
    WORKER_ID_ALLOC("worker-id-allocator"),

    /** ID 自动补货相关脚本。*/
    ID_AUTO_REPLENISHER("id-auto-replenisher"),

    /** 其他脚本。*/
    OTHERS("others");

    private final String catalogue;

    @Override
    public String getCatalogue() {
        return this.catalogue;
    }
}