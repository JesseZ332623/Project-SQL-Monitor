package com.jesse.sqlmonitor.config;

import com.jesse.sqlmonitor.properties.R2dbcMasterProperties;
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
// import org.springframework.data.r2dbc.config.AbstractR2dbcConfiguration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.r2dbc.convert.R2dbcCustomConversions;
import org.springframework.data.r2dbc.dialect.MySqlDialect;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;
import org.springframework.r2dbc.core.DatabaseClient;

import java.time.Duration;

import static io.r2dbc.spi.ConnectionFactoryOptions.*;
import static org.springframework.data.r2dbc.dialect.DialectResolver.getDialect;

/** Spring Data R2DBC 主数据源配置类。*/
@Configuration
@EnableR2dbcRepositories
@RequiredArgsConstructor
public class R2dbcMasterConfiguration // extends AbstractR2dbcConfiguration
{
    /** 来自配置文件的 R2DBC 主数据库属性类。*/
    private final R2dbcMasterProperties masterProperties;

    /** R2DBC 连接工厂配置。*/
    @Primary
    @Bean(name = "R2dbcMasterConnectionFactory")
    public @NotNull ConnectionFactory connectionFactory()
    {
        ConnectionFactoryOptions options
            = ConnectionFactoryOptions.builder()
                .option(DRIVER, "mysql")
                .option(HOST, masterProperties.getHost())
                .option(PORT, masterProperties.getPort())
                .option(USER, masterProperties.getUser())
                .option(PASSWORD, masterProperties.getPassword())
                .option(DATABASE, masterProperties.getDefaultSchema())
                .option(SSL, false)
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

    @Primary
    @Bean("R2dbcMasterDatabaseClient")
    public @NotNull DatabaseClient
    databaseClient(
        @Qualifier("R2dbcMasterConnectionFactory")
        ConnectionFactory connectionFactory
    )
    {
        return
        DatabaseClient.builder()
            .connectionFactory(connectionFactory)
            .bindMarkers(getDialect(connectionFactory).getBindMarkersFactory())
            .build();
    }

    /** 向 R2DBC 阐明使用 MySQL 方言。*/
    @Bean
    public R2dbcCustomConversions
    customConversions(DatabaseClient client)
    {
        return R2dbcCustomConversions.of(
            MySqlDialect.INSTANCE
        );
    }
}