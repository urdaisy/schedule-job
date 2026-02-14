package com.schedule.job.security.repository;

import com.schedule.job.common.infra.jdbc.JdbcUtil;
import com.schedule.job.common.infra.AbstractBaseRepository;
import com.schedule.job.common.enums.BusinessExceptionCode;
import com.schedule.job.common.exception.BusinessException;
import com.schedule.job.security.domain.Permission;
import com.schedule.job.security.domain.UserDetailInfo;
import com.schedule.job.security.domain.UserInfo;
import com.schedule.job.security.entity.EntityConvert;
import com.schedule.job.security.entity.UserInfoEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Repository
public class UserInfoRepository extends AbstractBaseRepository<UserInfoEntity, Long> {
    /**
     * 根据用户名查询
     */
    public Optional<UserInfo> findByUsername(String username) {
        try {
            StringBuilder sql = new StringBuilder("select * from ").append(this.tableName).append(" where username=? and status = 1");
            List<UserInfoEntity> userInfoEntity = JdbcUtil.executeQuery(sql.toString(), this::mapResultSetToEntity, username);
            if (userInfoEntity.isEmpty()) {
                return Optional.empty();
            }
            UserInfo userInfo = EntityConvert.convertToUserInfo(userInfoEntity.getFirst());
            return Optional.of(userInfo);
        } catch (SQLException e) {
            throw  new BusinessException(BusinessExceptionCode.USER_NOT_FOUND);
        }
    }

    /**
     * 根据用户名和密码查询
     */
    public Optional<UserInfo> findByUsernameAndPassword(String username, String password) {
        try {
            StringBuilder sql = new StringBuilder("select * from ").append(this.tableName).append(" where username=? and password=?");
            List<UserInfoEntity> userInfoEntity = JdbcUtil.executeQuery(sql.toString(), this::mapResultSetToEntity, username, password);
            if (userInfoEntity.isEmpty()) {
                return Optional.empty();
            }
            UserInfo userInfo = EntityConvert.convertToUserInfo(userInfoEntity.getFirst());
            return Optional.of(userInfo);
        } catch (SQLException e) {
            throw new BusinessException(BusinessExceptionCode.USER_NOT_FOUND);
        }
    }

    /**
     * 修改密码
     */
    public UserInfo updatePassword(Long userId, String password) {
        UserInfoEntity userInfo = this.findById(userId);
        userInfo.setPassword(password);
        boolean isSaved = this.save(userInfo);
        if (!isSaved) {
            throw new BusinessException(BusinessExceptionCode.USER_UPDATE_FAILED, "密码更新失败");
        }
        if (userInfo.getId() == null) {
            log.warn("保存成功但未获取到自增ID，尝试重新查询。userId={}, userName={}", userId, userInfo.getUsername());
            // 如果通过Id获取失败，那么尝试通过username获取
            Optional<UserInfo> userInfo1 =  findByUsername(userInfo.getUsername());
            if (userInfo1.isPresent()) {
                return userInfo1.get();
            } else  {
                log.error("用户修改密码失败，userId={}", userId);
                throw new BusinessException(BusinessExceptionCode.USER_UPDATE_FAILED);
            }
        }
        return EntityConvert.convertToUserInfo(userInfo);
    }
    
    /**
     * 查询所有用户（包括禁用的用户）
     */
    public List<UserInfo> findAllUsers() {
        try {
            StringBuilder sql = new StringBuilder("SELECT * FROM ").append(this.tableName)
                    .append(" ORDER BY create_time DESC");
            List<UserInfoEntity> userInfoEntities = JdbcUtil.executeQuery(sql.toString(), this::mapResultSetToEntity);
            List<UserInfo> userInfos = new ArrayList<>();
            for (UserInfoEntity entity : userInfoEntities) {
                userInfos.add(EntityConvert.convertToUserInfo(entity));
            }
            return userInfos;
        } catch (SQLException e) {
            log.error("查询所有用户失败", e);
            throw new BusinessException(BusinessExceptionCode.USER_NOT_FOUND, "查询用户列表失败");
        }
    }
    
    /**
     * 根据ID查询用户（包括禁用的用户）
     */
    public Optional<UserInfo> findUserById(Long userId) {
        try {
            UserInfoEntity entity = this.findById(userId);
            if (entity == null) {
                return Optional.empty();
            }
            return Optional.of(EntityConvert.convertToUserInfo(entity));
        } catch (Exception e) {
            log.error("查询用户失败, userId: {}", userId, e);
            return Optional.empty();
        }
    }
    
    /**
     * 更新用户信息
     */
    public UserInfo updateUser(UserInfoEntity userEntity) {
        boolean isSaved = this.save(userEntity);
        if (!isSaved) {
            throw new BusinessException(BusinessExceptionCode.USER_UPDATE_FAILED, "用户更新失败");
        }
        return EntityConvert.convertToUserInfo(userEntity);
    }
    
