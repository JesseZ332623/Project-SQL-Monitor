package com.jesse.sqlmonitor.response_body.qps_statistics;

import com.jesse.sqlmonitor.response_body.base.ResponseBase;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

/** 指定条件下的 QPS 标准差和负载均衡率响应。*/
@Getter
@Builder
@ToString
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Schema(
    description = "指定条件下的 QPS 标准差和负载均衡率",
    requiredProperties = {
        "stddev",
        "loadStability",
        "dataPoints"
    }
)
public class StandingDeviationQPS extends ResponseBase<StandingDeviationQPS>
{
    private final static
    StandingDeviationQPS EMPTY_STDDEV_QPS = new StandingDeviationQPS();

    @Schema(description = "QPS 标准差", example = "66.63315972948297")
    private double stddev;

    @Schema(description = "QPS 负载均衡率", examples = "3.655882882369868163")
    private double loadStability;

    @Schema(description = "数据点采样数", example = "2722")
    private long dataPoints;

    /**
     * 本指标响应数据是否有效？（所有子类必须实现）
     */
    @Override
    public boolean isValid() {
        return !this.equals(EMPTY_STDDEV_QPS);
    }
}