package com.schedule.job.admin.service;

import com.schedule.job.admin.job.JobInfo;
import com.schedule.job.admin.repository.JobInfoRepository;
import com.schedule.job.admin.redis.RedissonLockUtil;
import com.schedule.job.common.enums.BusinessExceptionCode;
import com.schedule.job.common.enums.JobStatus;
import com.schedule.job.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

 import java.text.ParseException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static com.schedule.job.common.enums.BusinessExceptionCode.JOB_LOAD_FAILED;

@Service
@Slf4j
public class JobManagerService {
    @Autowired
    private Scheduler scheduler;

    @Autowired
    private JobInfoRepository jobInfoRepository;

    @Autowired
    private RedissonLockUtil redissonLockUtil;

    /**
     * 添加任务，使用事务管理，在数据库操作失败时能够回滚
     */
    @Transactional(rollbackFor = Exception.class)
    public void addJob(JobInfo jobInfo) throws BusinessException, SchedulerException {
        // 使用分布式锁，避免高并发场景，重复创建任务
        String redisKey = "job:add:" + jobInfo.getJobName() + ":" + jobInfo.getJobGroup();
        try {
            if (!redissonLockUtil.tryLock(redisKey, 5, 30, TimeUnit.SECONDS)) {
                throw new BusinessException(BusinessExceptionCode.JOB_INSERT_FAILED, "任务创建中，请稍后重试");
            }
            // 1. 校验任务是否存在，避免quartz重复注册
            Optional<JobInfo> existJob = jobInfoRepository.findByJobNameAndJobGroup(jobInfo.getJobName(), jobInfo.getJobGroup());
            if (existJob.isPresent()) {
                throw new BusinessException(BusinessExceptionCode.JOB_INSERT_FAILED, "任务名+任务组已存在，无法重复创建");
            }
            // 2. 设置默认任务类（如果未指定）
            if (jobInfo.getJobClassName() == null || jobInfo.getJobClassName().trim().isEmpty()) {
                jobInfo.setJobClassName("com.schedule.job.admin.job.DefaultJob");
                log.info("未指定任务类，使用默认任务类：DefaultJob");
            }
            // 3. 保存任务信息到数据库
            jobInfo.setStartTime(LocalDateTime.now());
            jobInfo.setUpdateTime(LocalDateTime.now());
            JobInfo savedJobInfo = jobInfoRepository.saveByEntity(jobInfo);

            Class<? extends Job> clazz = getJobClass(jobInfo.getJobClassName());
            // 4. 构建JobDetail
            JobDetail jobDetail = JobBuilder.newJob(clazz)
                    .withIdentity(jobInfo.getJobName(), jobInfo.getJobGroup())
                    .withDescription(jobInfo.getDescription())
                    .storeDurably() // 无触发器时也保留任务
                    .build();

            // 5. 设置任务参数
            JobDataMap jobDataMap = jobDetail.getJobDataMap();
            jobDataMap.put("jobName", savedJobInfo.getJobName());
            jobDataMap.put("jobParam", savedJobInfo.getJobParam());
            jobDataMap.put("jobId", savedJobInfo.getId()); // 使用保存后返回的对象，确保ID已设置
            jobDataMap.put("jobGroup", savedJobInfo.getJobGroup());
            // 设置重试相关参数
            jobDataMap.put("currentRetryCount", 0); // 初始重试次数为0
            if (savedJobInfo.getRetryCount() != null && savedJobInfo.getRetryCount() > 0) {
                jobDataMap.put("maxRetryCount", savedJobInfo.getRetryCount());
            } else {
                jobDataMap.put("maxRetryCount", 3); // 默认最大重试3次
            }
            jobDataMap.put("retryInterval", 60); // 默认重试间隔60秒

            // 6. 构建Cron触发器Trigger
            CronTrigger trigger = TriggerBuilder.newTrigger()
                    .withIdentity(savedJobInfo.getJobName() + "_trigger", savedJobInfo.getJobGroup())
                    .withSchedule(CronScheduleBuilder.cronSchedule(savedJobInfo.getCronExpression()))
                    .startNow()
                    .build();

            try {
                // 7. 注册任务到调度器
                scheduler.scheduleJob(jobDetail, trigger);
                if (savedJobInfo.getStatus() == JobStatus.PAUSED.getCode()) {
                    pauseJob(savedJobInfo.getJobName(), savedJobInfo.getJobGroup());
                    log.info("任务已暂停: {}", savedJobInfo.getId());
                }
                log.info("任务创建成功: jobId={}, jobName={}", savedJobInfo.getId(), savedJobInfo.getJobName());
            } catch (SchedulerException e) {
                log.error("注册Quartz任务失败，触发事务回滚: jobName={}", savedJobInfo.getJobName(), e);
                throw e;
            }
        } finally {
            redissonLockUtil.unlock(redisKey);
        }
    }

