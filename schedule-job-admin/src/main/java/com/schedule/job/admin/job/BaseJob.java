package com.schedule.job.admin.job;

import com.schedule.job.admin.repository.JobLogRepository;
import com.schedule.job.admin.repository.JobInfoRepository;
import com.schedule.job.admin.service.JobManagerService;
import com.schedule.job.alarm.service.AlertService;
import com.schedule.job.admin.config.ApplicationContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.quartz.JobExecutionContext;
import org.springframework.context.ApplicationContext;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 定时任务抽象基类，统一处理日志、参数解析、异常封装、重试机制
 */
@Slf4j
public abstract class BaseJob implements Job {
    protected JobLogRepository jobLogRepository;
    protected AlertService alertService;
    protected JobInfoRepository jobInfoRepository;
    protected JobManagerService jobManagerService;
    
    /**
     * 无参构造函数，供 Quartz 反射创建实例使用
     * JobLogRepository 和 AlertService 通过 com.schedule.job.admin.config.ApplicationContextHolder 延迟获取
     */
    public BaseJob() {}
    
    /**
     * 获取 JobLogRepository（延迟初始化）
     * @return JobLogRepository 实例
     */
    protected JobLogRepository getJobLogRepository() {
        if (jobLogRepository == null) {
            try {
                ApplicationContext context = ApplicationContextHolder.getApplicationContext();
                jobLogRepository = context.getBean(JobLogRepository.class);
                log.debug("通过 com.schedule.job.admin.config.ApplicationContextHolder 获取 JobLogRepository 成功");
            } catch (Exception e) {
                log.error("获取 JobLogRepository 失败，任务日志将无法保存", e);
            }
        }
        return jobLogRepository;
    }
    
    /**
     * 获取 AlertService（延迟初始化）
     * @return AlertService 实例
     */
    protected AlertService getAlertService() {
        if (alertService == null) {
            try {
                ApplicationContext context = ApplicationContextHolder.getApplicationContext();
                alertService = context.getBean(AlertService.class);
                log.debug("通过 com.schedule.job.admin.config.ApplicationContextHolder 获取 AlertService 成功");
            } catch (Exception e) {
                log.warn("获取 AlertService 失败，告警功能将不可用", e);
            }
        }
        return alertService;
    }
    
    /**
     * 获取 JobInfoRepository（延迟初始化）
     * @return JobInfoRepository 实例
     */
    protected JobInfoRepository getJobInfoRepository() {
        if (jobInfoRepository == null) {
            try {
                ApplicationContext context = ApplicationContextHolder.getApplicationContext();
                jobInfoRepository = context.getBean(JobInfoRepository.class);
                log.debug("通过 com.schedule.job.admin.config.ApplicationContextHolder 获取 JobInfoRepository 成功");
            } catch (Exception e) {
                log.warn("获取 JobInfoRepository 失败，无法通过任务名称查询任务ID", e);
            }
        }
        return jobInfoRepository;
    }
    
    /**
     * 获取 JobManagerService（延迟初始化）
     * @return JobManagerService 实例
     */
    protected JobManagerService getJobManagerService() {
        if (jobManagerService == null) {
            try {
                ApplicationContext context = ApplicationContextHolder.getApplicationContext();
                jobManagerService = context.getBean(JobManagerService.class);
                log.debug("通过 com.schedule.job.admin.config.ApplicationContextHolder 获取 JobManagerService 成功");
            } catch (Exception e) {
                log.warn("获取 JobManagerService 失败，无法暂停任务", e);
            }
        }
        return jobManagerService;
    }
    
    /**
     * 默认最大重试次数
     */
    private static final int DEFAULT_RETRY_COUNT = 3;
    
    /**
     * 默认重试间隔（秒）
     */
    private static final int DEFAULT_RETRY_INTERVAL = 60;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        // 获取任务参数（JobDataMap）
        JobDataMap dataMap = context.getMergedJobDataMap();
        String jobName = dataMap.getString("jobName");
        String jobParam = dataMap.getString("jobParam");
        
        // 获取jobId，如果不存在则尝试从JobKey获取
        Long jobId = null;
        if (dataMap.containsKey("jobId")) {
            Object jobIdObj = dataMap.get("jobId");
            if (jobIdObj instanceof Number) {
                jobId = ((Number) jobIdObj).longValue();
            }
        }
        
