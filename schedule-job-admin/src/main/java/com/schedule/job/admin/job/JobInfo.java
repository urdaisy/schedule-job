package com.schedule.job.admin.job;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class JobInfo {
    private Long id;

    // 任务名称
    private String jobName;

    // 任务组名
    private String jobGroup;

    // 任务描述
    private String description;

    // 任务执行类全路径
    private String jobClassName;

    // cron表达式
    private String cronExpression;

    // 任务参数
    private String jobParam;

    // 任务状态：0-暂停，1-运行中
    private Integer status;

    // 重试次数
    private Integer retryCount;

    // 创建时间
    private LocalDateTime startTime;

    // 更新时间
    private LocalDateTime updateTime;

}