    /**
     * 通过连表查询获取用户详细信息（包含角色和权限）
     * 一次性查询用户、角色和权限信息，避免多次查询
     * 
     * @param userId 用户ID
     * @return 用户详细信息，如果用户不存在返回Optional.empty()
     */
    public Optional<UserDetailInfo> findUserDetailById(Long userId) {
        try {
            StringBuilder sql = new StringBuilder()
                    .append("SELECT ")
                    .append("u.id, u.username, u.email, u.role_id, u.status, u.create_time, u.update_time, ")
                    .append("r.id as role_id_from_role, r.role_name, r.role_code, r.description as role_description, ")
                    .append("p.id as permission_id, p.permission_name, p.permission_code, p.resource, p.action, ")
                    .append("p.create_time as permission_create_time, p.update_time as permission_update_time ")
                    .append("FROM ").append(this.tableName).append(" u ")
                    .append("LEFT JOIN sys_job_role r ON u.role_id = r.id ")
                    .append("LEFT JOIN sys_job_role_permission rp ON r.id = rp.role_id ")
                    .append("LEFT JOIN sys_job_permission p ON rp.permission_id = p.id ")
                    .append("WHERE u.id = ? ")
                    .append("ORDER BY p.id ASC");
            
            List<Map<String, Object>> rows;
            try {
                rows = JdbcUtil.executeQuery(sql.toString(), (ResultSet rs) -> {
                    try {
                        return mapUserDetailResultSet(rs);
                    } catch (SQLException e) {
                        throw new RuntimeException("映射结果集失败", e);
                    }
                }, userId);
            } catch (Exception e) {
                log.error("执行连表查询失败", e);
                throw new BusinessException(BusinessExceptionCode.USER_NOT_FOUND, "查询用户详情失败");
            }
            
            if (rows.isEmpty()) {
                return Optional.empty();
            }
            
            // 将多行结果合并为一个UserDetailInfo对象
            UserDetailInfo userDetail = new UserDetailInfo();
            List<Permission> permissions = new ArrayList<>();
            Map<Long, Permission> permissionMap = new HashMap<>(); // 用于去重
            
            boolean firstRow = true;
            for (Map<String, Object> row : rows) {
                if (firstRow) {
                    // 第一行设置用户和角色信息
                    userDetail.setId((Long) row.get("id"));
                    userDetail.setUsername((String) row.get("username"));
                    userDetail.setEmail((String) row.get("email"));
                    userDetail.setRoleId((Long) row.get("role_id"));
                    userDetail.setStatus((Integer) row.get("status"));
                    userDetail.setCreateTime((LocalDateTime) row.get("create_time"));
                    userDetail.setUpdateTime((LocalDateTime) row.get("update_time"));
                    
                    // 角色信息
                    if (row.get("role_id_from_role") != null) {
                        userDetail.setRoleName((String) row.get("role_name"));
                        userDetail.setRoleCode((String) row.get("role_code"));
                        userDetail.setRoleDescription((String) row.get("role_description"));
                    }
                    firstRow = false;
                }
                
                // 收集权限信息（去重）
                Long permissionId = (Long) row.get("permission_id");
                if (permissionId != null && !permissionMap.containsKey(permissionId)) {
                    Permission permission = new Permission();
                    permission.setId(permissionId);
                    permission.setPermissionName((String) row.get("permission_name"));
                    permission.setPermissionCode((String) row.get("permission_code"));
                    permission.setResource((String) row.get("resource"));
                    permission.setAction((String) row.get("action"));
                    permission.setCreateTime((LocalDateTime) row.get("permission_create_time"));
                    permission.setUpdateTime((LocalDateTime) row.get("permission_update_time"));
                    
                    permissionMap.put(permissionId, permission);
                    permissions.add(permission);
                }
            }
            
            userDetail.setPermissions(permissions);
            return Optional.of(userDetail);
        } catch (Exception e) {
            log.error("连表查询用户详情失败, userId: {}", userId, e);
            throw new BusinessException(BusinessExceptionCode.USER_NOT_FOUND, "查询用户详情失败");
        }
    }
    
