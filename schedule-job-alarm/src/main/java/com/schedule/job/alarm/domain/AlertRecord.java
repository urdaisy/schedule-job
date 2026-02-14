package com.schedule.job.alarm.domain;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AlertRecord {
    private Long id;
    private Long jobId;
    private Long jobLogId;
    private String alertType;
    private String receiver;
    private Integer alertStatus; // 0-待发送、1-发送成功、2-发送失败
    private LocalDateTime alertTime;
    private String errorMsg;
    private LocalDateTime createTime;
}
