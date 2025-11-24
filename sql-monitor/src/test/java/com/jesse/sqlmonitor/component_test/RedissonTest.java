package com.jesse.sqlmonitor.component_test;

import cn.hutool.core.util.IdUtil;
import com.jesse.sqlmonitor.component_test.dto.RandomJokeDTO;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.redisson.Redisson;
import org.redisson.api.RLockReactive;
import org.redisson.api.RedissonReactiveClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.netty.transport.ProxyProvider;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/** Redisson 分布式锁简单测试。*/
@SpringBootTest
public class RedissonTest
{
    /**
     * 随机笑话，响应体：
     * {
     *   "type": "general",
     *   "setup": "What did the dog say to the two trees?",
     *   "punchline": "Bark bark.",
     *   "id": 170
     * }
     */
    private final static
    String RANDOM_JOKE_URL = "https://official-joke-api.appspot.com/random_joke";

    @Autowired
    @Qualifier("R2dbcMasterDatabaseClient")
    private DatabaseClient databaseClient;

    private RedissonReactiveClient redissonReactiveClient = null;

    public RedissonReactiveClient
    getRedissonReactiveClient()
    {
        if (Objects.isNull(this.redissonReactiveClient))
        {
            Config singleServerConfig = new Config();

            singleServerConfig
                .useSingleServer()
                .setAddress("redis://localhost:6379")
                .setUsername("Jesse")
                .setPassword("1234567890")
                .setConnectionPoolSize(64)
                .setConnectionMinimumIdleSize(32)
                .setRetryAttempts(3)
                .setKeepAlive(true);

            this.redissonReactiveClient
                = Redisson.create(singleServerConfig).reactive();

        }

        return this.redissonReactiveClient;
    }

    /** 挂上了代理的 HTTP 客户端实例。*/
    private final
    WebClient webClient
        = WebClient.builder()
            .clientConnector(new ReactorClientHttpConnector(
                HttpClient.create()
                    .proxy((proxy) ->
                        proxy.type(ProxyProvider.Proxy.SOCKS5)
                            .host("localhost")
                            .port(10808)
                    )
            )).build();

    private @NotNull Mono<String> getDatabaseTime()
    {
        return
        this.databaseClient
            .sql("SELECT NOW() AS now")
            .fetch()
            .one()
            .timeout(Duration.ofSeconds(5L))
            .map((result) ->
                ((LocalDateTime) result.get("now"))
                    .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            );
    }

    private @NotNull Mono<String>
    getRandomJokeFromInternet()
    {
        return
            this.webClient
                .get()
                .uri(RANDOM_JOKE_URL)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(RandomJokeDTO.class)
                .timeout(Duration.ofSeconds(5L))
                .map((joke) ->
                    String.format(
                        "%s %s\n",
                        joke.getSetup(), joke.getPunchline()
                    )
                );
    }

    @Test
    public void distributionLockTest()
    {
        RLockReactive lockReactive
            = this.getRedissonReactiveClient().getLock("test-redisson-lock");

        final long contextThreadId = IdUtil.getSnowflakeNextId();

        Flux.interval(Duration.ZERO, Duration.ofMillis(1500L))
            .take(10L)
            .concatMap((sequence) ->
                Mono.fromRunnable(() -> System.out.println("No. " + sequence))
                    .then(
                        Mono.usingWhen(
                            lockReactive.tryLock(500L, -1L, TimeUnit.MILLISECONDS, contextThreadId),
                            (isLocked) -> {
                                if (isLocked)
                                {
                                    return
                                    Mono.defer(() ->
                                        Mono.when(
                                            this.getRandomJokeFromInternet()
                                                .doOnSuccess(System.out::println),
                                            this.getDatabaseTime()
                                                .doOnSuccess(System.out::println)
                                        )
                                    );
                                }
                                else
                                {
                                    return
                                    Mono.fromRunnable(() ->
                                        System.out.println("Try to acquire lock timeout!")
                                    );
                                }
                            },
                            (ignore) ->
                                lockReactive.unlock(contextThreadId)
                        )
                    )
            )
            .collectList()
            .block();
    }
}