package com.schedule.job.security.entity;

import com.schedule.job.common.infra.orm.Column;
import com.schedule.job.common.infra.orm.Entity;
import com.schedule.job.common.infra.orm.Id;
import com.schedule.job.common.infra.orm.UniqueConstraint;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity(name = "sys_job_user", uniqueConstraints = {@UniqueConstraint(columnNames = {"username"})})
public class UserInfoEntity {
    @Id(autoIncrement = true)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "username", nullable = false)
    private String username;

    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "role_id", nullable = false)
    private Long roleId;

    @Column(name = "status", nullable = false)
    private Integer status;

    @Column(name = "create_time", nullable = false)
    private LocalDateTime createTime;

    @Column(name = "update_time", nullable = false)
    private LocalDateTime updateTime;

    // 无参构造，反射需要
    public UserInfoEntity() {}
}
