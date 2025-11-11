package com.jesse.sqlmonitor.indicator_record.service;

import com.jesse.sqlmonitor.response_body.SentIndicator;
import com.jesse.sqlmonitor.response_body.base.ResponseBase;
import reactor.core.publisher.Mono;

/** 指标数据发送器接口。*/
public interface IndicatorSender
{
    <T extends ResponseBase<T>>
    Mono<Void> sendIndicator(SentIndicator<T> indicator);
}