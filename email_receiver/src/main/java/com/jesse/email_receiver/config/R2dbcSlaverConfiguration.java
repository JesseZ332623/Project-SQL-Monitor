package com.jesse.email_receiver.config;

import com.jesse.email_receiver.properties.R2dbcSlaverProperties;
import io.r2dbc.spi.ConnectionFactories;
import io.r2dbc.spi.ConnectionFactory;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.r2dbc.core.DatabaseClient;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/** R2DBC 数据库配置类。*/
@Configuration
@RequiredArgsConstructor
public class R2dbcSlaverConfiguration
{
    /** 来自配置文件的 R2DBC 从数据库属性类。*/
    private final R2dbcSlaverProperties slaverProperties;

    /** 连接工厂配置。*/
    @Bean(name = "R2dbcSlaverConnectionFactory")
    public @NotNull ConnectionFactory connectionFactory()
    {
        final String coonectionURL
            = String.format(
                "r2dbc:mysql://%s:%s@%s:%d/%s?serverTimezone=Asia/Shanghai" +
                "&allowPublicKeyRetrieval=true" +
                "&useUnicode=true"              +
                "&characterEncoding=UTF8"       +
                "&sslMode=preferred"            +
                "&connectTimeout=PT10S"         +
                "&socketTimeout=PT30S"          +
                "&tcpKeepAlive=true",
                slaverProperties.getUser(),
                URLEncoder.encode(slaverProperties.getPassword(), StandardCharsets.UTF_8),
                slaverProperties.getHost(),
                slaverProperties.getPort(),
                slaverProperties.getDefaultSchema()
        );

        /*
         * 只是简单的从库中查询 email_auth_code，
         * 因此不需要煞费苦心的配置连接池。
         */

        return ConnectionFactories.get(coonectionURL);
    }

    /** 数据库客户端配置。*/
    @Bean("R2dbcSlaverDatabaseClient")
    public @NotNull DatabaseClient
    databaseClient(
        @Autowired
        @Qualifier("R2dbcSlaverConnectionFactory")
        final ConnectionFactory connectionFactory
    )
    {
        return
        DatabaseClient.builder()
            .connectionFactory(connectionFactory)
            .build();
    }
}