package com.jesse.sqlmonitor.response_body;

import com.jesse.sqlmonitor.monitor.constants.SizeUnit;
import com.jesse.sqlmonitor.monitor.impl.network_traffic.impl.TrafficRateCalculator;
import com.jesse.sqlmonitor.monitor.impl.network_traffic.impl.TrafficStateSnapshot;
import com.jesse.sqlmonitor.response_body.base.ResponseBase;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;

/** 数据库网络流量监控数据响应。*/
@Getter
@Builder(builderClassName = "Builder")
@ToString
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Schema(
    description = "数据库网络流量监控数据",
    requiredProperties = {
        "totalBytesSent",
        "totalBytesReceive",
        "receivePerSec",
        "sentPerSec",
        "queryDiff",
        "sizeUnit",
        "resetDetected",
        "error"
    }
)
public class NetWorkTraffic extends ResponseBase<NetWorkTraffic>
{
    @Schema(description = "[服务器发送给客户端] 的全部字节数")
    private long totalBytesSent;

    @Schema(description = "[所有客户端发送给服务器] 的全部字节数")
    private long totalBytesReceive;

    @Schema(description = "服务器每秒的接收速率")
    private BigDecimal receivePerSec;

    @Schema(description = "服务器发送速率")
    private BigDecimal sentPerSec;

    @Schema(description = "两次查询的时间间隔（单位：毫秒）")
    private long queryDiff;

    @Schema(description = "本次流量统计的计量单位")
    private SizeUnit sizeUnit;

    @Schema(description = "指标是否被外部重置？")
    private boolean resetDetected = false;

    @Schema(description = "在统计网络流量的过程中出错？")
    private boolean error = false;

    /** 是否是一个有效结果？ */
    public boolean isValid()
    {
        return
        (!receivePerSec.equals(BigDecimal.ZERO) && !sentPerSec.equals(BigDecimal.ZERO))
        && (!this.resetDetected || !this.error);
    }

    /** 构建零速率结果。*/
    public static NetWorkTraffic
    buildZeroRate(@NotNull TrafficStateSnapshot currentState, SizeUnit unit)
    {
        return
        NetWorkTraffic.builder()
            .totalBytesSent(currentState.getTotalBytesSent())
            .totalBytesReceive(currentState.getTotalBytesReceive())
            .sentPerSec(BigDecimal.ZERO)
            .receivePerSec(BigDecimal.ZERO)
            .queryDiff(0L)
            .sizeUnit(unit)
            .resetDetected(false)
            .build();
    }

    /** 构建流量统计结果。*/
    public static NetWorkTraffic
    buildTrafficResult(
        @NotNull TrafficStateSnapshot currentState,
        @NotNull TrafficRateCalculator.TrafficRate rate
    )
    {
        return
        NetWorkTraffic.builder()
            .totalBytesSent(currentState.getTotalBytesSent())
            .totalBytesReceive(currentState.getTotalBytesReceive())
            .sentPerSec(rate.getSentKBytesPerSec())
            .receivePerSec(rate.getReceiveKBytesPerSec())
            .queryDiff(rate.getQueryDiff())
            .sizeUnit(rate.getSizeUnit())
            .resetDetected(rate.isResetDetected())
            .build();
    }

    /** 构造一个表述错误的实例。*/
    public static @NotNull NetWorkTraffic onError()
    {
        return
        NetWorkTraffic.builder()
            .totalBytesSent(0L)
            .totalBytesReceive(0L)
            .sentPerSec(BigDecimal.ZERO)
            .receivePerSec(BigDecimal.ZERO)
            .queryDiff(0L)
            .sizeUnit(null)
            .resetDetected(false)
            .error(true)
            .build();
    }
}