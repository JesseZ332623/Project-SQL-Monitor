package com.jesse.sqlmonitor.properties;

// import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** 来自配置文件的 RabbitMQ 属性类。*/
@Data
@ToString
@Component
@ConfigurationProperties(prefix = "app.rabbitmq")
public class RabbitMQProperties
{
    private String host;
    private int    port;
    private String user;
    private String password;
    private String virtualHost;
    private int    connectTimeout;

//    @PostConstruct
//    private void showProperties() {
//        System.out.println(this);
//    }
}