    /**
     * 暂停任务
     */
    @Transactional(rollbackFor = Exception.class)
    public void pauseJob(String jobName, String jobGroup) throws SchedulerException {
        JobInfo jobInfo = jobInfoRepository.findByJobNameAndJobGroup(jobName, jobGroup)
                .orElseThrow(() -> new BusinessException(BusinessExceptionCode.JOB_NOT_FOUND));
        jobInfo.setStatus(JobStatus.PAUSED.getCode());
        jobInfo.setUpdateTime(LocalDateTime.now());
        jobInfoRepository.saveByEntity(jobInfo);
        // 暂停Quartz任务
        scheduler.pauseJob(JobKey.jobKey(jobName, jobGroup));
        log.info("任务暂停成功: jobName={}, jobGroup={}", jobName, jobGroup);
    }

    /**
     * 恢复任务
     */
    @Transactional(rollbackFor = Exception.class)
    public void resumeJob(String jobName, String jobGroup) throws SchedulerException {
        JobInfo jobInfo = jobInfoRepository.findByJobNameAndJobGroup(jobName, jobGroup)
                .orElseThrow(() -> new BusinessException(BusinessExceptionCode.JOB_NOT_FOUND));
        jobInfo.setStatus(JobStatus.RUNNING.getCode());
        jobInfo.setUpdateTime(LocalDateTime.now());
        jobInfoRepository.saveByEntity(jobInfo);
        scheduler.resumeJob(JobKey.jobKey(jobName, jobGroup));
        log.info("任务恢复成功: jobName={}, jobGroup={}", jobName, jobGroup);
    }

    /**
     * 标记任务为失败状态（达到最大重试次数后）
     */
    @Transactional(rollbackFor = Exception.class)
    public void markJobAsFailed(String jobName, String jobGroup) throws SchedulerException {
        JobInfo jobInfo = jobInfoRepository.findByJobNameAndJobGroup(jobName, jobGroup)
                .orElseThrow(() -> new BusinessException(BusinessExceptionCode.JOB_NOT_FOUND));
        jobInfo.setStatus(JobStatus.FAILED.getCode());
        jobInfo.setUpdateTime(LocalDateTime.now());
        jobInfoRepository.saveByEntity(jobInfo);
        // 暂停Quartz任务，避免继续调度执行
        scheduler.pauseJob(JobKey.jobKey(jobName, jobGroup));
        log.info("任务已标记为失败状态: jobName={}, jobGroup={}", jobName, jobGroup);
    }

