package com.jesse.sqlmonitor.config;

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
            new Info().title("MySQL 数据库指标监视器")
                .version("1.0.0")
                .description("实时监控 MySQL 的各项指标。")
        );
    }

    @Bean
    public GroupedOpenApi sqlmonitorPublicAPI()
    {
        return
        GroupedOpenApi.builder()
            .group("sqlmonitor")
            .pathsToMatch("/api/sql-monitor/**")
            .build();
    }

    @Bean
    public GroupedOpenApi sqlIndicatorPublicAPI()
    {
        return
        GroupedOpenApi.builder()
            .group("indicator-record")
            .pathsToMatch("/api/indicator/**")
            .build();
    }
}