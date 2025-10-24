package com.jesse.indicator_receiver.properties;

// import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** 来自配置文件的 R2DBC 从数据库属性类。*/
@Data
@ToString
@Component
@ConfigurationProperties(prefix = "app.r2dbc.slaver")
public class R2dbcSlaverProperties
{
    private String host;
    private int    port;
    private String user;
    private String password;
    private String defaultSchema;

//    @PostConstruct
//    private void showProperties() {
//        System.out.println(this);
//    }
}