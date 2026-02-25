package com.jesse.email_receiver.config;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.InputStreamReader;

/** maven-model POM.xml 文件解析1配置。*/
@Configuration
public class PomPropertiesConfig
{
    @Bean(name = "pom-model")
    public Model pomModel()
    {
        try
        {
            final ClassPathResource resource = new ClassPathResource("pom.xml");

            return new
            MavenXpp3Reader().read(new InputStreamReader(resource.getInputStream()));
        }
        catch (Exception exception) {
            throw new RuntimeException("Failed to read pom.xml", exception);
        }
    }
}