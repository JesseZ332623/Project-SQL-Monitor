package com.jesse.sqlmonitor.component_test;

import cn.hutool.core.util.RandomUtil;
import com.jesse.sqlmonitor.luascript_reader.LuaScriptReader;
import com.jesse.sqlmonitor.luascript_reader.impl.LuaOperatorResult;
import com.jesse.sqlmonitor.luascript_reader.impl.LuaScriptOperatorType;
import com.jesse.sqlmonitor.properties.R2dbcMasterProperties;
import com.jesse.sqlmonitor.properties.RedisCacheProperties;
import com.jesse.sqlmonitor.utils.PrettyJSONPrinter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * {@link LuaScriptReader} 简单测试，
 * 旨在展示读取器和 Lua 脚本的相关标准用法。
 */
@SpringBootTest
public class LuaScriptReaderTest
{
    @Autowired
    private R2dbcMasterProperties masterProperties;

    @Autowired
    private RedisCacheProperties cacheProperties;

    @Autowired
    private LuaScriptReader luaScriptReader;

    @Autowired
    private ReactiveRedisTemplate<String, LuaOperatorResult> scriptTemmplate;

    /** 测试 Redis-Lua 脚本对 CJSON 的支持（终于不用手动拼接 JSON 字符串了）。*/
    @Test
    public void luaScriptReaderTest()
    {
        Flux.interval(Duration.ZERO, Duration.ofMillis(100L))
            .take(5L)
            .concatMap((tick) ->
                this.luaScriptReader
                    .read(LuaScriptOperatorType.TEST_SCRIPT, "test-script-01.lua")
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

    /** 测试 Redis-Lua 脚本如何处理 {@link Map} 类型的数据。*/
    @Test
    public void luaScriptReaderMapTest()
    {
        final String redisKey
            = this.cacheProperties.getKeyPrefix() + this.masterProperties.getHost() + ":hash-map";

        final Map<String, Object> testMap
            = Map.of("name", "Jesse", "age", 114);

        System.out.println(redisKey);

        this.luaScriptReader
            .read(LuaScriptOperatorType.TEST_SCRIPT, "test-script-02.lua")
            .flatMap((script) ->
                this.scriptTemmplate
                    .execute(script, List.of(redisKey), testMap)
                    .timeout(Duration.ofSeconds(1L))
                    .next()
                    .map(PrettyJSONPrinter::getPrettyFormatJSON)
                    .doOnSuccess(System.out::println)
            ).block();
    }
}