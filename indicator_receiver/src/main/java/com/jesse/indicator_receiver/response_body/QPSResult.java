package com.jesse.indicator_receiver.response_body;

import com.jesse.indicator_receiver.response_body.base.ResponseBase;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;

/** 数据库 QPS 监控数据响应。*/
@Getter
@Builder
@ToString
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Schema(
    description = "数据库 QPS 监控数据",
    requiredProperties = {
        "qps",
        "currentQueries",
        "queryDiff",
        "timeDiffMs",
        "resetDetected",
        "error"
    }
)
public class QPSResult extends ResponseBase<QPSResult>
{
    @Schema(description = "QPS 具体值")
    private BigDecimal qps;

    @Schema(description = "当前总查询数")
    private long currentQueries;

    @Schema(description = "与上一次获取的总查询数的差值")
    private Long queryDiff;

    @Schema(description = "与上一次获取的总查询数的时间间隔（单位：毫秒）")
    private Long timeDiffMs;

    @Schema(description = "总查询数是否被外部重置？")
    @Builder.Default
    private boolean resetDetected = false;

    @Schema(description = "在统计 QPS 的过程中出错？")
    @Builder.Default
    private boolean error = false;
}