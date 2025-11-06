package com.jesse.indicator_receiver;

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
    /** 获取本机 IP 地址。*/
    @Test
    public void getLocalIPByIPConfig()
    {
        try
        {
            System.out.println(
                InetAddress.getLocalHost().getHostAddress()
            );
        }
        catch (UnknownHostException e) {
            log.error("{}", e.getMessage());
        }
    }
}
