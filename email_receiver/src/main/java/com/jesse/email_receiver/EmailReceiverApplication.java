package com.jesse.email_receiver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** 邮件消费者服务启动类。*/
@SpringBootApplication
public class EmailReceiverApplication
{
    public static void main(String[] args) {
        SpringApplication.run(EmailReceiverApplication.class, args);
    }
}