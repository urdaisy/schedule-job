package com.schedule.job.admin.entity;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class AlertMessageMq implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Long jobId;
    private Long jobLogId;
    private String jobName;
    private String errorMessage;
    private LocalDateTime alarmTime;
}
