package com.schedule.job.security.entity;
import com.schedule.job.common.infra.orm.Column;
import com.schedule.job.common.infra.orm.Entity;
import com.schedule.job.common.infra.orm.Id;


import com.schedule.job.common.infra.orm.UniqueConstraint;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity(name = "sys_job_role", uniqueConstraints = {@UniqueConstraint(columnNames = {"role_code"})})
public class RoleInfoEntity {
    @Id(autoIncrement = true)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "role_name", nullable = false)
    private String roleName;

    @Column(name = "role_code", nullable = false)
    private String roleCode;

    @Column(name = "description")
    private String description;

    @Column(name = "create_time", nullable = false)
    private LocalDateTime createTime;

    @Column(name = "update_time", nullable = false)
    private LocalDateTime updateTime;

    // 无参构造，反射需要
    public RoleInfoEntity() {}
}
