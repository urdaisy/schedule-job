package com.schedule.job.admin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * 启动类，排除：Quartz默认数据源自动配置和要Spring默认的HikariCP
 */
@SpringBootApplication
@ComponentScan(basePackages = {"com.schedule.job"})
public class AdminApplication {
    public static void main(String[] args) {
        SpringApplication.run(AdminApplication.class, args);
    }
}