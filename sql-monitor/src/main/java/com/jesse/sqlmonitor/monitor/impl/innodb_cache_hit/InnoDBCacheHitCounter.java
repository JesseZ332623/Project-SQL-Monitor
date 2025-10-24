package com.jesse.sqlmonitor.monitor.impl.innodb_cache_hit;

import com.jesse.sqlmonitor.response_body.InnodbBufferCacheHitRate;
import reactor.core.publisher.Mono;

/** InnDB 缓存命中率计算器接口。*/
public interface InnoDBCacheHitCounter
{
    /** 计算缓存命中率。*/
    Mono<InnodbBufferCacheHitRate>
    calculateBufferCacheHitRate();
}