        // 获取jobGroup，如果不存在则从JobKey获取
        String jobGroup = dataMap.getString("jobGroup");
        if (jobGroup == null || jobGroup.isEmpty()) {
            jobGroup = context.getJobDetail().getKey().getGroup();
        }

        // 如果jobId仍然为null，尝试通过jobName和jobGroup从数据库查询
        if (jobId == null && jobName != null && jobGroup != null) {
            try {
                JobInfoRepository repository = getJobInfoRepository();
                if (repository != null) {
                    Optional<JobInfo> jobInfo = repository.findByJobNameAndJobGroup(jobName, jobGroup);
                    if (jobInfo.isPresent()) {
                        jobId = jobInfo.get().getId();
                        log.debug("通过任务名称和组名查询到jobId: jobName={}, jobGroup={}, jobId={}",
                                jobName, jobGroup, jobId);
                    } else {
                        log.warn("无法通过任务名称和组名查询到jobId，任务可能已被删除: jobName={}, jobGroup={}",
                                jobName, jobGroup);
                    }
                }
            } catch (Exception e) {
                log.warn("通过任务名称和组名查询jobId失败: jobName={}, jobGroup={}",
                        jobName, jobGroup, e);
            }
        }
        
        // 获取当前重试次数（从JobDataMap中获取，如果没有则默认为0）
        int currentRetryCount = 0;
        if (dataMap.containsKey("currentRetryCount")) {
            Object retryCountObj = dataMap.get("currentRetryCount");
            if (retryCountObj instanceof Number) {
                currentRetryCount = ((Number) retryCountObj).intValue();
            }
        }
        
        // 获取配置的最大重试次数（从JobDataMap中获取，如果没有则使用默认值）
        int maxRetryCount = DEFAULT_RETRY_COUNT;
        if (dataMap.containsKey("maxRetryCount")) {
            Object maxRetryObj = dataMap.get("maxRetryCount");
            if (maxRetryObj instanceof Number) {
                int maxRetry = ((Number) maxRetryObj).intValue();
                if (maxRetry >= 0) {
                    maxRetryCount = maxRetry;
                }
            }
        }
        
        // 获取重试间隔（秒）
        int retryInterval = DEFAULT_RETRY_INTERVAL;
        if (dataMap.containsKey("retryInterval")) {
            Object intervalObj = dataMap.get("retryInterval");
            if (intervalObj instanceof Number) {
                int interval = ((Number) intervalObj).intValue();
                if (interval > 0) {
                    retryInterval = interval;
                }
            }
        }
        
        // 记录执行开始时间
        LocalDateTime triggerTime = LocalDateTime.now();
        LocalDateTime executeTime = triggerTime;
        long startTime = System.currentTimeMillis();
        
        log.info("任务[{}]开始执行，参数：{}，当前重试次数：{}/{}", 
                jobName, jobParam, currentRetryCount, maxRetryCount);

