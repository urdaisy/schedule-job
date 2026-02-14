package com.schedule.job.security.domain;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户详细信息（包含角色和权限）
 */
@Data
public class UserDetailInfo {
    private Long id;
    private String username;
    private String email;
    private Long roleId;
    private String roleName;
    private String roleCode;
    private String roleDescription;
    private Integer status;
    private List<Permission> permissions;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