    /**
     * 通过连表查询获取所有用户的详细信息（包含角色和权限）
     * 一次性查询所有用户、角色和权限信息
     * 
     * @return 用户详细信息列表
     */
    public List<UserDetailInfo> findAllUserDetails() {
        try {
            StringBuilder sql = new StringBuilder()
                    .append("SELECT ")
                    .append("u.id, u.username, u.email, u.role_id, u.status, u.create_time, u.update_time, ")
                    .append("r.id as role_id_from_role, r.role_name, r.role_code, r.description as role_description, ")
                    .append("p.id as permission_id, p.permission_name, p.permission_code, p.resource, p.action, ")
                    .append("p.create_time as permission_create_time, p.update_time as permission_update_time ")
                    .append("FROM ").append(this.tableName).append(" u ")
                    .append("LEFT JOIN sys_job_role r ON u.role_id = r.id ")
                    .append("LEFT JOIN sys_job_role_permission rp ON r.id = rp.role_id ")
                    .append("LEFT JOIN sys_job_permission p ON rp.permission_id = p.id ")
                    .append("ORDER BY u.create_time DESC, p.id ASC");
            
            List<Map<String, Object>> rows;
            try {
                rows = JdbcUtil.executeQuery(sql.toString(), (ResultSet rs) -> {
                    try {
                        return mapUserDetailResultSet(rs);
                    } catch (SQLException e) {
                        throw new RuntimeException("映射结果集失败", e);
                    }
                });
            } catch (Exception e) {
                log.error("执行连表查询失败", e);
                throw new BusinessException(BusinessExceptionCode.USER_NOT_FOUND, "查询用户列表失败");
            }
            
            // 按用户ID分组，合并权限
            Map<Long, UserDetailInfo> userMap = new HashMap<>();
            
            for (Map<String, Object> row : rows) {
                Long userId = (Long) row.get("id");
                UserDetailInfo userDetail = userMap.get(userId);
                
                if (userDetail == null) {
                    // 创建新的用户详情对象
                    userDetail = new UserDetailInfo();
                    userDetail.setId(userId);
                    userDetail.setUsername((String) row.get("username"));
                    userDetail.setEmail((String) row.get("email"));
                    userDetail.setRoleId((Long) row.get("role_id"));
                    userDetail.setStatus((Integer) row.get("status"));
                    userDetail.setCreateTime((LocalDateTime) row.get("create_time"));
                    userDetail.setUpdateTime((LocalDateTime) row.get("update_time"));
                    
                    // 角色信息
                    if (row.get("role_id_from_role") != null) {
                        userDetail.setRoleName((String) row.get("role_name"));
                        userDetail.setRoleCode((String) row.get("role_code"));
                        userDetail.setRoleDescription((String) row.get("role_description"));
                    }
                    
                    userDetail.setPermissions(new ArrayList<>());
                    userMap.put(userId, userDetail);
                }
                
                // 添加权限（去重）
                Long permissionId = (Long) row.get("permission_id");
                if (permissionId != null) {
                    boolean exists = userDetail.getPermissions().stream()
                            .anyMatch(p -> p.getId().equals(permissionId));
                    
                    if (!exists) {
                        Permission permission = new Permission();
                        permission.setId(permissionId);
                        permission.setPermissionName((String) row.get("permission_name"));
                        permission.setPermissionCode((String) row.get("permission_code"));
                        permission.setResource((String) row.get("resource"));
                        permission.setAction((String) row.get("action"));
                        permission.setCreateTime((LocalDateTime) row.get("permission_create_time"));
                        permission.setUpdateTime((LocalDateTime) row.get("permission_update_time"));
                        
                        userDetail.getPermissions().add(permission);
                    }
                }
            }
            
            return new ArrayList<>(userMap.values());
        } catch (Exception e) {
            log.error("连表查询所有用户详情失败", e);
            throw new BusinessException(BusinessExceptionCode.USER_NOT_FOUND, "查询用户列表失败");
        }
    }
    
    /**
     * 映射连表查询结果集
     */
    private Map<String, Object> mapUserDetailResultSet(ResultSet rs) throws SQLException {
        Map<String, Object> row = new HashMap<>();
        
        // 用户信息
        row.put("id", rs.getLong("id"));
        row.put("username", rs.getString("username"));
        row.put("email", rs.getString("email"));
        row.put("role_id", rs.getObject("role_id") != null ? rs.getLong("role_id") : null);
        row.put("status", rs.getInt("status"));
        
        Object createTime = rs.getTimestamp("create_time");
        row.put("create_time", createTime != null ? 
                ((java.sql.Timestamp) createTime).toLocalDateTime() : null);
        
        Object updateTime = rs.getTimestamp("update_time");
        row.put("update_time", updateTime != null ? 
                ((java.sql.Timestamp) updateTime).toLocalDateTime() : null);
        
        // 角色信息
        Object roleId = rs.getObject("role_id_from_role");
        row.put("role_id_from_role", roleId != null ? rs.getLong("role_id_from_role") : null);
        row.put("role_name", rs.getString("role_name"));
        row.put("role_code", rs.getString("role_code"));
        row.put("role_description", rs.getString("role_description"));
        
        // 权限信息
        Object permissionId = rs.getObject("permission_id");
        row.put("permission_id", permissionId != null ? rs.getLong("permission_id") : null);
        row.put("permission_name", rs.getString("permission_name"));
        row.put("permission_code", rs.getString("permission_code"));
        row.put("resource", rs.getString("resource"));
        row.put("action", rs.getString("action"));
        
        Object permissionCreateTime = rs.getTimestamp("permission_create_time");
        row.put("permission_create_time", permissionCreateTime != null ? 
                ((java.sql.Timestamp) permissionCreateTime).toLocalDateTime() : null);
        
        Object permissionUpdateTime = rs.getTimestamp("permission_update_time");
        row.put("permission_update_time", permissionUpdateTime != null ? 
                ((java.sql.Timestamp) permissionUpdateTime).toLocalDateTime() : null);
        
        return row;
    }
}
