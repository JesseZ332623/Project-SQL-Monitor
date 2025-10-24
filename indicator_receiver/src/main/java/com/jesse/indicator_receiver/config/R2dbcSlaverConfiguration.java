package com.jesse.indicator_receiver.config;

import com.jesse.indicator_receiver.properties.R2dbcSlaverProperties;
import io.r2dbc.pool.ConnectionPool;
import io.r2dbc.pool.ConnectionPoolConfiguration;
import io.r2dbc.spi.ConnectionFactories;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.ConnectionFactoryOptions;
import io.r2dbc.spi.Option;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.transaction.ReactiveTransactionManager;
import org.springframework.transaction.reactive.TransactionalOperator;

import java.time.Duration;

import static io.r2dbc.spi.ConnectionFactoryOptions.*;
import static io.r2dbc.spi.ConnectionFactoryOptions.DATABASE;
import static io.r2dbc.spi.ConnectionFactoryOptions.PASSWORD;
import static io.r2dbc.spi.ConnectionFactoryOptions.SSL;
import static io.r2dbc.spi.ConnectionFactoryOptions.USER;
import static org.springframework.data.r2dbc.dialect.DialectResolver.getDialect;

/** R2DBC 从数据库配置类。*/
@Configuration
@EnableR2dbcRepositories
@RequiredArgsConstructor
public class R2dbcSlaverConfiguration
{
    /** 来自配置文件的 R2DBC 从数据库属性类。*/
    private final R2dbcSlaverProperties slaverProperties;

    @Bean(name = "R2dbcSlaverConnectionFactory")
    public @NotNull ConnectionFactory connectionFactory()
    {
        ConnectionFactoryOptions options
            = ConnectionFactoryOptions.builder()
            .option(DRIVER, "mysql")
            .option(HOST, slaverProperties.getHost())
            .option(PORT, slaverProperties.getPort())
            .option(USER, slaverProperties.getUser())
            .option(PASSWORD, slaverProperties.getPassword())
            .option(DATABASE, slaverProperties.getDefaultSchema())
            .option(SSL, true)
            .option(Option.valueOf("useUnicode"), "true")
            .option(Option.valueOf("characterEncoding"), "utf8")
            .option(Option.valueOf("zeroDateTimeBehavior"), "convertToNull")
            .option(Option.valueOf("serverTimezone"), "Asia/Shanghai")
            .option(Option.valueOf("autoReconnect"), "true")
            .option(Option.valueOf("rewriteBatchedStatements"), "true")
            .option(Option.valueOf("allowPublicKeyRetrieval"), "true")
            .build();

        ConnectionFactory connectionFactory = ConnectionFactories.get(options);

        // 配置连接池
        ConnectionPoolConfiguration poolConfiguration
            = ConnectionPoolConfiguration.builder()
            .connectionFactory(connectionFactory)
            .validationQuery("SELECT 1")
            .initialSize(35)
            .maxSize(65)
            .maxIdleTime(Duration.ofSeconds(30))
            .build();

        return new ConnectionPool(poolConfiguration);
    }

    @Bean("R2dbcSlaverDatabaseClient")
    public @NotNull DatabaseClient
    databaseClient(
        @Qualifier("R2dbcSlaverConnectionFactory")
        ConnectionFactory connectionFactory
    )
    {
        return
        DatabaseClient.builder()
            .connectionFactory(connectionFactory)
            .bindMarkers(getDialect(connectionFactory).getBindMarkersFactory())
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