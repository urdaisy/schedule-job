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
 * 数据处理任务
 * 用于执行数据清洗、转换、统计等任务，参数格式：operation|sql(可选)
 * 操作类型：
 * - CLEAN: 数据清洗
 * - TRANSFORM: 数据转换
 * - STATISTICS: 数据统计
 */
@Slf4j
public class DataProcessJob extends BaseJob {

    public DataProcessJob(JobLogRepository jobLogRepository) {
        super(jobLogRepository);
    }

    @Override
    protected void executeInternal(String jobName, String jobParam) throws Exception {
        log.info("执行数据处理任务：{}，参数：{}", jobName, jobParam);
        
        // 解析参数
        String[] params = jobParam.split("\\|");
        if (params.length < 1) {
            throw new IllegalArgumentException("数据处理任务参数格式错误，需要：operation|sql(可选)");
        }
        
        String operation = params[0].trim().toUpperCase();
        String sql = params.length > 1 ? params[1].trim() : null;
        
        // 根据操作类型执行不同逻辑
        switch (operation) {
            case "CLEAN":
                executeDataClean(jobName, sql);
                break;
            case "TRANSFORM":
                executeDataTransform(jobName, sql);
                break;
            case "STATISTICS":
                executeStatistics(jobName, sql);
                break;
            default:
                throw new IllegalArgumentException("不支持的操作类型：" + operation);
        }
    }
    
    /**
     * 数据清洗
     */
    private void executeDataClean(String jobName, String sql) throws Exception {
        log.info("执行数据清洗：{}", jobName);
        // TODO: 实现数据清洗逻辑，清理脏数据、去重、格式化等
        Thread.sleep(500); // 模拟处理时间
        log.info("数据清洗完成");
    }
    
    /**
     * 数据转换
     */
    private void executeDataTransform(String jobName, String sql) throws Exception {
        log.info("执行数据转换：{}", jobName);
        // TODO: 实现数据转换逻辑
        Thread.sleep(500);
        log.info("数据转换完成");
    }
    
    /**
     * 数据统计
     */
    private void executeStatistics(String jobName, String sql) throws Exception {
        log.info("执行数据统计：{}", jobName);
        // TODO: 实现数据统计逻辑，计算总数、平均值、最大值等
        Thread.sleep(500);
        log.info("数据统计完成");
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

        log.info("创建数据处理任务重试成功：jobName={}, jobGroup={}, 将在{}秒后执行",
                jobName, jobGroup, retryInterval);
    }
}
