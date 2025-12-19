package com.jesse.sqlmonitor.luascript_reader;

import com.jesse.sqlmonitor.luascript_reader.impl.LuaOperatorResult;
import com.jesse.sqlmonitor.luascript_reader.impl.LuaScriptOperatorType;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import reactor.core.publisher.Mono;

/** Redis Lua 脚本读取器接口。*/
public interface LuaScriptReader
{
    /**
     * 根据配置，读取 Lua 脚本并包装。
     *
     * @param operatorType  Lua 脚本类型
     * @param luaScriptName Lua 脚本名
     *
     * @return 由 {@link DefaultRedisScript} 包装的，
     *         发布 Lua 脚本执行结果 {@link LuaOperatorResult} 的 {@link Mono}
     */
    @NotNull Mono<DefaultRedisScript<LuaOperatorResult>>
    read(LuaScriptOperatorType operatorType, String luaScriptName);
}