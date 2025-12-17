package com.jesse.sqlmonitor.component_test;

import cn.hutool.core.util.RandomUtil;
import com.jesse.sqlmonitor.luascript_reader.LuaOperatorResult;
import com.jesse.sqlmonitor.luascript_reader.LuaScriptOperatorType;
import com.jesse.sqlmonitor.luascript_reader.LuaScriptReader;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.List;

/**
 * {@link LuaScriptReader} 简单测试，
 * 旨在展示标准用法和 Redis Lua 脚本对 CJSON 的支持。
 */
@SpringBootTest
public class LuaScriptReaderTest
{
    @Autowired
    private LuaScriptReader luaScriptReader;

    @Autowired
    private ReactiveRedisTemplate<String, LuaOperatorResult> scriptTemmplate;

    @Test
    public void luaScriptReaderTest()
    {
        Flux.interval(Duration.ZERO, Duration.ofMillis(100L))
            .take(5L)
            .concatMap((tick) ->
                this.luaScriptReader
                    .read(LuaScriptOperatorType.TEST_SCRIPT, "test-script.lua")
                    .flatMap((script) ->
                        this.scriptTemmplate
                            .execute(script, List.of(), RandomUtil.randomInt(100, 200))
                            .timeout(Duration.ofSeconds(1L))
                            .next()
                            .doOnNext((result) -> {
                                switch (result.getStatus())
                                {
                                    case "SUCCESS"     -> System.out.println(result);
                                    case "TEST_FIALED" -> System.err.println(result);
                                    case null, default -> System.err.println("Unexcepted status...");
                                }
                            }))
                    .then()
            )
            .blockLast();
    }
}