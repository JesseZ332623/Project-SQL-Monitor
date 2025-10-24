package com.jesse.sqlmonitor.config;

import com.jesse.sqlmonitor.properties.RabbitMQProperties;
import com.rabbitmq.client.ConnectionFactory;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.rabbitmq.*;

/** RabbitMQ 配置类。*/
@Configuration
@RequiredArgsConstructor
public class RabbitMQConfiguration
{
    /** 来自配置文件的 RabbitMQ 属性类。*/
    private final RabbitMQProperties properties;

    /** RabbitMQ 连接工厂配置。*/
    @Bean(name = "RabbitMQConnectionFactory")
    public @NotNull ConnectionFactory connectionFactory()
    {
        ConnectionFactory factory = new ConnectionFactory();

        factory.setHost(properties.getHost());
        factory.setPort(properties.getPort());
        factory.setUsername(properties.getUser());
        factory.setPassword(properties.getPassword());
        factory.setVirtualHost(properties.getVirtualHost());
        factory.setConnectionTimeout(properties.getConnectTimeout());

        // 心跳和自动恢复配置
        factory.setRequestedHeartbeat(60);
        factory.setAutomaticRecoveryEnabled(true);
        factory.setNetworkRecoveryInterval(5000);

        return factory;
    }

    /** RabbitMQ 发送器配置。*/
    @Bean
    public Sender
    sender(
        @Qualifier("RabbitMQConnectionFactory")
        ConnectionFactory factory
    )
    {
        return
        RabbitFlux.createSender(
            new SenderOptions().connectionFactory(factory)
        );
    }

    /** RabbitMQ 接收器配置。*/
    @Bean
    public Receiver
    receiver(
        @Qualifier("RabbitMQConnectionFactory")
        ConnectionFactory factory
    )
    {
        return
        RabbitFlux.createReceiver(
            new ReceiverOptions().connectionFactory(factory)
        );
    }
}