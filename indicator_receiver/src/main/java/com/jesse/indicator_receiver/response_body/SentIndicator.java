package com.jesse.indicator_receiver.response_body;

import com.jesse.indicator_receiver.response_body.base.ResponseBase;
import lombok.*;

import java.time.LocalDateTime;

/** 携带数据库地址 + 时间戳 + 指标数据的响应
 *（通过 {@link reactor.rabbitmq.Sender}）发送出去。
 */
@Getter
@ToString
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PUBLIC)
public class SentIndicator<T extends ResponseBase<T>>
    extends ResponseBase<T>
{
    private String        messageId;
    private LocalDateTime localDateTime;
    private String        address;
    private T             indicator;
}