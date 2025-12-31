package com.schedule.job.admin.config;

import com.schedule.job.admin.jdbc.CustomQuartzDataSource;
import org.quartz.Scheduler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;

import java.io.IOException;

@Configuration
public class QuartzConfig {
    /**
     * 配置 SchedulerFactoryBean 核心调度器
     */
    @Bean
    public SchedulerFactoryBean schedulerFactoryBean() throws IOException {
        SchedulerFactoryBean factory = new SchedulerFactoryBean();
        // 持久化数据库
        factory.setDataSource(new CustomQuartzDataSource());
        return factory;
    }

    /**
     * 暴露 Scheduler 实例到 Spring 容器
     */
    @Bean
    public Scheduler scheduler(SchedulerFactoryBean schedulerFactoryBean) throws Exception {
        return schedulerFactoryBean.getScheduler();
    }
}
