package com.jesse.sqlmonitor.luascript_reader;

import com.jesse.sqlmonitor.luascript_reader.exception.LuaScriptLoadFailed;
import com.jesse.sqlmonitor.luascript_reader.exception.LuaScriptNotFound;
import com.jesse.sqlmonitor.luascript_reader.exception.LuaScriptSecurityException;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static java.lang.String.format;

/** Redis Lua 脚本读取器（实现脚本实例缓存功能）。*/
@Slf4j
@Component
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@RequiredArgsConstructor(access = AccessLevel.PUBLIC)
final public class LuaScriptReader
{
    /** 本项目 Lua 脚本根目录（从配置中读取）。*/
    @Value("{app.lua-root-classpath}")
    private String LUA_SCRIPT_ROOT_CLASSPATH;

    /** Lua 脚本缓存：operatorType -> (scriptName -> script) */
    private final ConcurrentMap<
        LuaScriptOperatorType,
        ConcurrentMap<String, DefaultRedisScript<LuaOperatorResult>>>
        scriptCache = new ConcurrentHashMap<>();

    /** 从 classpath 中加载脚本。*/
    @Contract("_, _ -> new")
    private @NotNull
    DefaultRedisScript<LuaOperatorResult>
    loadFromClassPath(@NotNull LuaScriptOperatorType operatorType, String luaScriptName) throws IOException
    {
        // 拼接 Lua 脚本的 classpath，
        // 格式：lua-script/{operator-type}/{script-name.lua}
        final Path scriptClasspath
            = Paths.get(LUA_SCRIPT_ROOT_CLASSPATH)
                   .resolve(operatorType.getTypeName())
                   .resolve(luaScriptName)
                   .normalize();

        if (scriptClasspath.startsWith(Paths.get(LUA_SCRIPT_ROOT_CLASSPATH).normalize()))
        {
            final String scriptClasspathStr
                = scriptClasspath.toString();

            try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(scriptClasspathStr))
            {
                if (Objects.isNull(inputStream))
                {
                    throw new
                    LuaScriptNotFound(
                        format("Lua script: %s not found!", scriptClasspathStr)
                    );
                }

                return new
                DefaultRedisScript<>(
                    new String(inputStream.readAllBytes(), StandardCharsets.UTF_8),
                    LuaOperatorResult.class
                );
            }
        }
        else
        {
            throw new
            LuaScriptSecurityException(
                format("Illegal Lua script classpath attempt! (%s)", scriptClasspath),
                scriptClasspath
            );
        }
    }

    /** 获取或者创建指定操作类型的脚本缓存。*/
    private ConcurrentMap<String, DefaultRedisScript<LuaOperatorResult>>
    getOrCreateScriptCache(LuaScriptOperatorType scriptOperatorType)
    {
        return
        this.scriptCache.computeIfAbsent(
            scriptOperatorType,
            (scriptName) -> new ConcurrentHashMap<>()
        );
    }

    /**
     * 通过操作类型 + 脚本名尝试从缓存中获取指定的
     * {@link DefaultRedisScript<LuaOperatorResult>}，没有则从 classpath 中加载然后缓存。
     *
     * @param operatorType  Lua 脚本类型
     * @param luaScriptName Lua 脚本名
     *
     * @return 由 {@link DefaultRedisScript} 包装的，
     *         发布 Lua 脚本执行结果 {@link LuaOperatorResult} 的 {@link Mono}
     */
    private @NotNull Mono<DefaultRedisScript<LuaOperatorResult>>
    getScriptFromCache(LuaScriptOperatorType operatorType, String luaScriptName)
    {
        return
        Mono.fromCallable(() -> {
            final ConcurrentMap<String, DefaultRedisScript<LuaOperatorResult>>
                operatorCache = this.getOrCreateScriptCache(operatorType);

            // 调用 computeIfAbsent() 方法，
            // 存在则直接返回，不存在执行 mappingFunction 缓存后再返回。
            return
            operatorCache.computeIfAbsent(
                luaScriptName,
                (scriptName) -> {
                    try
                    {
                        return
                        this.loadFromClassPath(operatorType, luaScriptName);
                    }
                    catch (IOException exception)
                    {
                        throw new
                        LuaScriptLoadFailed(
                            format(
                                "Load lua script %s failed! Caused by: %s",
                                luaScriptName, exception.getMessage()),
                            exception
                        );
                    }
                }
            );
        });
    }

    /**
     * 根据配置，读取 Lua 脚本并包装。
     *
     * @param operatorType  Lua 脚本类型
     * @param luaScriptName Lua 脚本名
     *
     * @return 由 {@link DefaultRedisScript} 包装的，
     *         发布 Lua 脚本执行结果 {@link LuaOperatorResult} 的 {@link Mono}
     */
    public @NotNull Mono<DefaultRedisScript<LuaOperatorResult>>
    read(LuaScriptOperatorType operatorType, String luaScriptName)
    {
        return
        this.getScriptFromCache(operatorType, luaScriptName);
    }
}