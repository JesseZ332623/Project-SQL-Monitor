package com.jesse.sqlmonitor;

import com.jesse.sqlmonitor.monitor.cacher.IndicatorCacher;
import com.jesse.sqlmonitor.monitor.constants.IndicatorKeyNames;
import com.jesse.sqlmonitor.response_body.QPSResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/** 指标数据缓存器测试用例。*/
@SpringBootTest
public class IndicatorCacherTest
{
    @Autowired
    private IndicatorCacher indicatorCacher;

    @Test
    public void indicatorCacherTest()
    {
        this.indicatorCacher
            .cacheIndicatorData(
                IndicatorKeyNames.QPSResultKey,
                QPSResult.buildZeroQPS(),
                QPSResult.class)
            .then(
                this.indicatorCacher
                    .getIndicatorCache(IndicatorKeyNames.QPSResultKey, QPSResult.class)
                    .doOnSuccess(System.out::println))
            .block();
    }
}