    /**
     * 更新任务
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateJob(JobInfo jobInfo) throws BusinessException, SchedulerException{
        String redisKey = "job:update:" + jobInfo.getId();
        boolean lockAcquired = false;
        try {
            // 分布式锁：防止并发更新
            // waitTime: 等待获取锁的时间（5秒）
            // leaseTime: 锁的持有时间（60秒，足够完成更新操作）
            lockAcquired = redissonLockUtil.tryLock(redisKey, 5, 60, TimeUnit.SECONDS);
            if (!lockAcquired) {
                log.warn("获取任务更新锁失败，可能正在被其他线程更新：jobId={}", jobInfo.getId());
                throw new BusinessException(BusinessExceptionCode.JOB_UPDATE_FAILED, "任务更新中，请稍后重试");
            }
            // 基础校验逻辑
            JobInfo oldJob = jobInfoRepository.findByEntity(jobInfo.getId());
            String oldJobClassName = oldJob.getJobClassName(); // 保存原有的jobClassName用于后续比较
            // 保存原始的jobName和jobGroup，用于后续Quartz操作
            String originalJobName = oldJob.getJobName();
            String originalJobGroup = oldJob.getJobGroup();
            boolean isJobKeyChanged = !oldJob.getJobName().equals(jobInfo.getJobName()) || !oldJob.getJobGroup().equals(jobInfo.getJobGroup());
            if (isJobKeyChanged) {
                Optional<JobInfo> existJob = jobInfoRepository.findByJobNameAndJobGroup(jobInfo.getJobName(), jobInfo.getJobGroup());
                if (existJob.isPresent() && !existJob.get().getId().equals(jobInfo.getId())) {
                    throw new BusinessException(BusinessExceptionCode.JOB_UPDATE_FAILED,  "新的任务名+任务组已存在");
                }
            }
            // 更新cron表达式
            if (jobInfo.getCronExpression() != null && !jobInfo.getCronExpression().equals(oldJob.getCronExpression())) {
                try {
                    CronExpression.validateExpression(jobInfo.getCronExpression());
                } catch (ParseException e) {
                    throw new BusinessException(BusinessExceptionCode.JOB_PARAM_FAILED, "Cron表达式不合法: " + jobInfo.getCronExpression());
                }
            }
            // 补全jobClassName（如果未指定或为空，使用原有值或默认值）
            String jobClassName = jobInfo.getJobClassName();
            if (jobClassName == null || jobClassName.trim().isEmpty()) {
                jobClassName = oldJobClassName; // 保持原有值
                if (jobClassName == null || jobClassName.trim().isEmpty()) {
                    jobClassName = "com.schedule.job.admin.job.DefaultJob"; // 如果原有值也为空，使用默认值
                    log.info("更新任务时未指定任务类，使用默认任务类：DefaultJob");
                } else {
                    log.info("更新任务时未指定任务类，保持原有任务类：{}", jobClassName);
                }
            }
            // 更新数据库字段
            oldJob.setJobName(jobInfo.getJobName());
            oldJob.setJobGroup(jobInfo.getJobGroup());
            oldJob.setDescription(jobInfo.getDescription());
            oldJob.setJobParam(jobInfo.getJobParam());
            oldJob.setJobClassName(jobClassName); // 更新任务类
            oldJob.setCronExpression(jobInfo.getCronExpression()); // 更新cron表达式
            oldJob.setUpdateTime(LocalDateTime.now());
            JobInfo updatedJob = jobInfoRepository.saveByEntity(oldJob);

            try {
                // 如果key(jobName和jobGroup)都有变化，或者jobClassName有变化，则需要重新注册
                boolean isJobClassNameChanged = !jobClassName.equals(oldJobClassName);
                if (isJobKeyChanged || isJobClassNameChanged) {
                    if (isJobKeyChanged) {
                        scheduler.deleteJob(JobKey.jobKey(oldJob.getJobName(), oldJob.getJobGroup()));
                    } else {
                        // 如果只是jobClassName变化，需要删除旧任务并重新注册
                        scheduler.deleteJob(JobKey.jobKey(updatedJob.getJobName(), updatedJob.getJobGroup()));
                    }
                    Class<? extends Job> clazz = getJobClass(jobClassName);
                    JobDetail newJobDetail =  JobBuilder.newJob(clazz)
                            .withIdentity(updatedJob.getJobName(), updatedJob.getJobGroup())
                            .withDescription(updatedJob.getDescription())
                            .storeDurably() // 无触发器时也保留任务
                            .build();
                    // 设置任务参数（包括重试配置）
                    JobDataMap newJobDataMap = newJobDetail.getJobDataMap();
                    newJobDataMap.put("jobName", updatedJob.getJobName());
                    newJobDataMap.put("jobParam", updatedJob.getJobParam());
                    newJobDataMap.put("jobId", updatedJob.getId());
                    newJobDataMap.put("jobGroup", updatedJob.getJobGroup());
                    newJobDataMap.put("currentRetryCount", 0);
                    if (updatedJob.getRetryCount() != null && updatedJob.getRetryCount() > 0) {
                        newJobDataMap.put("maxRetryCount", updatedJob.getRetryCount());
                    } else {
                        newJobDataMap.put("maxRetryCount", 3);
                    }
                    newJobDataMap.put("retryInterval", 60);
                    CronTrigger newTrigger = TriggerBuilder.newTrigger()
                            .withIdentity(updatedJob.getJobName() + "_trigger", updatedJob.getJobGroup())
                            .withSchedule(CronScheduleBuilder.cronSchedule(updatedJob.getCronExpression()))
                            .startNow()
                            .build();
                    scheduler.scheduleJob(newJobDetail, newTrigger);
                    // 保持原始状态不变
                    if (updatedJob.getStatus() == JobStatus.PAUSED.getCode()) {
                        pauseJob(updatedJob.getJobName(), updatedJob.getJobGroup());
                    }
                } else {
                    // 更新cron表达式
                    // 注意：如果jobName或jobGroup没有变化，使用原始的jobName和jobGroup来查找触发器
                    // 因为Quartz中的触发器是用原始名称注册的
                    updateJobCron(originalJobName, originalJobGroup, jobInfo.getCronExpression());
                }
                log.info("任务更新成功: jobId={}, newJobName={}", updatedJob.getId(), updatedJob.getJobName());
            } catch (SchedulerException e) {
                log.error("更新Quartz任务失败，触发事务回滚: jobId={}", updatedJob.getId(), e);
                throw e;
            }
        } finally {
            // 确保锁被释放（只有成功获取锁的情况下才释放）
            if (lockAcquired) {
                redissonLockUtil.unlock(redisKey);
            }
        }
    }

    /**
     * 更新任务 cron 表达式
     */
    public void updateJobCron(String jobName, String jobGroup, String cronExpression) throws SchedulerException, BusinessException {
        TriggerKey triggerKey = TriggerKey.triggerKey(jobName + "_trigger", jobGroup);
        CronTrigger trigger = (CronTrigger) scheduler.getTrigger(triggerKey);

        if (trigger == null) {
            // 检查Job是否存在
            JobKey jobKey = JobKey.jobKey(jobName, jobGroup);
            if (!scheduler.checkExists(jobKey)) {
                log.error("任务不存在，无法更新cron表达式: jobName={}, jobGroup={}", jobName, jobGroup);
                throw new BusinessException(BusinessExceptionCode.JOB_NOT_FOUND, 
                        "任务不存在，无法更新cron表达式。请先创建任务或检查任务名称和组名是否正确");
            }
            // Job存在但触发器不存在，可能是触发器被意外删除
            log.warn("任务存在但触发器不存在，尝试重新创建触发器: jobName={}, jobGroup={}", jobName, jobGroup);
            // 重新创建触发器
            JobDetail jobDetail = scheduler.getJobDetail(jobKey);
            if (jobDetail != null) {
                CronTrigger newTrigger = TriggerBuilder.newTrigger()
                        .withIdentity(triggerKey)
                        .forJob(jobKey)
                        .withSchedule(CronScheduleBuilder.cronSchedule(cronExpression))
                        .startNow()
                        .build();
                scheduler.scheduleJob(newTrigger);
                log.info("已重新创建触发器并更新cron表达式: jobName={}, cronExpression={}", jobName, cronExpression);
                return;
            }
            throw new BusinessException(BusinessExceptionCode.JOB_TRIGGER_NOT_FOUND, 
                    "任务触发器不存在。任务名称：" + jobName + "，任务组：" + jobGroup + 
                    "。请检查任务是否正确创建，或联系管理员");
        }

        String oldCron = trigger.getCronExpression();
        if (!oldCron.equalsIgnoreCase(cronExpression)) {
            CronScheduleBuilder scheduleBuilder = CronScheduleBuilder.cronSchedule(cronExpression);
            trigger = trigger.getTriggerBuilder().withSchedule(scheduleBuilder).build();
            scheduler.rescheduleJob(triggerKey, trigger);
            log.info("任务cron更新成功: jobName={}, oldCron={}, newCron={}", jobName, oldCron, cronExpression);
        } else {
            log.debug("任务cron表达式未变化，无需更新: jobName={}, cronExpression={}", jobName, cronExpression);
        }
    }

