package com.schedule.job.admin.entity;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 任务触发消息体
 */
@Data
public class JobTriggerMessageMq implements Serializable {
    private static final long serialVersionUID = 1L;

    private String jobName;
    private String jobGroup;
    private String jobParam;
    private String triggerSource;
    private LocalDateTime triggerTime;
}
