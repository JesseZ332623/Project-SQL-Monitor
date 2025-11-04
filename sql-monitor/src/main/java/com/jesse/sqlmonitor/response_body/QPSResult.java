package com.jesse.sqlmonitor.response_body;

import com.jesse.sqlmonitor.monitor.impl.qps.impl.QPSCounterImpl;
import com.jesse.sqlmonitor.response_body.base.ResponseBase;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import org.jetbrains.annotations.NotNull;

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

    /**
     * 检查本结果是否是值得记录的有效结果。
     * 只有 QPS 不是 0 且不是错误或者重置结果的 QPSResult 才是有效的。
     */
    public boolean isValid() {
        return (!this.qps.equals(BigDecimal.ZERO) && (!this.resetDetected || !this.error));
    }

    public static @NotNull
    QPSResult buildZeroQPS()
    {
        return
        QPSResult.builder()
            .qps(BigDecimal.ZERO)
            .currentQueries(0L)
            .queryDiff(0L)
            .timeDiffMs(0L)
            .resetDetected(false)
            .error(false)
            .build();
    }

    public static @NotNull
    QPSResult buildQPSResult(@NotNull QPSCounterImpl.QPS qps)
    {
        return
        QPSResult.builder()
            .qps(qps.getQps())
            .currentQueries(qps.getCurrentQuires())
            .queryDiff(qps.getQueryDiff())
            .timeDiffMs(qps.getTimeDiffMs())
            .resetDetected(qps.isResetDetected())
            .error(false)
            .build();
    }

    /** 构造一个表述错误 QPS 结果的实例。*/
    public static @NotNull QPSResult onError()
    {
        return
        QPSResult.builder()
            .qps(BigDecimal.ZERO)
            .currentQueries(0L)
            .queryDiff(0L)
            .timeDiffMs(0L)
            .resetDetected(false)
            .error(true)
            .build();
    }
}