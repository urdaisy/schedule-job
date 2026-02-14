package com.schedule.job.admin.job;

import com.schedule.job.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;

import java.util.Date;

import static com.schedule.job.common.enums.BusinessExceptionCode.JOB_RETRY_FAILED;

/**
 * 通用任务（默认任务）
 * 如果未指定具体的Job类，使用此任务
 */
@Slf4j
public class DefaultJob extends BaseJob {
    @Override
    protected void executeInternal(String jobName, String jobParam) throws Exception {
        log.info("执行通用任务：{}，参数：{}", jobName, jobParam);
        
        // 简单的打印任务
        System.out.println("[" + jobName + "] 执行任务，参数：" + jobParam);
        log.info("任务[{}]执行完成", jobName);
        
        // 模拟一些处理时间
        Thread.sleep(100);
    }

    @Override
    protected void scheduleRetryJob(Scheduler scheduler, String jobName, String jobGroup, 
                                   int retryInterval, int nextRetryCount, int maxRetryCount,
                                   Long jobId, String jobParam) throws Exception {
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

        // 获取原始JobDetail，以便复制JobDataMap
        JobDetail originalJobDetail = scheduler.getJobDetail(jobKey);
        if (originalJobDetail == null) {
            log.error("无法获取原始任务详情：jobName={}, jobGroup={}", jobName, jobGroup);
            throw new BusinessException(JOB_RETRY_FAILED, "无法获取原始任务详情");
        }
        
        // 创建新的JobDataMap，包含重试信息
        JobDataMap retryDataMap = new JobDataMap(originalJobDetail.getJobDataMap());
        retryDataMap.put("currentRetryCount", nextRetryCount);
        retryDataMap.put("maxRetryCount", maxRetryCount);
        retryDataMap.put("retryInterval", retryInterval);

        Date startTime = new Date(System.currentTimeMillis() + retryInterval * 1000L);

        SimpleTrigger retryTrigger = TriggerBuilder.newTrigger()
                .withIdentity(triggerKey)
                .forJob(jobKey)
                .usingJobData(retryDataMap)
                .startAt(startTime)
                .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                        .withRepeatCount(0)
                        .withIntervalInMilliseconds(0))
                .build();

        scheduler.scheduleJob(retryTrigger);

        log.info("创建重试任务成功：jobName={}, jobGroup={}, 第{}次重试将在{}秒后执行",
                jobName, jobGroup, nextRetryCount, retryInterval);
    }
}
