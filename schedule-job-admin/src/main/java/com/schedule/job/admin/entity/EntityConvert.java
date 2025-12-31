package com.schedule.job.admin.entity;

import com.schedule.job.admin.job.JobInfo;

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
}
