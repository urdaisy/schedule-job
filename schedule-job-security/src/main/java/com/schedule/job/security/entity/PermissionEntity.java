package com.schedule.job.security.entity;

import com.schedule.job.common.infra.orm.Column;
import com.schedule.job.common.infra.orm.Entity;
import com.schedule.job.common.infra.orm.Id;

import com.schedule.job.common.infra.orm.UniqueConstraint;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity(name = "sys_job_permission", uniqueConstraints = {@UniqueConstraint(columnNames = {"permission_code"})})
public class PermissionEntity {
    @Id(autoIncrement = true)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "permission_name", nullable = false)
    private String permissionName;

    @Column(name = "permission_code", nullable = false)
    private String permissionCode;

    @Column(name = "resource", nullable = false)
    private String resource;

    @Column(name = "action", nullable = false)
    private String action;

    @Column(name = "create_time", nullable = false)
    private LocalDateTime createTime;

    @Column(name = "update_time", nullable = false)
    private LocalDateTime updateTime;

    // 无参构造，反射需要
    public PermissionEntity() {}
}
