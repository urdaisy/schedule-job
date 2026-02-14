package com.schedule.job.security.entity;

import com.schedule.job.security.domain.Permission;
import com.schedule.job.security.domain.RoleInfo;
import com.schedule.job.security.domain.UserInfo;
import com.schedule.job.security.domain.UserSession;

public class EntityConvert {
    public static UserInfo convertToUserInfo(UserInfoEntity userInfoEntity) {
        if (userInfoEntity == null) {
            return null;
        }
        UserInfo userInfo = new UserInfo();
        userInfo.setId(userInfoEntity.getId());
        userInfo.setUsername(userInfoEntity.getUsername());
        userInfo.setPassword(userInfoEntity.getPassword());
        userInfo.setEmail(userInfoEntity.getEmail());
        userInfo.setRoleId(userInfoEntity.getRoleId());
        userInfo.setStatus(userInfoEntity.getStatus());
        userInfo.setCreateTime(userInfoEntity.getCreateTime());
        userInfo.setUpdateTime(userInfoEntity.getUpdateTime());
        return userInfo;
    }

    public static RoleInfo convertToRoleInfo(RoleInfoEntity entity) {
        if (entity == null) {
            return null;
        }
        RoleInfo roleInfo = new RoleInfo();
        roleInfo.setId(entity.getId());
        roleInfo.setRoleName(entity.getRoleName());
        roleInfo.setRoleCode(entity.getRoleCode());
        roleInfo.setDescription(entity.getDescription());
        roleInfo.setCreateTime(entity.getCreateTime());
        roleInfo.setUpdateTime(entity.getUpdateTime());
        return roleInfo;
    }

    public static Permission convertToPermission(PermissionEntity entity) {
        if (entity == null) {
            return null;
        }
        Permission permission = new Permission();
        permission.setId(entity.getId());
        permission.setPermissionName(entity.getPermissionName());
        permission.setPermissionCode(entity.getPermissionCode());
        permission.setResource(entity.getResource());
        permission.setAction(entity.getAction());
        permission.setCreateTime(entity.getCreateTime());
        permission.setUpdateTime(entity.getUpdateTime());
        return permission;
    }

    public static UserSession convertToUserSession(UserSessionEntity entity) {
        if (entity == null) {
            return null;
        }
        UserSession userSession = new UserSession();
        userSession.setId(entity.getId());
        userSession.setUserId(entity.getUserId());
        userSession.setToken(entity.getToken());
        userSession.setExpireTime(entity.getExpireTime());
        userSession.setCreateTime(entity.getCreateTime());
        return userSession;
    }

    public static UserSessionEntity convertToUserSessionEntity(UserSession userSession) {
        if (userSession == null) {
            return null;
        }
        UserSessionEntity entity = new UserSessionEntity();
        entity.setId(userSession.getId());
        entity.setUserId(userSession.getUserId());
        entity.setToken(userSession.getToken());
        entity.setExpireTime(userSession.getExpireTime());
        entity.setCreateTime(userSession.getCreateTime());
        return entity;
    }
}
