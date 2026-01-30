package com.schedule.job.admin.entity;

import com.schedule.job.admin.job.JobInfo;
import com.schedule.job.admin.job.JobLog;

import java.util.ArrayList;
import java.util.List;

public class EntityConvert {
    public static JobInfoEntity convertToEntity(JobInfo jobInfo) {
        JobInfoEntity jobInfoEntity = new JobInfoEntity();
        jobInfoEntity.setId(jobInfo.getId());
        jobInfoEntity.setJobName(jobInfo.getJobName());
        jobInfoEntity.setJobGroup(jobInfo.getJobGroup());
        jobInfoEntity.setJobClassName(jobInfo.getJobClassName());
        jobInfoEntity.setCronExpression(jobInfo.getCronExpression());
        jobInfoEntity.setDescription(jobInfo.getDescription());
        jobInfoEntity.setJobParam(jobInfo.getJobParam());
        jobInfoEntity.setStatus(jobInfo.getStatus());
        jobInfoEntity.setStartTime(jobInfo.getStartTime());
        jobInfoEntity.setUpdateTime(jobInfo.getUpdateTime());
        return jobInfoEntity;
    }

    public static JobInfo convertToDomain(JobInfoEntity jobInfoEntity) {
        JobInfo jobInfo = new JobInfo();
        jobInfo.setId(jobInfoEntity.getId());
        jobInfo.setJobName(jobInfoEntity.getJobName());
        jobInfo.setJobGroup(jobInfoEntity.getJobGroup());
        jobInfo.setJobClassName(jobInfoEntity.getJobClassName());
        jobInfo.setCronExpression(jobInfoEntity.getCronExpression());
        jobInfo.setDescription(jobInfoEntity.getDescription());
        jobInfo.setJobParam(jobInfoEntity.getJobParam());
        jobInfo.setStatus(jobInfoEntity.getStatus());
        jobInfo.setStartTime(jobInfoEntity.getStartTime());
        jobInfo.setUpdateTime(jobInfoEntity.getUpdateTime());
        return jobInfo;
    }

    public static List<JobInfo> convertToDomainList(List<JobInfoEntity> jobInfoEntityList) {
        List<JobInfo> jobInfos= new ArrayList<>();
        for (JobInfoEntity jobInfoEntity : jobInfoEntityList) {
            jobInfos.add(convertToDomain(jobInfoEntity));
        }
        return jobInfos;
    }

    public static List<JobInfoEntity> convertToEntityList(List<JobInfo> jobInfoEntityList) {
        List<JobInfoEntity> jobInfos= new ArrayList<>();
        for (JobInfo jobInfo : jobInfoEntityList) {
            jobInfos.add(convertToEntity(jobInfo));
        }
        return jobInfos;
    }

    public static JobLogEntity convertToLogEntity(JobLog jobLog) {
        JobLogEntity jobLogEntity = new JobLogEntity();
        jobLogEntity.setId(jobLog.getId());
        jobLogEntity.setJobId(jobLog.getJobId());
        jobLogEntity.setTriggerTime(jobLog.getTriggerTime());
        jobLogEntity.setExecuteTime(jobLog.getExecuteTime());
        jobLogEntity.setDuration(jobLog.getDuration());
        jobLogEntity.setStatus(jobLog.getStatus());
        jobLogEntity.setErrorMsg(jobLog.getErrorMsg());
        jobLogEntity.setRetryCount(jobLog.getRetryCount());
        jobLogEntity.setCreateTime(jobLog.getCreateTime());
        jobLogEntity.setUpdateTime(jobLog.getUpdateTime());
        jobLogEntity.setDeleted(jobLog.getDeleted());
        return jobLogEntity;
    }

    public static JobLog convertToLogDomain(JobLogEntity jobLogEntity) {
        JobLog jobLog = new JobLog();
        jobLog.setId(jobLogEntity.getId());
        jobLog.setJobId(jobLogEntity.getJobId());
        jobLog.setTriggerTime(jobLogEntity.getTriggerTime());
        jobLog.setExecuteTime(jobLogEntity.getExecuteTime());
        jobLog.setDuration(jobLogEntity.getDuration());
        jobLog.setStatus(jobLogEntity.getStatus());
        jobLog.setErrorMsg(jobLogEntity.getErrorMsg());
        jobLog.setRetryCount(jobLogEntity.getRetryCount());
        jobLog.setCreateTime(jobLogEntity.getCreateTime());
        jobLog.setUpdateTime(jobLogEntity.getUpdateTime());
        jobLog.setDeleted(jobLogEntity.getDeleted());
        return jobLog;
    }

    public static JobLog convertToLog(JobLogEntity jobLogEntity) {
        return convertToLogDomain(jobLogEntity);
    }

    public static List<JobLog> convertToLogDomainList(List<JobLogEntity> jobLogEntityList) {
        List<JobLog> jobLogs = new ArrayList<>();
        for (JobLogEntity jobLogEntity : jobLogEntityList) {
            jobLogs.add(convertToLogDomain(jobLogEntity));
        }
        return jobLogs;
    }

    public static List<JobLogEntity> convertToLogEntityList(List<JobLog> jobLogList) {
        List<JobLogEntity> jobLogEntities = new ArrayList<>();
        for (JobLog jobLog : jobLogList) {
            jobLogEntities.add(convertToLogEntity(jobLog));
        }
        return jobLogEntities;
    }
}
