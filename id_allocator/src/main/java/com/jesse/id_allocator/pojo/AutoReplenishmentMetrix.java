package com.jesse.id_allocator.pojo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.*;
import org.jetbrains.annotations.NotNull;

/** ID 自动补货操作信息矩阵。（用于监控）*/
@Data
@ToString
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class AutoReplenishmentMetrix
{
    /** 补充的 ID 数量。*/
    private int added;

    /** 补充前的列表长度。*/
    private int beforeLen;

    /** 补充后的列表长度。*/
    private int afterLen;

    /** 目标列表长度。*/
    private int target;

    public static AutoReplenishmentMetrix
    fromJson(@NotNull ObjectMapper mapper, String JSON) throws JsonProcessingException {
        return mapper.readValue(JSON, AutoReplenishmentMetrix.class);
    }
}