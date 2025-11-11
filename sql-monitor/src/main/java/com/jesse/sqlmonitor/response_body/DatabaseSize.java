package com.jesse.sqlmonitor.response_body;

import com.jesse.sqlmonitor.response_body.base.ResponseBase;
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
    private final static
    DatabaseSize EMPTY_DATABASE_SIZE = new DatabaseSize();

    @Schema(description = "字节数")
    private long sizeBytes;

    @Schema(description = "兆字节数")
    private double sizeMBytes;

    @Builder.Default
    @Schema(description = "该数据库下所有表的大小（单位：MB）")
    private Map<String, Double> tableSizes = new LinkedHashMap<>();

    @Override
    public boolean isValid() {
        return !this.equals(EMPTY_DATABASE_SIZE);
    }
}