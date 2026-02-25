package com.jesse.email_receiver.config;

import com.jesse.email_receiver.route.EmailSenderEndpoints;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import lombok.RequiredArgsConstructor;
import org.apache.maven.model.Model;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Swagger 配置类。*/
@Configuration
@RequiredArgsConstructor
public class OpenAPIConfiguration
{
    @Qualifier(value = "pom-model")
    private final Model model;

    @Bean
    public OpenAPI openAPI()
    {
        final Info serverInfo
            = new Info().title(this.model.getName())
                .version(this.model.getVersion())
                .description(this.model.getDescription());

        return new OpenAPI().info(serverInfo);
    }

    @Bean
    public GroupedOpenApi
    groupedOpenApi()
    {
        return
        GroupedOpenApi.builder()
            .group(model.getArtifactId())
            .pathsToMatch(EmailSenderEndpoints.ROOT + "/**")
            .build();
    }
}