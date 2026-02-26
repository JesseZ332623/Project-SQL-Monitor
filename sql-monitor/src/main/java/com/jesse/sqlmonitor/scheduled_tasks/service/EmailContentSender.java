package com.jesse.sqlmonitor.scheduled_tasks.service;

import io.github.jessez332623.reactive_email_sender.dto.EmailContent;
import org.jetbrains.annotations.NotNull;
import reactor.core.publisher.Mono;

/** 邮件内容 {@link EmailContent} 发送器接口。*/
public interface EmailContentSender
{
    /** 将构造好的邮件内容发往消息队列。*/
    Mono<Void> sendEmailContent(@NotNull EmailContent content);
}
