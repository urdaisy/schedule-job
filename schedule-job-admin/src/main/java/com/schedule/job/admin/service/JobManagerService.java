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
            jobDataMap.put("jobName", jobInfo.getJobName());
            jobDataMap.put("jobParam", jobInfo.getJobParam());
            jobDataMap.put("jobId", jobInfo.getId());

            // 6. 构建Cron触发器Trigger
            CronTrigger trigger = TriggerBuilder.newTrigger()
                    .withIdentity(jobInfo.getJobName() + "_trigger", jobInfo.getJobGroup())
                    .withSchedule(CronScheduleBuilder.cronSchedule(jobInfo.getCronExpression()))
                    .startNow()
                    .build();

            try {
                // 7. 注册任务到调度器
                scheduler.scheduleJob(jobDetail, trigger);
                if (jobInfo.getStatus() == JobStatus.PAUSED.getCode()) {
                    pauseJob(jobInfo.getJobName(), jobInfo.getJobGroup());
                    log.info("任务已暂停: {}", jobInfo.getId());
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
     * 更新任务
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateJob(JobInfo jobInfo) throws BusinessException, SchedulerException{
        String redisKey = "job:update:" + jobInfo.getId();
        try {
            // 分布式锁：防止并发更新
            if (!redissonLockUtil.tryLock(redisKey, 5, 30, TimeUnit.SECONDS)) {
                throw new BusinessException(BusinessExceptionCode.JOB_UPDATE_FAILED, "任务更新中，请稍后重试");
            }
            // 基础校验逻辑
            JobInfo oldJob = jobInfoRepository.findByEntity(jobInfo.getId());
            String oldJobClassName = oldJob.getJobClassName(); // 保存原有的jobClassName用于后续比较
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
            oldJob.setJobClassName(jobClassName);
            oldJob.setCronExpression(jobInfo.getCronExpression());
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
                    updateJobCron(oldJob.getJobName(), oldJob.getJobGroup(), jobInfo.getCronExpression());
                }
                log.info("任务更新成功: jobId={}, newJobName={}", updatedJob.getId(), updatedJob.getJobName());
            } catch (SchedulerException e) {
                log.error("更新Quartz任务失败，触发事务回滚: jobId={}", updatedJob.getId(), e);
                throw e;
            }
        } finally {
            redissonLockUtil.unlock(redisKey);
        }
    }

    /**
     * 更新任务 cron 表达式
     */
    public void updateJobCron(String jobName, String jobGroup, String cronExpression) throws SchedulerException, BusinessException {
        TriggerKey triggerKey = TriggerKey.triggerKey(jobName + "_trigger", jobGroup);
        CronTrigger trigger = (CronTrigger) scheduler.getTrigger(triggerKey);

        if (trigger == null) {
            throw new BusinessException(BusinessExceptionCode.JOB_TRIGGER_NOT_FOUND, "任务触发器不存在");
        }

        String oldCron = trigger.getCronExpression();
        if (!oldCron.equalsIgnoreCase(cronExpression)) {
            CronScheduleBuilder scheduleBuilder = CronScheduleBuilder.cronSchedule(cronExpression);
            trigger = trigger.getTriggerBuilder().withSchedule(scheduleBuilder).build();
            scheduler.rescheduleJob(triggerKey, trigger);
            log.info("任务cron更新成功: jobName={}, oldCron={}, newCron={}", jobName, oldCron, cronExpression);
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