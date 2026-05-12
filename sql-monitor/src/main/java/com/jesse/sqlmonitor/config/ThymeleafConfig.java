package com.jesse.sqlmonitor.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.thymeleaf.spring6.SpringWebFluxTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.net.URL;

/** Spring 模板引擎配置，用于 Thymeleaf Template 渲染。*/
@Slf4j
@Configuration
public class ThymeleafConfig
{
    /** 邮件模板的存放路径。*/
    @Value("${app.thymeleaf.prefix}")
    private String prefix;

    @Bean
    public SpringWebFluxTemplateEngine templateEngine()
    {
        final SpringWebFluxTemplateEngine templateEngine
            = new SpringWebFluxTemplateEngine();

        final ClassLoaderTemplateResolver
            resolver = new ClassLoaderTemplateResolver();

        log.info("{}", this.prefix);

        resolver.setPrefix(prefix);
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding("UTF-8");
        resolver.setCacheable(true); // 生产环境开启缓存

        // 打印实际路径
        final URL resource
            = getClass().getClassLoader().getResource("templates/mail/database-indicator-report.html");

        log.info(
            "Template file exists: {}",
            resource != null ? resource.getPath() : "NOT FOUND"
        );

        templateEngine.setTemplateResolver(resolver);

        return templateEngine;
    }
}
