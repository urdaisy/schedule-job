package com.schedule.job.admin.repository;

import com.schedule.job.admin.entity.EntityConvert;
import com.schedule.job.admin.entity.JobInfoEntity;
import com.schedule.job.admin.jdbc.JdbcUtil;
import com.schedule.job.admin.job.JobInfo;
import com.schedule.job.common.enums.BusinessExceptionCode;
import com.schedule.job.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

@Slf4j
@Repository
public class JobInfoRepository extends AbstractBaseRepository<JobInfoEntity, Long>{
    public Optional<JobInfo> findByJobNameAndJobGroup(String jobName, String jobGroup) {
        try {
            StringBuilder sql = new StringBuilder("SELECT * FROM").append(this.tableName)
                    .append("WHERE job_name = ? AND job_group = ?");
            List<JobInfoEntity> jobInfoEntities = JdbcUtil.executeQuery(sql.toString(), this::mapResultSetToEntity, jobName, jobGroup);
            JobInfo jobInfo = jobInfoEntities.size() == 0 ? null : EntityConvert.convertToDomain(jobInfoEntities.get(0));
            return Optional.ofNullable(jobInfo);
        } catch (SQLException e) {
            throw new BusinessException(BusinessExceptionCode.JOB_NOT_FOUND);
        }
    }

    public JobInfo saveByEntity(JobInfo oldJobInfo) {
        JobInfoEntity jobInfoEntity = EntityConvert.convertToEntity(oldJobInfo);
        boolean isSaved = this.save(jobInfoEntity);
        if (!isSaved) {
            throw new BusinessException(BusinessExceptionCode.JOB_INSERT_FAILED, "任务名+任务组创建失败，请稍后重试");
        }
        JobInfoEntity jobInfoEntity1 = this.findById(jobInfoEntity.getId());
        return EntityConvert.convertToDomain(jobInfoEntity1);
    }

    public JobInfo findByJobGroup(String jobGroup) throws SQLException {
        StringBuilder sql = new StringBuilder("SELECT * FROM").append(this.tableName).append(" WHERE job_group = ?");
        List<JobInfoEntity> jobInfos = JdbcUtil.executeQuery(sql.toString(), this::mapResultSetToEntity, jobGroup);
        return jobInfos.isEmpty() ? null : EntityConvert.convertToDomain(jobInfos.get(0));
    }

    public JobInfo findByJobName(String jobName) throws SQLException {
        StringBuilder sql = new StringBuilder("SELECT * FROM").append(this.tableName).append(" WHERE job_name = ?");
        List<JobInfoEntity> jobInfos = JdbcUtil.executeQuery(sql.toString(), this::mapResultSetToEntity, jobName);
        return jobInfos.isEmpty() ? null : EntityConvert.convertToDomain(jobInfos.get(0));
    }

    public List<JobInfo> findAllJobInfo() {
        List<JobInfoEntity> jobInfoEntities = this.findAll();
        return jobInfoEntities.isEmpty() ? null : EntityConvert.convertToDomainList(jobInfoEntities);
    }

    public int deleteByJobGroup(String jobGroup) throws SQLException {
        StringBuilder sql = new StringBuilder("DELETE * FROM").append(this.tableName).append(" WHERE job_group = ?");
        List<JobInfoEntity> jobInfos = JdbcUtil.executeQuery(sql.toString(), this::mapResultSetToEntity, jobGroup);
        return jobInfos.size();
    }
    public int deleteByJobName(String jobName) throws SQLException {
        StringBuilder sql = new StringBuilder("DELETE * FROM").append(this.tableName).append(" WHERE job_name = ?");
        List<JobInfoEntity> jobInfos = JdbcUtil.executeQuery(sql.toString(), this::mapResultSetToEntity, jobName);
        return jobInfos.size();
    }

    public void delete(JobInfo jobInfo) {
        boolean deleteById = this.deleteById(jobInfo.getId());
        if (!deleteById) {
            log.info("delete job info id={}", jobInfo.getId());
        }
    }

    public JobInfo findByEntity(Long id) {
        JobInfoEntity jobInfoEntity = this.findById(id);
        return EntityConvert.convertToDomain(jobInfoEntity);
    }
}
