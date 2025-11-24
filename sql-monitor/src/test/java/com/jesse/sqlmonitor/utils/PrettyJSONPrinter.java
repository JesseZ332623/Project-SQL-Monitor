package com.jesse.sqlmonitor.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

/** JSON 格式美化器。*/
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final public class PrettyJSONPrinter
{
    private final static
    ObjectMapper mapper
        = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    /** 获取美化格式后的 JSON 字符串。*/
    public static String
    getPrettyFormatJSON(@NotNull String json)
    {
        try
        {
            return
            mapper.writeValueAsString(mapper.readTree(json));
        }
        catch (JsonProcessingException exception)
        {
            log.error("{}", exception.getMessage());
            return json;
        }
    }
}