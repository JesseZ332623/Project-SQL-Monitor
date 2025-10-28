package com.jesse.sqlmonitor.config;

import io.github.jessez332623.reactive_email_sender.authorization.EmailServiceAuthCodeGetter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.r2dbc.core.DatabaseClient;

import java.util.Map;
import java.util.Objects;

/** 邮箱服务授权码获取实现。*/
@Slf4j
@Configuration
public class EmailAuthCodeConfig implements EmailServiceAuthCodeGetter
{
    private final DatabaseClient databaseClient;

    @Value("${app.reactive-email-sender.sender-email}")
    private String senderEmail;

    public EmailAuthCodeConfig(
        @Qualifier("R2dbcSlaverDatabaseClient")
        DatabaseClient databaseClient)
    {
        this.databaseClient = databaseClient;
    }

    @Override
    public String get()
    {
        final String authCodeGetterSQL
            = """
            SELECT
                `email_auth_code`
            FROM
                `sql_monitor`.`email_auth_code`
            WHERE
                email = :email
            """;

        Map<String, Object> authCodeResult
            = this.databaseClient
                  .sql(authCodeGetterSQL)
                  .bind("email", senderEmail)
                  .fetch()
                  .one()
                  .doOnError((error) ->
                      log.error(
                          "Get email auth code for {} failed, Caused by: {}",
                          senderEmail, error.getMessage()
                      ))
                  .block();

        return
        (String)
        Objects.requireNonNull(authCodeResult)
               .get("email_auth_code");
    }
}
