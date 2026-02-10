package com.jesse.sqlmonitor.config;

import com.jesse.sqlmonitor.properties.R2dbcSlaverProperties;
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

/** R2DBC 从数据库配置类。（存储指标数据的数据库）*/
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

        final ConnectionFactory connectionFactory = ConnectionFactories.get(coonectionURL);

        // 配置连接池
        final ConnectionPoolConfiguration poolConfiguration
            = ConnectionPoolConfiguration.builder()
                .name("sql-monitor-r2dbc-slaver-pool")   // 连接池名
                .connectionFactory(connectionFactory)
                .validationQuery("SELECT 1")             // 连接验证查询语句
                .validationDepth(ValidationDepth.REMOTE) // 连接验证深度（远程）
                .initialSize(5)                          // 初始连接池大小
                .minIdle(2)                              // 最低闲置连接数 (r2dbc-pool 0.9+)
                .maxSize(15)                             // 最大连接池大小
                .backgroundEvictionInterval(Duration.ofSeconds(30L)) // 失效连接检查间隔
                .maxIdleTime(Duration.ofMinutes(5L))                 // 连接最大闲置时间
                .maxLifeTime(Duration.ofMinutes(30L))                // 连接最大存活时间
                .maxAcquireTime(Duration.ofSeconds(10L))             // 获取连接期限时间
                .acquireRetry(5)                       // 获取连接失败最多重试次数
                .maxCreateConnectionTime(Duration.ofSeconds(10L))   // 建立单个连接期限时间
                .registerJmx(true)                                  // 将本连接池注册到 JMX，方便观察调试
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