    /**
     * 删除任务
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteJob(String jobName, String jobGroup) throws SchedulerException, BusinessException {
        String lockKey = "job:delete:" + jobName + ":" + jobGroup;
        try {
            if (!redissonLockUtil.tryLock(lockKey, 5, 30, TimeUnit.SECONDS)) {
                throw new BusinessException(BusinessExceptionCode.JOB_DELETE_FAILED, "任务删除中，请稍后重试");
            }
            // 先删除数据库
            JobInfo jobInfo = jobInfoRepository.findByJobNameAndJobGroup(jobName, jobGroup)
                    .orElseThrow(() -> new BusinessException(BusinessExceptionCode.JOB_NOT_FOUND));
            jobInfoRepository.delete(jobInfo);

            try {
                // 再删除定时任务
                boolean isDeleted = scheduler.deleteJob(JobKey.jobKey(jobName, jobGroup));
                if (isDeleted) {
                    log.info("任务删除成功: jobName={}, jobGroup={}", jobName, jobGroup);
                } else {
                    log.warn("Quartz任务不存在，但数据库记录已删除: jobName={}", jobName);
                }
            } catch (SchedulerException e) {
                log.error("删除Quartz任务失败，触发数据库事务回滚: jobName={}", jobName, e);
                throw e;
            }

        } finally {
            redissonLockUtil.unlock(lockKey);
        }
    }

    /**
     * 立即执行任务，不需要事务管理，无需回滚
     */
    public void runJobNow(String jobName, String jobGroup) throws SchedulerException {
        jobInfoRepository.findByJobNameAndJobGroup(jobName, jobGroup)
                .orElseThrow(() -> new BusinessException(BusinessExceptionCode.JOB_NOT_FOUND));
        JobKey jobKey = JobKey.jobKey(jobName, jobGroup);
        scheduler.triggerJob(jobKey);
        log.info("任务立即执行成功: jobName={}, jobGroup={}", jobName, jobGroup);
    }

