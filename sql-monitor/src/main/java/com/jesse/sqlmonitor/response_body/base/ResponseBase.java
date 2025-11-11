package com.jesse.sqlmonitor.response_body.base;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.jesse.sqlmonitor.response_body.*;
import com.jesse.sqlmonitor.response_body.qps_statistics.ExtremeQPS;
import com.jesse.sqlmonitor.response_body.QPSResult;
import com.jesse.sqlmonitor.response_body.qps_statistics.StandingDeviationQPS;
import lombok.*;

/** 所有响应体的基类。（在 Jackson 序列化/反序列化时有大用，作为类型令牌传入）*/
@EqualsAndHashCode
@RequiredArgsConstructor(access = AccessLevel.PUBLIC)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = ConnectionUsage.class, name = "connectionUsage"),
    @JsonSubTypes.Type(value = DatabaseSize.class,    name = "databaseSize"),
    @JsonSubTypes.Type(value = InnodbBufferCacheHitRate.class, name = "innodbBufferCacheHitRate"),
    @JsonSubTypes.Type(value = NetWorkTraffic.class, name = "networkTraffic"),
    @JsonSubTypes.Type(value = QPSResult.class,      name = "qps"),
    @JsonSubTypes.Type(value = ExtremeQPS.class,     name = "extreme-qps"),
    @JsonSubTypes.Type(value = StandingDeviationQPS.class, name = "stddev-qps")
})
public abstract class ResponseBase<T extends ResponseBase<T>>
{
    /** 本指标响应数据是否有效？（所有子类必须实现）*/
    public abstract boolean isValid();
}