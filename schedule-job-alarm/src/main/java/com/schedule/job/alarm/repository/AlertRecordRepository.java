package com.schedule.job.alarm.repository;

import com.schedule.job.alarm.domain.AlertRecord;
import com.schedule.job.alarm.entity.AlertRecordEntity;
import com.schedule.job.alarm.entity.EntityConvert;
import com.schedule.job.common.infra.AbstractBaseRepository;
import com.schedule.job.common.infra.jdbc.JdbcUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Repository
public class AlertRecordRepository extends AbstractBaseRepository<AlertRecordEntity, Long> {
    
    /**
     * 保存告警记录
     */
    public AlertRecord saveAlertRecord(AlertRecord alertRecord) {
        AlertRecordEntity entity = EntityConvert.convertToAlertRecordEntity(alertRecord);
        boolean saved = this.save(entity);
        if (!saved) {
            throw new RuntimeException("保存告警记录失败");
        }
        return EntityConvert.convertToAlertRecord(entity);
    }
    
    /**
     * 更新告警记录状态
     */
    public boolean updateAlertRecordStatus(Long id, Integer status, String errorMsg) {
        try {
            AlertRecordEntity entity = this.findById(id);
            if (entity == null) {
                return false;
            }
            entity.setAlertStatus(status);
            if (errorMsg != null) {
                entity.setErrorMsg(errorMsg);
            }
            return this.update(entity);
        } catch (Exception e) {
            log.error("更新告警记录状态失败, id: {}", id, e);
            return false;
        }
    }
    
    /**
     * 根据任务ID查询告警记录
     */
    public List<AlertRecord> findByJobId(Long jobId) {
        try {
            StringBuilder sql = new StringBuilder("SELECT * FROM ").append(this.tableName)
                    .append(" WHERE job_id = ? ORDER BY alert_time DESC");
            List<AlertRecordEntity> entities = JdbcUtil.executeQuery(sql.toString(), this::mapResultSetToEntity, jobId);
            return entities.stream()
                    .map(EntityConvert::convertToAlertRecord)
                    .collect(Collectors.toList());
        } catch (SQLException e) {
            log.error("查询告警记录失败, jobId: {}", jobId, e);
            throw new RuntimeException("查询告警记录失败", e);
        }
    }
    
    /**
     * 根据任务日志ID查询告警记录
     */
    public List<AlertRecord> findByJobLogId(Long jobLogId) {
        try {
            StringBuilder sql = new StringBuilder("SELECT * FROM ").append(this.tableName)
                    .append(" WHERE job_log_id = ? ORDER BY alert_time DESC");
            List<AlertRecordEntity> entities = JdbcUtil.executeQuery(sql.toString(), this::mapResultSetToEntity, jobLogId);
            return entities.stream()
                    .map(EntityConvert::convertToAlertRecord)
                    .collect(Collectors.toList());
        } catch (SQLException e) {
            log.error("查询告警记录失败, jobLogId: {}", jobLogId, e);
            throw new RuntimeException("查询告警记录失败", e);
        }
    }
}
