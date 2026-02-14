package com.schedule.job.alarm.domain;

/**
 * 任务信息提供者接口
 * 用于解耦 alarm 模块对 admin 模块的依赖
 */
public interface JobInfoProvider {
    /**
     * 获取任务名称
     */
    String getJobName(Long jobId);
    
    /**
     * 获取任务组
     */
    String getJobGroup(Long jobId);
}
