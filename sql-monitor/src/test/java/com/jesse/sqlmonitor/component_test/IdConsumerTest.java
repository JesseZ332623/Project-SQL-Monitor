package com.jesse.sqlmonitor.component_test;

import com.jesse.sqlmonitor.utils.GlobalIdConsumer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;

/** ID 消费机测试用例。*/
@SpringBootTest
public class IdConsumerTest
{
    @Autowired
    private GlobalIdConsumer idConsumer;

    /** 尝试单个消费。*/
    @Test
    public void getNextIdTest()
    {
        Flux.interval(Duration.ofMillis(50), Duration.ofMillis(200))
            .flatMap((tick) ->
                this.idConsumer.nextId()
                    .doOnSuccess(System.out::println))
            .take(15L)
            .subscribeOn(Schedulers.boundedElastic())
            .blockLast();
    }

    /** 尝试批量消费。*/
    @Test
    public void getNextBatchIdTest()
    {
        Flux.interval(Duration.ofMillis(50), Duration.ofMillis(200))
            .flatMap((tick) ->
                this.idConsumer.nextBatchId(10)
                    .doOnSuccess(System.out::println))
            .take(15L)
            .subscribeOn(Schedulers.boundedElastic())
            .blockLast();
    }
}
