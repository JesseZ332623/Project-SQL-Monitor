package com.jesse.sqlmonitor.component_test;

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

    /**
     * 将指标数据转化成 Map 发往 Redis，
     * 再从 Redis 读出转化回指标数据并输出，确保整个流程正常。
     */
    @Test
    public void indicatorCacherTest()
    {
        this.indicatorCacher
            .cacheIndicatorData(
                IndicatorKeyNames.QPSResultKey,
                QPSResult.buildZeroQPS(),
                QPSResult.class
            )
            .then(
                this.indicatorCacher
                    .getIndicatorCache(IndicatorKeyNames.QPSResultKey, QPSResult.class)
                    .doOnSuccess(System.out::println))
            .block();
    }
}