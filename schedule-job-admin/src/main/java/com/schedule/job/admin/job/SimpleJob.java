package com.schedule.job.admin.job;

import com.schedule.job.admin.repository.JobLogRepository;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Scheduler;
import org.quartz.SimpleTrigger;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.JobKey;

import java.util.Date;

import static com.schedule.job.common.enums.BusinessExceptionCode.JOB_RETRY_FAILED;
import com.schedule.job.common.exception.BusinessException;

/**
 * 简单任务
 * 用于执行简单的打印、日志记录等任务
 * 参数格式：任意字符串，会直接打印
 */
@Slf4j
public class SimpleJob extends BaseJob {

    public SimpleJob(JobLogRepository jobLogRepository) {
        super(jobLogRepository);
    }

    @Override
    protected void executeInternal(String jobName, String jobParam) throws Exception {
        log.info("执行简单任务：{}，参数：{}", jobName, jobParam);
        
        // 简单的打印任务
        System.out.println("[" + jobName + "] 执行任务，参数：" + jobParam);
        log.info("任务[{}]执行完成", jobName);
        
        // 模拟一些处理时间
        Thread.sleep(100);
    }

    @Override
    protected void scheduleRetryJob(Scheduler scheduler, String jobName, String jobGroup, int retryInterval) throws Exception {
        if (retryInterval <= 0) {
            retryInterval = 60;
            log.warn("重试间隔无效，使用默认值60秒");
        }
        
        String retryTriggerKey = jobName + "_retry_" + System.currentTimeMillis();
        TriggerKey triggerKey = TriggerKey.triggerKey(retryTriggerKey, jobGroup + "_retry");
        JobKey jobKey = JobKey.jobKey(jobName, jobGroup);
        
        if (!scheduler.checkExists(jobKey)) {
            log.error("原始任务不存在，无法创建重试任务：jobName={}, jobGroup={}", jobName, jobGroup);
            throw new BusinessException(JOB_RETRY_FAILED, "原始任务不存在，无法创建重试任务");
        }
        
        Date startTime = new Date(System.currentTimeMillis() + retryInterval * 1000L);
        
        SimpleTrigger retryTrigger = TriggerBuilder.newTrigger()
                .withIdentity(triggerKey)
                .forJob(jobKey)
                .startAt(startTime)
                .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                        .withRepeatCount(0)
                        .withIntervalInMilliseconds(0))
                .build();
        
        scheduler.scheduleJob(retryTrigger);
        
        log.info("创建简单任务重试成功：jobName={}, jobGroup={}, 将在{}秒后执行", 
                jobName, jobGroup, retryInterval);
    }
}
