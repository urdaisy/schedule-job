package com.schedule.job.alarm.entity;

import com.schedule.job.common.infra.orm.Column;
import com.schedule.job.common.infra.orm.Entity;
import com.schedule.job.common.infra.orm.Id;
import com.schedule.job.common.infra.orm.UniqueConstraint;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity(name = "sys_job_alert_config", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"job_id", "alert_type"})
})
public class AlertConfigEntity {
    @Id(autoIncrement = true)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "job_id", nullable = false)
    private Long jobId;

    @Column(name = "alert_type", nullable = false)
    private String alertType;

    @Column(name = "receiver_email", nullable = false)
    private String receiverEmail;

    @Column(name = "enabled", nullable = false)
    private Integer enabled;

    @Column(name = "create_time", nullable = false)
    private LocalDateTime createTime;

    @Column(name = "update_time", nullable = false)
    private LocalDateTime updateTime;

    // 无参构造，反射需要
    public AlertConfigEntity() {}
}
