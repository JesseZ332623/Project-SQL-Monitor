package com.jesse.indicator_receiver.response_body;

import com.jesse.indicator_receiver.response_body.base.ResponseBase;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;

/** InnoDB 缓存命中率监控数据响应。*/
@Getter
@Builder(builderClassName = "Builder")
@ToString
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Schema(
    description = "InnoDB 缓存命中率监控数据",
    requiredProperties = {
        "cacheHitRate",
        "queryDiff",
        "resetDetected",
        "usedPreviousResult",
        "error"
    }
)
public class InnodbBufferCacheHitRate extends ResponseBase<InnodbBufferCacheHitRate>
{
    @Schema(description = "InnoDB 缓存命中率具体值")
    private BigDecimal cacheHitRate;

    @Schema(description = "两次查询的时间间隔（单位：毫秒）")
    private long queryDiff;

    @Schema(description = "指标是否被外部重置？")
    private boolean resetDetected = false;

    @Schema(description = "是否使用了上一次的计算结果？")
    private boolean usedPreviousResult = false;

    @Schema(description = "在过程中是否出错？")
    private boolean error = false;
}
