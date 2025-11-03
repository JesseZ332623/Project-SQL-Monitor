package com.jesse.indicator_receiver.response_body;

import com.jesse.indicator_receiver.contants.SizeUnit;
import com.jesse.indicator_receiver.response_body.base.ResponseBase;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

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

    @lombok.Builder.Default
    @Schema(description = "指标是否被外部重置？")
    private boolean resetDetected = false;

    @lombok.Builder.Default
    @Schema(description = "在统计网络流量的过程中出错？")
    private boolean error = false;
}