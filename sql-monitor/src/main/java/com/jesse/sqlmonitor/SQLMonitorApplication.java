package com.jesse.sqlmonitor;

import com.jesse.sqlmonitor.banner.CustomBannerPrinter;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;


/** sql-monitor 应用启动类。*/
@EnableScheduling
@SpringBootApplication
public class SQLMonitorApplication
{
    static {
        CustomBannerPrinter.printStartupBanner();
    }

    public static void main(String[] args) {
        SpringApplication.run(SQLMonitorApplication.class, args);
    }
}