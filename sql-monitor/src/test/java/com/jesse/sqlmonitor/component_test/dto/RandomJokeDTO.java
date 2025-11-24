package com.jesse.sqlmonitor.component_test.dto;

import lombok.*;

/** 表示一个随机笑话响应体的 DTO。*/
@Data
@ToString
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class RandomJokeDTO
{
    private String type;
    private String setup;
    private String punchline;
    private Long   id;
}
