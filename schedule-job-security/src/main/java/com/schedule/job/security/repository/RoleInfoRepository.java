package com.schedule.job.security.repository;

import com.schedule.job.common.infra.jdbc.JdbcUtil;
import com.schedule.job.common.infra.AbstractBaseRepository;
import com.schedule.job.common.enums.BusinessExceptionCode;
import com.schedule.job.common.exception.BusinessException;
import com.schedule.job.security.domain.RoleInfo;
import com.schedule.job.security.entity.EntityConvert;
import com.schedule.job.security.entity.RoleInfoEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

@Slf4j
@Repository
public class RoleInfoRepository extends AbstractBaseRepository<RoleInfoEntity, Long> {
    public Optional<RoleInfo> findByRoleCode(String roleCode) {
        try {
            StringBuilder sql = new StringBuilder("SELECT * FROM ").append(tableName).append(" WHERE role_code=? ");
            List<RoleInfoEntity> roleInfoEntities = JdbcUtil.executeQuery(sql.toString(), this::mapResultSetToEntity, roleCode);
            if (roleInfoEntities.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(EntityConvert.convertToRoleInfo(roleInfoEntities.getFirst()));
        } catch (SQLException e) {
            throw new BusinessException(BusinessExceptionCode.ROLE_NOT_FOUND);
        }
    }
    
    /**
     * 查询所有角色
     */
    public List<RoleInfo> findAllRoles() {
        try {
            StringBuilder sql = new StringBuilder("SELECT * FROM ").append(tableName).append(" ORDER BY id ASC");
            List<RoleInfoEntity> roleInfoEntities = JdbcUtil.executeQuery(sql.toString(), this::mapResultSetToEntity);
            List<RoleInfo> roleInfos = new java.util.ArrayList<>();
            for (RoleInfoEntity entity : roleInfoEntities) {
                roleInfos.add(EntityConvert.convertToRoleInfo(entity));
            }
            return roleInfos;
        } catch (SQLException e) {
            log.error("查询所有角色失败", e);
            throw new BusinessException(BusinessExceptionCode.ROLE_NOT_FOUND, "查询角色列表失败");
        }
    }
    
    /**
     * 查询可注册的角色列表
     * 如果需要允许注册所有角色，可以调用 findAllRoles()
     */
    public List<RoleInfo> findRegisterableRoles() {
        try {
            // 查询所有角色，但排除管理员（roleId=1）
            StringBuilder sql = new StringBuilder("SELECT * FROM ").append(tableName)
                    .append(" WHERE id != 1 ORDER BY id ASC");
            List<RoleInfoEntity> roleInfoEntities = JdbcUtil.executeQuery(sql.toString(), this::mapResultSetToEntity);
            List<RoleInfo> roleInfos = new java.util.ArrayList<>();
            for (RoleInfoEntity entity : roleInfoEntities) {
                roleInfos.add(EntityConvert.convertToRoleInfo(entity));
            }
            return roleInfos;
        } catch (SQLException e) {
            log.error("查询可注册角色列表失败", e);
            throw new BusinessException(BusinessExceptionCode.ROLE_NOT_FOUND, "查询可注册角色列表失败");
        }
    }
    
    /**
     * 根据ID查询角色
     */
    public Optional<RoleInfo> findRoleById(Long id) {
        try {
            RoleInfoEntity entity = super.findById(id);
            if (entity == null) {
                return Optional.empty();
            }
            return Optional.of(EntityConvert.convertToRoleInfo(entity));
        } catch (Exception e) {
            log.error("查询角色失败, roleId: {}", id, e);
            return Optional.empty();
        }
    }
}
