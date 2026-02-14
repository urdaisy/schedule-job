package com.schedule.job.security.domain;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RoleInfo {
    private Long id;

    private String roleName;

    private String roleCode;

    private String description;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
