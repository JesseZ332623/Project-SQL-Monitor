package com.jesse.sqlmonitor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/** MySQL 监视器应用启动类。*/
@EnableScheduling
@SpringBootApplication
public class SQLMonitorApplication
{
	public static void main(String[] args) {
		SpringApplication.run(SQLMonitorApplication.class, args);
	}
}