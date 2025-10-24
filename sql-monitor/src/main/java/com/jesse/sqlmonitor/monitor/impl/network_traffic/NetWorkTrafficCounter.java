package com.jesse.sqlmonitor.monitor.impl.network_traffic;

import com.jesse.sqlmonitor.monitor.constants.SizeUnit;
import com.jesse.sqlmonitor.response_body.NetWorkTraffic;
import reactor.core.publisher.Mono;

/** MySQL 服务器网络流量计算器接口。*/
public interface NetWorkTrafficCounter
{
    /** 计算此刻数据库的网络流量（支持不同的计量单位）*/
    Mono<NetWorkTraffic>
    calculateNetWorkTraffic(SizeUnit unit);
}