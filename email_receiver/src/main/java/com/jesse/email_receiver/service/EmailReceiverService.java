package com.jesse.email_receiver.service;

import reactor.core.publisher.Mono;

/** 异步邮件接收发送器接口。*/
public interface EmailReceiverService
{
    /** 设置是否正在运行的原子标志位。*/
    void setRunningFlag(boolean flag);

    /** 从队列中消费邮件消息并发送。*/
    Mono<Void> receiveEmail();
}