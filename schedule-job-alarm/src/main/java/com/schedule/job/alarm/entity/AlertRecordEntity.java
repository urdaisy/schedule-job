package com.schedule.job.alarm.entity;

import com.schedule.job.common.infra.orm.Column;
import com.schedule.job.common.infra.orm.Entity;
import com.schedule.job.common.infra.orm.Id;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity(name = "sys_job_alert_record")
public class AlertRecordEntity {
    @Id(autoIncrement = true)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "job_id", nullable = false)
    private Long jobId;

    @Column(name = "job_log_id", nullable = false)
    private Long jobLogId;

    @Column(name = "alert_type", nullable = false)
    private String alertType;

    @Column(name = "receiver", nullable = true)
    private String receiver;

    @Column(name = "alert_status", nullable = false)
    private Integer alertStatus;

    @Column(name = "alert_time", nullable = false)
    private LocalDateTime alertTime;

    @Column(name = "error_msg", nullable = true)
    private String errorMsg;

    @Column(name = "create_time", nullable = false)
    private LocalDateTime createTime;

    // 无参构造，反射需要
    public AlertRecordEntity() {}
}
