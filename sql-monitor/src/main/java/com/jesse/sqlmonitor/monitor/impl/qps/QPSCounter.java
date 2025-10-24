package com.jesse.sqlmonitor.monitor.impl.qps;

import com.jesse.sqlmonitor.response_body.qps.QPSResult;
import reactor.core.publisher.Mono;

/** MySQL QPS 计算器接口。*/
public interface QPSCounter
{
    Mono<QPSResult> calculateQPS();
}