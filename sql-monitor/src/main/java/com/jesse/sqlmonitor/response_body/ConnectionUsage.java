package com.jesse.sqlmonitor.response_body;

import com.jesse.sqlmonitor.response_body.base.ResponseBase;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

/** 数据库连接使用率监控数据响应。*/
@Getter
@Builder
@ToString
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Schema(
    description = "数据库连接使用率监控数据",
    requiredProperties = {
        "maxConnections",
        "currentConnections",
        "connectUsagePercent"
    }
)
public class ConnectionUsage extends ResponseBase<ConnectionUsage>
{
    /** 一个空的连接使用率对象，一般用于哈希值的比较。*/
    private static final
    ConnectionUsage EMPTY_USAGE = new ConnectionUsage();

    @Schema(
        description  = "数据库允许的最大连接数",
        example      = "8000"
    )
    private int maxConnections;

    @Schema(
        description  = "数据库当前连接数",
        example      = "30"
    )
    private int currentConnections;

    @Schema(
        description  = "数据库当前连接使用率",
        example      = "0.3750"
    )
    private double connectUsagePercent;

    /**
     * 本指标响应数据是否有效？（所有子类必须实现）
     */
    @Override
    public boolean isValid() {
        return !this.equals(EMPTY_USAGE);
    }
}
