package com.jesse.sqlmonitor.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jesse.sqlmonitor.properties.RedisProperties;
import io.lettuce.core.ClientOptions;
import io.lettuce.core.SocketOptions;
import io.lettuce.core.TimeoutOptions;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

/** Redis 配置类。*/
@Configuration
@RequiredArgsConstructor
public class RedisConfig
{
    private final RedisProperties redisProperties;

    /** Redis 响应式连接工厂配置类。*/
    @Bean
    @Primary
    public ReactiveRedisConnectionFactory
    reactiveRedisConnectionFactory()
    {
        // 1. 创建独立 Redis 配置
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(this.redisProperties.getHost());       // Redis 地址
        config.setUsername(this.redisProperties.getUsername());   // Redis 用户名
        config.setPort(this.redisProperties.getPort());           // Redis 端口

        // 密码
        config.setPassword(
            RedisPassword.of(
                this.redisProperties.getPassword()
            )
        );

        // 2. 创建客户端配置
        LettuceClientConfiguration clientConfig
            = LettuceClientConfiguration.builder()
            .clientOptions(
                ClientOptions.builder()
                    .autoReconnect(true)
                    .suspendReconnectOnProtocolFailure(false)
                    .socketOptions(
                        SocketOptions.builder()
                            .connectTimeout(Duration.ofSeconds(5L)) // 连接超时
                            .keepAlive(true) // 自动管理 TCP 连接存活
                            .build()
                    )
                    .timeoutOptions(
                        TimeoutOptions.builder()
                            .fixedTimeout(Duration.ofSeconds(15L)) // 操作超时
                            .build()
                    ).build()
            )
            .commandTimeout(Duration.ofSeconds(15L))  // 命令超时时间
            .shutdownTimeout(Duration.ofSeconds(5L))  // 关闭超时时间
            .build();

        // 3. 创建连接工厂
        return new LettuceConnectionFactory(config, clientConfig);
    }

    /**
     * Redis 响应式模板的构建。
     *
     * @param factory Redis 连接工厂，
     *                Spring 会自动读取配置文件中的属性去构建。
     *
     * @return 配置好的 Redis 响应式模板
     */
    @Bean
    public ReactiveRedisTemplate<String, Object>
    reactiveRedisTemplate(
        ReactiveRedisConnectionFactory factory,
        ObjectMapper objectMapper
    )
    {
        /* Redis 键使用字符串进行序列化。 */
        RedisSerializer<String> keySerializer
            = new StringRedisSerializer();

        /* Redis 值使用 Jackson 进行序列化。 */
        RedisSerializer<Object> valueSerializer
            = new GenericJackson2JsonRedisSerializer(objectMapper);

        /* Redis Hash Key / Value 的序列化。 */
        RedisSerializationContext.RedisSerializationContextBuilder<String, Object>
            builder = RedisSerializationContext.newSerializationContext(keySerializer);

        /* 创建 Redis 序列化上下文，设置序列化方式。 */
        RedisSerializationContext<String, Object> context
            = builder.value(valueSerializer)
            .hashKey(keySerializer)
            .hashValue(valueSerializer)
            .build();

        /* 根据上述配置构建 ReactiveRedisTemplate。 */
        return new ReactiveRedisTemplate<>(factory, context);
    }
}