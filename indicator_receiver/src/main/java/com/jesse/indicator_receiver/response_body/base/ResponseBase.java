package com.jesse.indicator_receiver.response_body.base;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.jesse.indicator_receiver.response_body.*;
import lombok.*;

/** 所有响应体的基类，仅作标记。（在 Jackson 序列化/反序列化时作为类型令牌传入）*/
@Getter
@ToString
@EqualsAndHashCode
@RequiredArgsConstructor(access = AccessLevel.PUBLIC)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = ConnectionUsage.class, name = "connectionUsage"),
    @JsonSubTypes.Type(value = DatabaseSize.class,    name = "databaseSize"),
    @JsonSubTypes.Type(value = InnodbBufferCacheHitRate.class, name = "innodbBufferCacheHitRate"),
    @JsonSubTypes.Type(value = NetWorkTraffic.class, name = "networkTraffic"),
    @JsonSubTypes.Type(value = QPSResult.class,      name = "qps")
})
public abstract class ResponseBase<T extends ResponseBase<T>> {}