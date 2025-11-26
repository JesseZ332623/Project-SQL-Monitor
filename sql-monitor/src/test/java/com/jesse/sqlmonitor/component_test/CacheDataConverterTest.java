package com.jesse.sqlmonitor.component_test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jesse.sqlmonitor.monitor.cacher.util.CacheDataConverter;
import com.jesse.sqlmonitor.response_body.ConnectionUsage;
import com.jesse.sqlmonitor.response_body.QPSResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 缓存指标数据转换器测试用例。
 * {@literal (Map <=> <T extends ResponseBase<T>>)}
 */
@SpringBootTest
public class CacheDataConverterTest
{
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void cacheDataConverterTest()
    {
        CacheDataConverter
            .makeCacheDataFromIndicator(
                QPSResult.buildZeroQPS(), QPSResult.class, objectMapper)
            .doOnSuccess((res) -> {
                System.out.println("Type: " + res.getClass());
                System.out.println(res);
            })
            .flatMap((res) ->
                CacheDataConverter.restoreIndicatorMapToInstance(res, QPSResult.class, this.objectMapper))
            .doOnSuccess((res) -> {
                System.out.println("Type: " + res.getClass());
                System.out.println(res);
            })
            .block();

        CacheDataConverter
            .makeCacheDataFromIndicator(
                ConnectionUsage.builder().build(),
                ConnectionUsage.class, objectMapper)
            .doOnSuccess((res) -> {
                System.out.println("Type: " + res.getClass());
                System.out.println(res);
            })
            .flatMap((res) ->
                CacheDataConverter.restoreIndicatorMapToInstance(res, ConnectionUsage.class, this.objectMapper))
            .doOnSuccess((res) -> {
                System.out.println("Type: " + res.getClass());
                System.out.println(res);
            })
            .block();
    }
}
