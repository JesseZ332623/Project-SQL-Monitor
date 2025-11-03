package com.jesse.indicator_receiver.response_body;

import com.jesse.indicator_receiver.response_body.base.ResponseBase;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.LinkedHashMap;
import java.util.Map;

/** 数据库大小数据响应。*/
@Getter
@Builder
@ToString
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Schema(
    description = "数据库大小数据",
    requiredProperties = {
        "sizeBytes", "sizeMBytes"
    }
)
public class DatabaseSize extends ResponseBase<DatabaseSize>
{
    @Schema(description = "字节数")
    private long sizeBytes;

    @Schema(description = "兆字节数")
    private double sizeMBytes;

    @lombok.Builder.Default
    @Schema(description = "该数据库下所有表的大小（单位：MB）")
    private Map<String, Double> tableSizes = new LinkedHashMap<>();
}