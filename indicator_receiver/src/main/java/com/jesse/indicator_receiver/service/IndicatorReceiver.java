package com.jesse.indicator_receiver.service;

import com.jesse.indicator_receiver.service.impl.ReceiverLifecycleManager;
import reactor.core.publisher.Mono;

import java.util.concurrent.CountDownLatch;

/** 指标数据接收器接口。*/
public interface IndicatorReceiver
{
    /** 设置是否正在运行的原子标志位。*/
    void setRunningFlag(boolean flag);

    /**
     * 获取指标接收器的 CountDownLatch 实例，
     * 供 {@link ReceiverLifecycleManager#stop()} 方法中尝试使用它
     * 来等待所有批量插入操作完成。
     */
    CountDownLatch getCountDownLatch();

    Mono<Void> receiveIndicator();
}
