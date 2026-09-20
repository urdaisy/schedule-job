package com.schedule.job.alarm.domain;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 告警配置领域对象
 */
@Data
public class AlertConfig {
    private Long id;
    private Long jobId;
    private String alertType;
    private String receiverEmail;
    private Integer enabled;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
