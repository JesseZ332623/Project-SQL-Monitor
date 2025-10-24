package com.jesse.indicator_receiver.service;

import reactor.core.publisher.Mono;

/** 指标数据接收器接口。*/
public interface IndicatorReceive
{
    /** 设置是否正在运行的原子标志位。*/
    void setRunningFlag(boolean flag);

    Mono<Void> receiveIndicator();
}
