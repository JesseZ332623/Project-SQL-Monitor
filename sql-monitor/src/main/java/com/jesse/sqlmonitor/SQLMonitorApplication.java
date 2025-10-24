package com.jesse.sqlmonitor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** MySQL 监视器应用启动类。*/
@SpringBootApplication
public class SQLMonitorApplication
{
	public static void main(String[] args) {
		SpringApplication.run(SQLMonitorApplication.class, args);
	}
}