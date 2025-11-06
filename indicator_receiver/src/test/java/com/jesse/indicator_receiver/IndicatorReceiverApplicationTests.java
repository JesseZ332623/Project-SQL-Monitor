package com.jesse.indicator_receiver;

import com.jesse.indicator_receiver.utils.SystemIPv4Getter;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.net.InetAddress;
import java.net.UnknownHostException;

/** 指标接收器服务测试类。*/
@Slf4j
@SpringBootTest
class IndicatorReceiverApplicationTests
{
    @Test
    public void getLocalIPByIPConfig() throws UnknownHostException {
//        System.out.println(
//            SystemIPv4Getter.getLocalIPByIPConfig()
//        );

        System.out.println(InetAddress.getLocalHost().getHostAddress());
    }
}
