package com.jesse.indicator_receiver.config;

import com.jesse.indicator_receiver.properties.R2dbcSlaverProperties;
import io.r2dbc.pool.ConnectionPool;
import io.r2dbc.pool.ConnectionPoolConfiguration;
import io.r2dbc.spi.*;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.transaction.ReactiveTransactionManager;
import org.springframework.transaction.reactive.TransactionalOperator;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/** R2DBC 从数据库配置类。*/
@Configuration
@RequiredArgsConstructor
public class R2dbcSlaverConfiguration
{
    /** 来自配置文件的 R2DBC 从数据库属性类。*/
    private final R2dbcSlaverProperties slaverProperties;

    @Bean(name = "R2dbcSlaverConnectionFactory")
    public @NotNull ConnectionFactory connectionFactory()
    {
        final String coonectionURL
            = String.format(
                "r2dbc:mysql://%s:%s@%s:%d/%s?serverTimezone=Asia/Shanghai" +
                "&allowPublicKeyRetrieval=true" +
                "&useUnicode=true"              +
                "&characterEncoding=UTF8"       +
                "&sslMode=preferred",
                slaverProperties.getUser(),
                URLEncoder.encode(slaverProperties.getPassword(), StandardCharsets.UTF_8),
                slaverProperties.getHost(),
                slaverProperties.getPort(),
                slaverProperties.getDefaultSchema()
        );

        ConnectionFactory connectionFactory = ConnectionFactories.get(coonectionURL);

        // 配置连接池
        ConnectionPoolConfiguration poolConfiguration
            = ConnectionPoolConfiguration.builder()
            .connectionFactory(connectionFactory)
            .validationQuery("SELECT 1")             // 连接验证查询语句
            .validationDepth(ValidationDepth.REMOTE) // 连接验证深度（远程）
            .initialSize(0)                          // 初始连接池大小
            .maxSize(15)                             // 最大连接池大小
            .backgroundEvictionInterval(Duration.ofMinutes(1L)) // 定期验证限制连接间隔
            .maxIdleTime(Duration.ofMinutes(30))                // 连接最大闲置时间
            .maxLifeTime(Duration.ofHours(1L))                  // 连接最大存活时间
            .maxAcquireTime(Duration.ofSeconds(30L))            // 获取连接期限时间
            .acquireRetry(3)                      // 获取连接失败最多重试次数
            .maxCreateConnectionTime(Duration.ofSeconds(10L))   // 建立单个连接期限时间
            .build();

        return new ConnectionPool(poolConfiguration);
    }

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

    /** R2DBC 的事务操作器。*/
    @Bean("R2dbcSlaverTransactionalOperator")
    public TransactionalOperator
    transactionalOperator(ReactiveTransactionManager transactionManager)
    {
        return
        TransactionalOperator.create(transactionManager);
    }
}