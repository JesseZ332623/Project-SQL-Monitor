package com.jesse.sqlmonitor;

import com.jesse.sqlmonitor.dto.RandomJokeDTO;
import io.github.jessez332623.redis_lock.distributed_lock.RedisDistributedLock;
import io.github.jessez332623.redis_lock.fair_semaphore.RedisFairSemaphore;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
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

/** 自己搓的 Redis 分布式锁的简单测试。*/
@Slf4j
@SpringBootTest
public class DistributedLockTest
{
    private final static
    long TEST_AMOUNTS = 20;

    /** Redis 公平信号量接口。*/
    @Autowired
    private RedisFairSemaphore fairSemaphore;

    /** Redis 分布式互斥锁接口。*/
    @Autowired
    private RedisDistributedLock distributedLock;

    @Autowired
    @Qualifier("R2dbcMasterDatabaseClient")
    private DatabaseClient databaseClient;

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
    public void distributedLockTest()
    {
        Mono<String> getDistributeLock
            = this.distributedLock
                  .withLock(
                      "test-mutex-01",
                      Duration.ofMillis(500L),
                      Duration.ofSeconds(5L),
                      (identification) -> {
                          System.out.println(identification);
                          return this.getDatabaseTime();
                      })
            .doOnSuccess(System.out::println);

        Mono.delay(Duration.ofSeconds(3L)).block();

        Flux.interval(Duration.ofMillis(200L))
            .take(TEST_AMOUNTS)
            .flatMap((no) -> {
                System.out.println("No. " + no);
                return getDistributeLock;
            }, 16)
            .doOnComplete(() ->
                log.debug(
                "(Distributed-Lock) Total: {}, {}",
                TEST_AMOUNTS, this.distributedLock.getStatisticResultString()
                )
            )
            .blockLast();
    }

    @Test
    public void fairSemaphoreTest()
    {
        Mono<String> getFairSemaphoreMono
            = this.fairSemaphore
                  .withFairSemaphore(
                      "test-fair-semaphore-01",
                      3, Duration.ofSeconds(15L),
                      (identifier) ->
                          this.getRandomJokeFromInternet()
                  )
            .doOnSuccess(System.out::println);

        Mono.delay(Duration.ofSeconds(3L)).block();

        Flux.interval(Duration.ofSeconds(1L))
            .take(TEST_AMOUNTS)
            .flatMap((no) -> {
                System.out.println("No. " + no);
                return getFairSemaphoreMono;
            }, 16)
            .doOnComplete(() ->
                log.debug(
                    "(Fair-Semaphore) Total: {}, {}",
                    TEST_AMOUNTS, this.fairSemaphore.getStatisticResultString()
                )
            )
            .blockLast();
    }
}