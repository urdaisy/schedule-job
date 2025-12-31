package com.schedule.job.admin.job;

import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobDataMap;

/**
 * 定时任务抽象基类，统一处理日志、参数解析、异常封装
 */
@Slf4j
public abstract class BaseJob implements Job {
    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        // 获取任务参数（JobDataMap）
        JobDataMap dataMap = context.getMergedJobDataMap();
        String jobName = dataMap.getString("jobName");
        String jobParam = dataMap.getString("jobParam");

        log.info("任务[{}]开始执行，参数：{}", jobName, jobParam);

        try {
            // 调用子类实现的业务逻辑
            executeInternal(jobName, jobParam);
            log.info("任务[{}]执行成功", jobName);
        } catch (Exception e) {
            log.error("任务[{}]执行失败", jobName, e);
            // 抛出异常：false表示不重新执行，true则Quartz会重试
            throw new JobExecutionException("任务执行失败", e, false);
        }
    }

    /**
     * 子类需实现的具体业务逻辑
     * @param jobName 任务名称
     * @param jobParam 任务自定义参数
     * @throws Exception 业务异常（会被上层捕获并封装为JobExecutionException）
     */
    protected abstract void executeInternal(String jobName, String jobParam) throws Exception;
}