package com.schedule.job.security.entity;

import com.schedule.job.common.infra.orm.Column;
import com.schedule.job.common.infra.orm.Entity;
import com.schedule.job.common.infra.orm.Id;

import com.schedule.job.common.infra.orm.UniqueConstraint;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity(name = "sys_job_user_session", uniqueConstraints = {@UniqueConstraint(columnNames = {"token"})})
public class UserSessionEntity {
    @Id(autoIncrement = true)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "token", nullable = false)
    private String token;

    @Column(name = "expire_time", nullable = false)
    private LocalDateTime expireTime;

    @Column(name = "create_time", nullable = false)
    private LocalDateTime createTime;

    // 无参构造，反射需要
    public UserSessionEntity() {}
}
