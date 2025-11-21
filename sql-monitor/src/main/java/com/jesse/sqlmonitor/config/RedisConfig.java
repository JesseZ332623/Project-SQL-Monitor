package com.jesse.sqlmonitor.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jesse.sqlmonitor.properties.RedisProperties;
import io.lettuce.core.ClientOptions;
import io.lettuce.core.SocketOptions;
import io.lettuce.core.TimeoutOptions;
import lombok.RequiredArgsConstructor;
import org.redisson.Redisson;
import org.redisson.api.RedissonReactiveClient;
import org.redisson.config.Config;
import org.redisson.config.FullJitterDelay;
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
        config.setDatabase(0);                                    // 明确指定数据库

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
                    .suspendReconnectOnProtocolFailure(false) // 重连时不取消命令
                    .disconnectedBehavior(
                        // 断开连接时拒绝接收命令
                        ClientOptions.DisconnectedBehavior.REJECT_COMMANDS
                    )
                    .socketOptions(
                        SocketOptions.builder()
                            .connectTimeout(Duration.ofSeconds(15L)) // 连接超时
                            .keepAlive(true) // 自动管理 TCP 连接存活
                            .build()
                    )
                    .timeoutOptions(
                        TimeoutOptions.builder()
                            .fixedTimeout(Duration.ofSeconds(30L)) // 操作超时
                            .build()
                    ).build()
            )
            .commandTimeout(Duration.ofSeconds(30L))  // 命令超时时间
            .shutdownTimeout(Duration.ofSeconds(10L))  // 关闭超时时间
            .build();

        // 3. 创建连接工厂
        LettuceConnectionFactory connectionFactory
            = new LettuceConnectionFactory(config, clientConfig);

        connectionFactory.setValidateConnection(false); // 禁用连接验证

        return connectionFactory;
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
    @Primary
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

    /** Redisson 响应式客户端实例配置。*/
    @Bean
    @Primary
    public RedissonReactiveClient redissonReactiveClient()
    {
        Config singleServerConfig = new Config();

        // 组合服务器地址
        final String redisAddress
            = "redis://"                      +
              this.redisProperties.getHost()  + ":" +
              this.redisProperties.getPort();

        singleServerConfig
            .useSingleServer()
            .setAddress(redisAddress)
            .setUsername(this.redisProperties.getUsername())
            .setPassword(this.redisProperties.getPassword())
            .setTimeout(10000)
            .setRetryAttempts(5)
            /*
             * FullJitterDelay（全抖动）
             * 核心思想：“指数退避 + 全抖动”（Exponential Back - off + Full Jitter）。
             *
             * 初始延迟为 baseDelay（如 100ms）；
             * 随着重试次数增加，当前延迟值按指数增长（例如第1次 100ms，第2次 200ms，第3次 400ms…，直到达到 maxDelay 上限）；
             * 每次重试的实际延迟是 [0, 当前延迟值) 内的随机值（“全抖动”指随机范围覆盖整个当前延迟区间）。
             */
            .setRetryDelay(new FullJitterDelay(Duration.ofSeconds(2L), Duration.ofSeconds(8L)))
            .setConnectionPoolSize(128)
            .setConnectionMinimumIdleSize(32)
            .setSubscriptionConnectionPoolSize(50)
            .setSubscriptionConnectionMinimumIdleSize(10)
            .setKeepAlive(true)
            .setPingConnectionInterval(30000)   // 30 秒一次心跳检查
            .setDnsMonitoringInterval(5000);    // DNS 监控

        return
        Redisson.create(singleServerConfig).reactive();
    }
}