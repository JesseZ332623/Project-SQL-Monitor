package com.jesse.email_receiver.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.CountDownLatch;

/** 服务优雅退出相关配置。*/
@Configuration
public class GracefulShutdownConfig
{
    /**
     * 服务被终止时，
     * 需要先阻塞等待手头正在执行的任务完成，再放行继续执行退出流程。
     */
    @Bean(name = "shutdown-countdown-latch")
    public CountDownLatch countDownLatch() {
        return new CountDownLatch(1);
    }
}