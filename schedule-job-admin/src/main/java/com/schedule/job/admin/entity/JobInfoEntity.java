package com.schedule.job.admin.entity;

import jakarta.persistence.*;
import lombok.Data;
import com.schedule.job.admin.orm.UniqueConstraint;
import com.schedule.job.admin.orm.Entity;
import com.schedule.job.admin.orm.Id;
import com.schedule.job.admin.orm.Column;

import java.time.LocalDateTime;

@Data
@Entity(name = "sys_job", uniqueConstraints = {@UniqueConstraint(columnNames = {"job_name", "job_group"})})
public class JobInfoEntity {
    @Id(autoIncrement = true)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "job_name", nullable = false)
    private String jobName; // 任务名称

    @Column(name = "job_group", nullable = false)
    private String jobGroup; // 任务组名

    @Column(name = "job_class_name", nullable = false)
    private String jobClassName; // 任务执行类全路径（必须实现Quartz的Job接口）

    @Column(name = "cron_expression", nullable = false)
    private String cronExpression; // Cron表达式

    @Column(name = "description")
    private String description; // 任务描述

    @Column(name = "job_param")
    private String jobParam; // 任务参数（JSON格式字符串）

    @Column(name = "status", nullable = false)
    private Integer status; // 0-暂停，1-运行中

    @Column(name = "start_time")
    private LocalDateTime startTime; // 创建时间

    @Column(name = "update_time")
    private LocalDateTime updateTime; // 更新时间

    @Column(name = "last_execution_time")
    private LocalDateTime lastExecutionTime; // 最后执行时间（可选，用于监控）

    // 初始化状态为运行中
    @PrePersist
    public void prePersist() {
        if (this.status == null) {
            this.status = 1; // 默认运行中
        }
    }

    // 无参构造，反射需要
    public JobInfoEntity() {}
}