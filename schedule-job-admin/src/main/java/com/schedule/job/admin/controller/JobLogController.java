package com.schedule.job.admin.controller;

import com.schedule.job.admin.job.JobLog;
import com.schedule.job.admin.repository.PageInfo;
import com.schedule.job.admin.service.JobLogService;
import com.schedule.job.common.exception.Result;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.sql.SQLException;

/**
 * 日志查询API
 */
@RestController
@RequestMapping("/api/log")
public class JobLogController {
    @Resource
    private JobLogService jobLogService;

    /**
     * 查询任务日志
     */
    @GetMapping
    public Result<PageInfo<JobLog>> findAllJobLog(@RequestParam(defaultValue = "1") Integer pageNum, 
                                                   @RequestParam(defaultValue = "10") Integer pageSize) {
        PageInfo<JobLog> pageInfo = jobLogService.findAllJobLog(pageNum, pageSize);
        return Result.success(pageInfo);
    }

    /**
     * 查询指定日志详情
     */
    @GetMapping("/{id}")
    public Result<JobLog> findById(@PathVariable Long id) throws SQLException {
        JobLog jobLog = jobLogService.findById(id);
        return Result.success(jobLog);
    }

    /**
     * 删除日志
     */
    @DeleteMapping("/{id}")
    public Result<String> deleteById(@PathVariable Long id) throws SQLException {
        boolean deleted = jobLogService.deleteById(id);
        if (deleted) {
            return Result.success("日志删除成功");
        }
        return Result.error("日志删除失败");
    }
}
