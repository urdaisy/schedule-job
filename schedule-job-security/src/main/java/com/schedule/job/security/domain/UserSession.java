package com.schedule.job.security.domain;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserSession {
    private Long id;

    private Long userId;

    private String token;

    private LocalDateTime expireTime;

    private LocalDateTime createTime;
}
