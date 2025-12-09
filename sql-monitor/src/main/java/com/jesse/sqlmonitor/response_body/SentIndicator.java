package com.jesse.sqlmonitor.response_body;

import com.jesse.sqlmonitor.response_body.base.ResponseBase;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 携带数据库地址 + 时间戳 + 指标数据的响应
 *（通过 {@link reactor.rabbitmq.Sender}）发送出去。
 */
@Getter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor(access  = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PUBLIC)
public class SentIndicator<T extends ResponseBase<T>>
    extends ResponseBase<T>
{
    private LocalDateTime localDateTime;
    private String        address;
    private T             indicator;

    /** 本指标响应数据是否有效？（所有子类必须实现）*/
    @Override
    public boolean isValid() {
        return indicator.isValid();
    }
}