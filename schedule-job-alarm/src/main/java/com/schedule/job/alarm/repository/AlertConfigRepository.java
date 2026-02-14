package com.schedule.job.alarm.repository;

import com.schedule.job.alarm.domain.AlertConfig;
import com.schedule.job.alarm.entity.AlertConfigEntity;
import com.schedule.job.alarm.entity.EntityConvert;
import com.schedule.job.common.infra.AbstractBaseRepository;
import com.schedule.job.common.infra.jdbc.JdbcUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Repository
public class AlertConfigRepository extends AbstractBaseRepository<AlertConfigEntity, Long> {
    
    /**
     * 根据任务ID查询告警配置
     */
    public List<AlertConfig> findByJobId(Long jobId) {
        try {
            StringBuilder sql = new StringBuilder("SELECT * FROM ").append(this.tableName)
                    .append(" WHERE job_id = ? AND enabled = 1");
            List<AlertConfigEntity> entities = JdbcUtil.executeQuery(sql.toString(), this::mapResultSetToEntity, jobId);
            return entities.stream()
                    .map(EntityConvert::convertToAlertConfig)
                    .collect(Collectors.toList());
        } catch (SQLException e) {
            log.error("查询告警配置失败, jobId: {}", jobId, e);
            throw new RuntimeException("查询告警配置失败", e);
        }
    }
    
    /**
     * 根据任务ID和告警类型查询告警配置
     */
    public Optional<AlertConfig> findByJobIdAndAlertType(Long jobId, String alertType) {
        try {
            StringBuilder sql = new StringBuilder("SELECT * FROM ").append(this.tableName)
                    .append(" WHERE job_id = ? AND alert_type = ?");
            List<AlertConfigEntity> entities = JdbcUtil.executeQuery(sql.toString(), this::mapResultSetToEntity, jobId, alertType);
            if (entities.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(EntityConvert.convertToAlertConfig(entities.get(0)));
        } catch (SQLException e) {
            log.error("查询告警配置失败, jobId: {}, alertType: {}", jobId, alertType, e);
            throw new RuntimeException("查询告警配置失败", e);
        }
    }
    
    /**
     * 保存告警配置（如果已存在相同jobId和alertType的配置，则更新）
     */
    public AlertConfig saveAlertConfig(AlertConfig alertConfig) {
        // 先查询是否已存在
        Optional<AlertConfig> existing = findByJobIdAndAlertType(alertConfig.getJobId(), alertConfig.getAlertType());
        
        AlertConfigEntity entity = EntityConvert.convertToAlertConfigEntity(alertConfig);
        
        if (existing.isPresent()) {
            // 更新现有配置
            entity.setId(existing.get().getId());
            boolean updated = this.update(entity);
            if (!updated) {
                throw new RuntimeException("更新告警配置失败");
            }
        } else {
            // 新增配置
            boolean saved = this.save(entity);
            if (!saved) {
                throw new RuntimeException("保存告警配置失败");
            }
        }
        return EntityConvert.convertToAlertConfig(entity);
    }
    
    /**
     * 更新告警配置
     */
    public AlertConfig updateAlertConfig(AlertConfig alertConfig) {
        AlertConfigEntity entity = EntityConvert.convertToAlertConfigEntity(alertConfig);
        boolean updated = this.update(entity);
        if (!updated) {
            throw new RuntimeException("更新告警配置失败");
        }
        return EntityConvert.convertToAlertConfig(entity);
    }
    
    /**
     * 删除告警配置
     */
    public boolean deleteAlertConfig(Long id) {
        return this.deleteById(id);
    }
}