    /**
     * 查询任务列表，不需要事务管理，无需回滚
     */
    public List<JobInfo> findAll() {
        return jobInfoRepository.findAllJobInfo();
    }

    /**
     * 根据JobClass执行对应的定时任务
     */
    public Class<? extends Job> getJobClass(String jobClass) {
        // 1. 参数校验
        if (jobClass == null || jobClass.trim().isEmpty()) {
            log.error("任务类全路径不能为空");
            throw new BusinessException(JOB_LOAD_FAILED,"任务类全路径不能为空");
        }
        try {
            // 2. 动态加载类
            Class<?> clazz = Thread.currentThread().getContextClassLoader().loadClass(jobClass.trim());

            // 3. 校验是否为Job的子类/实现类
            if (!Job.class.isAssignableFrom(clazz)) {
                log.error("类[{}]不是Job的子类，无法作为定时任务执行", jobClass);
                throw new BusinessException(JOB_LOAD_FAILED, "类[" + jobClass + "]不是Job的子类，不支持作为定时任务");
            }

            // 4. 类型转换并返回
            Class<? extends Job> jobClazz = clazz.asSubclass(Job.class);
            log.info("成功加载定时任务类：{}", jobClass);
            return jobClazz;

        } catch (ClassNotFoundException e) {
            log.error("未找到指定的任务类：{}", jobClass, e);
            throw new BusinessException(JOB_LOAD_FAILED, "未找到指定的任务类：" + jobClass);
        } catch (Exception e) {
            log.error("加载任务类[{}]失败", jobClass, e);
            throw new BusinessException(JOB_LOAD_FAILED, "加载任务类失败：" + jobClass);
        }
    }
}