package com.jesse.sqlmonitor.properties;

// import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** 来自配置文件的 R2DBC 主数据库属性类。*/
@Data
@ToString
@Component
@ConfigurationProperties(prefix = "app.r2dbc.master")
public class R2dbcMasterProperties
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