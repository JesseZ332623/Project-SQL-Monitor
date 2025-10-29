package com.jesse.sqlmonitor.response_body.qps_statistics;

import com.jesse.sqlmonitor.response_body.base.ResponseBase;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

/** 指定条件下的 QPS 极值。*/
@Getter
@Builder
@ToString
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Schema(
    description = "指定条件下的 QPS 极值",
    requiredProperties = {"max", "min"}
)
public class ExtremeQPS extends ResponseBase<ExtremeQPS>
{
    @Schema(description = "最大 QPS", examples = "971.32377459")
    double max;

    @Schema(description = "最小 QPS", examples = "0.0000000")
    double min;
}