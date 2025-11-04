package com.jesse.sqlmonitor.response_body;

import com.jesse.sqlmonitor.monitor.impl.innodb_cache_hit.impl.InnoDBCacheHitCounterImpl;
import com.jesse.sqlmonitor.response_body.base.ResponseBase;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import org.jetbrains.annotations.NotNull;

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

    @lombok.Builder.Default
    @Schema(description = "指标是否被外部重置？")
    private boolean resetDetected = false;

    @lombok.Builder.Default
    @Schema(description = "是否使用了上一次的计算结果？")
    private boolean usedPreviousResult = false;

    @lombok.Builder.Default
    @Schema(description = "在过程中是否出错？")
    private boolean error = false;

    public boolean isValid() {
        return
        cacheHitRate.equals(BigDecimal.ZERO) && (!resetDetected || !error);
    }

    /** 构建零命中率结果。*/
    public static InnodbBufferCacheHitRate
    buildZeroRate()
    {
        return
        InnodbBufferCacheHitRate.builder()
            .cacheHitRate(BigDecimal.ZERO)
            .queryDiff(0L)
            .usedPreviousResult(false)
            .resetDetected(false)
            .error(false)
            .build();
    }

    /** 构建一个正常的结果。*/
    public static InnodbBufferCacheHitRate
    buildResult(
        @NotNull
        InnoDBCacheHitCounterImpl.CacheHitRate cacheHitRate
    )
    {
        return
        InnodbBufferCacheHitRate.builder()
            .cacheHitRate(cacheHitRate.getCacheHitRate())
            .queryDiff(cacheHitRate.getQueryDiff())
            .usedPreviousResult(cacheHitRate.isUsedPreviousResult())
            .resetDetected(cacheHitRate.isResetDetected())
            .error(false)
            .build();
    }

    /** 构造一个表述错误的实例。*/
    public static InnodbBufferCacheHitRate
    onError()
    {
        return
        InnodbBufferCacheHitRate.builder()
            .cacheHitRate(BigDecimal.ZERO)
            .queryDiff(0L)
            .usedPreviousResult(false)
            .resetDetected(false)
            .error(true)
            .build();
    }
}
