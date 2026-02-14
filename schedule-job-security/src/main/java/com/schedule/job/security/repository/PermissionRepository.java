package com.schedule.job.security.repository;

import com.schedule.job.common.infra.jdbc.JdbcUtil;
import com.schedule.job.common.infra.AbstractBaseRepository;
import com.schedule.job.common.enums.BusinessExceptionCode;
import com.schedule.job.common.exception.BusinessException;
import com.schedule.job.security.domain.Permission;
import com.schedule.job.security.entity.EntityConvert;
import com.schedule.job.security.entity.PermissionEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Repository
public class PermissionRepository extends AbstractBaseRepository<PermissionEntity, Long> {
    /**
     * 根据角色id查找权限，需要关联查询
     */
    public List<Permission> findByRoleId(Long roleId) {
        try {
            StringBuilder sql = new StringBuilder("SELECT p.* FROM ").append(this.tableName).append(" p ")
                    .append("INNER JOIN sys_job_role_permission rp ON p.id = rp.permission_id ")
                    .append("WHERE rp.role_id = ?");
            List<PermissionEntity> permissionEntities = JdbcUtil.executeQuery(sql.toString(), this::mapResultSetToEntity, roleId);
            if (permissionEntities.isEmpty()) {
                return new ArrayList<>();
            }
            List<Permission> permissions = new ArrayList<>();
            for (PermissionEntity entity : permissionEntities) {
                permissions.add(EntityConvert.convertToPermission(entity));
            }
            return permissions;
        } catch (SQLException e) {
            throw new BusinessException(BusinessExceptionCode.PERMISSION_NOT_FOUND);
        }
    }
}

