package com.jesse.indicator_receiver.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Swagger 配置类。*/
@Configuration
public class OpenAPIConfiguration
{
    @Bean
    public OpenAPI openAPI()
    {
        return new
        OpenAPI().info(
            new Info().title("基于 RabbitMQ 的数据库指标数据消费者实例")
                .version("1.0.0")
                .description("消费数据库指标数据存入数据库以便后续的数据分析")
        );
    }

    @Bean
    public GroupedOpenApi
    indicatorConsumerPublicAPI()
    {
        return
        GroupedOpenApi.builder()
            .group("indicator-consumer")
            .pathsToMatch("/api/indicator_receiver/**")
            .build();
    }
}