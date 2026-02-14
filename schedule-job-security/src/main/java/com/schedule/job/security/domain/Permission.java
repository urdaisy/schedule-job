package com.schedule.job.security.domain;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Permission {
    private Long id;

    private String permissionName;

    private String permissionCode;

    private String resource;

    private String action;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
