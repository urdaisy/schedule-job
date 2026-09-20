package com.schedule.job.admin.config;

import com.schedule.job.admin.entity.JobInfoEntity;
import com.schedule.job.admin.job.JobInfo;
import com.schedule.job.admin.repository.JobInfoRepository;
import com.schedule.job.alarm.domain.JobInfoProvider;
import com.schedule.job.common.infra.orm.Entity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 任务信息提供者实现
 * 用于alarm模块获取任务信息，解耦对admin模块的直接依赖
 */
@Slf4j
@Component
public class JobInfoProviderImpl implements JobInfoProvider {
    
    @Autowired
    private JobInfoRepository jobInfoRepository;
    
    @Override
    public String getJobName(Long jobId) {
        try {
            JobInfoEntity jobInfo = jobInfoRepository.findById(jobId);
            return jobInfo != null ? jobInfo.getJobName() : null;
        } catch (Exception e) {
            log.error("获取任务名称失败, jobId: {}", jobId, e);
            return null;
        }
    }
    
    @Override
    public String getJobGroup(Long jobId) {
        try {
            JobInfoEntity jobInfo = jobInfoRepository.findById(jobId);
            return jobInfo != null ? jobInfo.getJobGroup() : null;
        } catch (Exception e) {
            log.error("获取任务组失败, jobId: {}", jobId, e);
            return null;
        }
    }
}
