package com.jesse.indicator_receiver.uint_test;

import cn.hutool.core.util.RandomUtil;
import com.jesse.indicator_receiver.utils.IPv4Converter;
import com.jesse.indicator_receiver.utils.LocalIPGetter;
import com.jesse.indicator_receiver.utils.exception.InvalidIPv4Exception;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/** 工具类方法综合单元测试。*/
@Slf4j
@ExtendWith(MockitoExtension.class)
public class IndicatorReceiverUtilsUnitTest
{
    @Test
    public void getLocalIPTest() {
        log.info("Local IPv4: {}", LocalIPGetter.getLocalIP());
    }

    @Test
    public void ipToLongTest()
    {
        log.info("Localhost value: {}",    IPv4Converter.ipToLong("localhost"));
        log.info("198.165.1.12 value: {}", IPv4Converter.ipToLong("198.165.1.12"));

        try {
            IPv4Converter.ipToLong("666.666.666.666");
        }
        catch (InvalidIPv4Exception exception) {
            log.error("{}", exception.getMessage());
        }
    }

    @Test
    public void longToIpTest()
    {
        long begin = 3332702476L;

        for (int index = 0; index < 10; ++index)
        {
            log.info(
                "UINT32: {} = Ipv4: {}",
                begin, IPv4Converter.longToIp(begin)
            );

            begin += RandomUtil.randomLong(1000L, 2000L);
        }
    }
}
