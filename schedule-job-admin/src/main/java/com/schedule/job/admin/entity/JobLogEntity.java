package com.schedule.job.admin.entity;

import com.schedule.job.common.infra.orm.Column;
import com.schedule.job.common.infra.orm.Entity;
import com.schedule.job.common.infra.orm.Id;


import com.schedule.job.common.infra.orm.UniqueConstraint;
import jakarta.persistence.PrePersist;

import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity(name = "sys_job_log", uniqueConstraints = {@UniqueConstraint(columnNames = {"job_id"})})
public class JobLogEntity {
    @Id(autoIncrement = true)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "job_id", nullable = false)
    private Long jobId;

    @Column(name = "trigger_time", nullable = false)
    private LocalDateTime triggerTime;

    @Column(name = "execute_time", nullable = false)
    private LocalDateTime executeTime;

    @Column(name = "duration", nullable = false)
    private Long duration;

    @Column(name = "status", nullable = false)
    private Integer status;

    @Column(name = "error_msg", nullable = false)
    private String errorMsg;

    @Column(name = "retry_count", nullable = false)
    private Integer retryCount;

    @Column(name = "create_time", nullable = false)
    private LocalDateTime createTime;

    @Column(name = "update_time", nullable = false)
    private LocalDateTime updateTime;

    @Column(name = "deleted", nullable = false)
    private Integer deleted;

    // 初始化状态为未删除
    @PrePersist
    public void preDeleted() {
        if (this.deleted == null) {
            this.deleted = 0;
        }
    }

    // 无参构造，反射需要
    public JobLogEntity() {}
}
