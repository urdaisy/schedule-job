package com.schedule.job.admin.repository;

import com.schedule.job.admin.entity.EntityConvert;
import com.schedule.job.admin.entity.JobLogEntity;
import com.schedule.job.common.infra.jdbc.JdbcUtil;
import com.schedule.job.admin.job.JobLog;
import com.schedule.job.common.infra.AbstractBaseRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Repository
public class JobLogRepository extends AbstractBaseRepository<JobLogEntity, Long> {
    /**
     * 保存任务执行日志
     * @param jobId 任务ID
     * @param triggerTime 触发时间
     * @param executeTime 执行时间
     * @param duration 执行耗时（毫秒）
     * @param status 状态：0-成功，1-失败
     * @param errorMsg 错误信息
     * @param retryCount 重试次数
     * @return 保存的日志ID，如果保存失败返回null
     */
    public Long saveJobLog(Long jobId, LocalDateTime triggerTime, LocalDateTime executeTime,
                           long duration, int status, String errorMsg, int retryCount)  {
        try {
            JobLog jobLog = new JobLog();
            jobLog.setJobId(jobId);
            jobLog.setTriggerTime(triggerTime);
            jobLog.setExecuteTime(executeTime);
            jobLog.setDuration(duration);
            jobLog.setStatus(status);

            // 限制错误信息长度，避免数据库字段溢出
            String finalErrorMsg = errorMsg != null ? errorMsg : "";
            if (finalErrorMsg.length() > 500) {
                finalErrorMsg = finalErrorMsg.substring(0, 497) + "...";
            }
            jobLog.setErrorMsg(finalErrorMsg);

            jobLog.setRetryCount(retryCount);
            jobLog.setCreateTime(LocalDateTime.now());
            jobLog.setUpdateTime(LocalDateTime.now());
            jobLog.setDeleted(0);

            JobLogEntity jobLogEntity = EntityConvert.convertToLogEntity(jobLog);
            boolean saved = this.save(jobLogEntity);
            if (!saved) {
                log.error("保存任务日志失败，jobId={}", jobId);
                return null;
            }
            return jobLogEntity.getId();
        } catch (Exception e) {
            log.error("保存任务日志异常，jobId={}", jobId, e);
            throw new RuntimeException("保存任务日志失败", e);
        }
    }


    /**
     * 根据任务ID查询执行日志
     * @param jobId 任务ID
     * @return 日志列表
     */
    public List<JobLog> queryJobLogByJobId(Long jobId) throws SQLException {
        StringBuilder sql = new StringBuilder("SELECT * FROM ").append(this.tableName).append(" WHERE job_id = ? AND deleted = 0");
        List<JobLogEntity> jobLogEntityList = JdbcUtil.executeQuery(sql.toString(), this::mapResultSetToEntity, jobId);
        List<JobLog> jobLogList =
                jobLogEntityList.stream().map(EntityConvert::convertToLog).collect(Collectors.toList());
        return jobLogList;
    }

    /**
     * 分页查询所有任务日志
     * @param pageNum 页码（从1开始）
     * @param pageSize 每页条数
     * @return 分页结果
     */
    public PageInfo<JobLog> queryJobLogByPage(Integer pageNum, Integer pageSize) {
        try {
            // 参数校验
            if (pageNum == null || pageNum < 1) {
                pageNum = 1;
            }
            if (pageSize == null || pageSize < 1) {
                pageSize = 10;
            }

            // 1. 查询总数（只查询未删除的记录）
            Long total = JdbcUtil.executeCountQuery("SELECT COUNT(*) FROM " + this.tableName + " WHERE deleted = 0");

            // 2. 查询分页数据
            // 使用 LIMIT ? OFFSET ? 实现物理分页
            // 计算偏移量（OFFSET = (pageNum - 1) * pageSize）
            int offset = (pageNum - 1) * pageSize;
            String querySql = "SELECT * FROM " + this.tableName 
                    + " WHERE deleted = 0" 
                    + " ORDER BY create_time DESC" 
                    + " LIMIT ? OFFSET ?";
            
            List<JobLogEntity> jobLogEntityList = JdbcUtil.executeQuery(
                    querySql, 
                    this::mapResultSetToEntity, 
                    pageSize, 
                    offset
            );

            // 3. 转换为领域对象
            List<JobLog> jobLogList = new ArrayList<>();
            if (!jobLogEntityList.isEmpty()) {
                jobLogList = jobLogEntityList.stream()
                        .map(EntityConvert::convertToLog)
                        .collect(Collectors.toList());
            }

            // 4. 构建分页结果
            return new PageInfo<>(pageNum, pageSize, total, jobLogList);
        } catch (SQLException e) {
            log.error("分页查询任务日志失败，pageNum={}, pageSize={}", pageNum, pageSize, e);
            throw new RuntimeException("分页查询任务日志失败", e);
        }
    }

    /**
     * 根据ID删除日志（逻辑删除）
     *
     * @param id 日志ID
     * @return 是否删除成功
     */
    public boolean deleteJobLog(Long id) {
        try {
            StringBuilder sql = new StringBuilder("UPDATE ").append(this.tableName).append(" SET deleted = 1 WHERE id = ?");
            int rows = JdbcUtil.executeUpdate(sql.toString(), id);
            return rows > 0;
        } catch (SQLException e) {
            log.error("逻辑删除任务日志失败，id={}", id, e);
            throw new RuntimeException("逻辑删除任务日志失败", e);
        }
    }
}