        try {
            // 调用子类实现的业务逻辑
            executeInternal(jobName, jobParam);
            
            // 计算执行耗时
            long duration = System.currentTimeMillis() - startTime;
            executeTime = LocalDateTime.now();
            
            // 记录成功日志
            saveJobLog(jobId, triggerTime, executeTime, duration, 0, null, currentRetryCount);
            
            log.info("任务[{}]执行成功，耗时：{}ms", jobName, duration);

        } catch (Exception e) {
            // 计算执行耗时
            long duration = System.currentTimeMillis() - startTime;
            executeTime = LocalDateTime.now();
            String errorMsg = e.getMessage();
            if (errorMsg == null || errorMsg.isEmpty()) {
                errorMsg = e.getClass().getName();
            }
            
            log.error("任务[{}]执行失败，当前重试次数：{}/{}，错误信息：{}", 
                    jobName, currentRetryCount, maxRetryCount, errorMsg, e);
            
            // 检查是否还有重试机会
            if (currentRetryCount < maxRetryCount) {
                // 记录失败日志（标记为重试中）
                saveJobLog(jobId, triggerTime, executeTime, duration, 1, 
                        "执行失败，准备重试：" + errorMsg, currentRetryCount);
                
                try {
                    // 获取调度器
                    Scheduler scheduler = context.getScheduler();
                    
                    // 准备下一次重试的参数
                    int nextRetryCount = currentRetryCount + 1;
                    
                    // 调用子类实现的重试调度方法
                    scheduleRetryJob(scheduler, jobName, jobGroup, retryInterval, nextRetryCount, maxRetryCount, jobId, jobParam);
                    
                    log.info("任务[{}]已安排重试，第{}次重试将在{}秒后执行", 
                            jobName, nextRetryCount, retryInterval);
                    
                } catch (Exception retryException) {
                    log.error("任务[{}]安排重试失败", jobName, retryException);
                    // 记录最终失败日志
                    Long jobLogId = saveJobLog(jobId, triggerTime, executeTime, duration, 1,
                            "执行失败且重试调度失败：" + errorMsg + "，重试调度异常：" + retryException.getMessage(), 
                            currentRetryCount);
                    // 触发告警
                    if (jobId != null && jobLogId != null) {
                        triggerAlert(jobId, jobLogId, errorMsg + "，重试调度异常：" + retryException.getMessage());
                    }
                }
            } else {
                // 已达到最大重试次数，记录最终失败日志
                Long jobLogId = saveJobLog(jobId, triggerTime, executeTime, duration, 1, 
                        "执行失败，已达到最大重试次数：" + errorMsg, currentRetryCount);
                log.error("任务[{}]执行失败，已达到最大重试次数{}，不再重试", jobName, maxRetryCount);
                
                // 触发告警（仅在最终失败时触发，避免重试过程中重复告警）
                if (jobId != null && jobLogId != null) {
                    triggerAlert(jobId, jobLogId, errorMsg);
                }
                
                // 达到最大重试次数后，标记任务为失败状态，并暂停调度
                try {
                    JobManagerService service = getJobManagerService();
                    if (service != null && jobName != null && jobGroup != null) {
                        service.markJobAsFailed(jobName, jobGroup);
                        log.warn("任务[{}]已达到最大重试次数，已标记为失败状态并暂停调度", jobName);
                    }
                } catch (Exception markFailedException) {
                    log.error("标记任务为失败状态失败，任务[{}]可能继续被调度", jobName, markFailedException);
                }
            }
            
            // 抛出异常：false表示不重新执行，true则Quartz会重试
            // 这里设置为false，因为我们自己管理重试逻辑
            throw new JobExecutionException("任务执行失败", e, false);
        }
    }

    /**
     * 子类实现具体的业务逻辑
     * @param jobName 任务名称
     * @param jobParam 任务参数
     * @throws Exception 执行异常
     */
    protected abstract void executeInternal(String jobName, String jobParam) throws Exception;

    /**
     * 子类实现重试任务的调度逻辑
     * @param scheduler Quartz调度器
     * @param jobName 任务名称
     * @param jobGroup 任务组
     * @param retryInterval 重试间隔（秒）
     * @param nextRetryCount 下一次重试次数
     * @param maxRetryCount 最大重试次数
     * @param jobId 任务ID
     * @param jobParam 任务参数
     * @throws Exception 调度异常
     */
    protected abstract void scheduleRetryJob(Scheduler scheduler, String jobName, String jobGroup,
                                           int retryInterval, int nextRetryCount, int maxRetryCount,
                                           Long jobId, String jobParam) throws Exception;

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
    protected Long saveJobLog(Long jobId, LocalDateTime triggerTime, LocalDateTime executeTime, 
                             long duration, int status, String errorMsg, int retryCount) {
        // 如果jobId为null，无法保存日志（数据库约束），记录警告并返回null
        if (jobId == null) {
            log.warn("jobId为null，无法保存任务执行日志。这通常发生在任务已被删除或JobDataMap中未设置jobId的情况。");
            return null;
        }
        
        try {
            JobLogRepository repository = getJobLogRepository();
            if (repository == null) {
                log.warn("JobLogRepository未初始化，无法保存日志，jobId={}", jobId);
                return null;
            }
            return repository.saveJobLog(jobId, triggerTime, executeTime, duration, status, errorMsg, retryCount);
        } catch (Exception e) {
            log.error("保存任务执行日志失败，jobId={}", jobId, e);
            return null;
        }
    }
    
    /**
     * 触发告警
     * @param jobId 任务ID
     * @param jobLogId 任务日志ID
     * @param errorMsg 错误信息
     */
    protected void triggerAlert(Long jobId, Long jobLogId, String errorMsg) {
        try {
            AlertService service = getAlertService();
            if (service == null) {
                log.debug("AlertService未初始化，无法触发告警，jobId={}", jobId);
                return;
            }
            service.triggerAlert(jobId, jobLogId, errorMsg);
        } catch (Exception e) {
            log.error("触发告警失败，jobId={}, jobLogId={}", jobId, jobLogId, e);
        }
    }
}