package com.schedule.job.security.domain;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserInfo {
    private Long id;

    private String username;

    private String password;

    private String email;

    private Long roleId;

    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
