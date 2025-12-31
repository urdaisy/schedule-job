package com.schedule.job.admin.controller;

import com.schedule.job.admin.job.JobInfo;
import com.schedule.job.admin.service.JobManagerService;
import com.schedule.job.common.enums.ResultCode;
import com.schedule.job.common.exception.BusinessException;
import com.schedule.job.common.exception.Result;
import io.micrometer.common.util.StringUtils;
import jakarta.annotation.Resource;
import org.quartz.SchedulerException;
import org.springframework.web.bind.annotation.*;


import java.util.List;

@RestController
@RequestMapping("/api/job")
public class JobController {

    @Resource
    private JobManagerService jobManagerService;

    /**
     * 创建任务
     */
    @PostMapping
    public Result<String> addJob(@RequestBody JobInfo jobInfo) throws BusinessException, SchedulerException {
        // 先校验参数是否有误
        if (jobInfo == null || StringUtils.isEmpty(jobInfo.getJobName()) || StringUtils.isEmpty(jobInfo.getJobGroup())) {
            return Result.error(ResultCode.PARAM_ERROR, "任务名称和任务组不能为空");
        }
        jobManagerService.addJob(jobInfo);
        return Result.success("任务创建成功");
    }

    /**
     * 暂停任务
     */
    @PutMapping("/pause")
    public Result<String> pauseJob(@RequestParam String jobName, @RequestParam String jobGroup) throws BusinessException, SchedulerException {
        jobManagerService.pauseJob(jobName, jobGroup);
        return Result.success("任务暂停成功");
    }

    /**
     * 恢复任务
     */
    @PutMapping("/resume")
    public Result<String> resumeJob(@RequestParam String jobName, @RequestParam String jobGroup) throws BusinessException, SchedulerException{
        jobManagerService.resumeJob(jobName, jobGroup);
        return Result.success("任务恢复成功");
    }

    /**
     * 更新任务
     */
    @PutMapping
    public Result<String> updateJob(@RequestBody JobInfo jobInfo) throws SchedulerException {
        jobManagerService.updateJob(jobInfo);
        return Result.success("任务更新成功");
    }

    /**
     * 删除任务
     */
    @DeleteMapping
    public Result<String> deleteJob(@RequestParam String jobName, @RequestParam String jobGroup) throws SchedulerException {
        jobManagerService.deleteJob(jobName, jobGroup);
        return Result.success("任务删除成功");
    }

    /**
     * 立即执行任务
     */
    @PostMapping("/run")
    public Result<String> runJobNow(@RequestParam String jobName, @RequestParam String jobGroup) throws SchedulerException {
        jobManagerService.runJobNow(jobName, jobGroup);
        return Result.success("任务已触发执行");
    }

    /**
     * 查询任务列表
     */
    @GetMapping
    public Result<List<JobInfo>> getJobList() {
        return Result.success(jobManagerService.findAll());
    }
}
