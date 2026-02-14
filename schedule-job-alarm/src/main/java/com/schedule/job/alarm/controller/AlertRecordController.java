package com.schedule.job.alarm.controller;

import com.schedule.job.alarm.domain.AlertRecord;
import com.schedule.job.alarm.service.AlertService;
import com.schedule.job.common.enums.ResultCode;
import com.schedule.job.common.exception.Result;
import com.schedule.job.security.annotation.RequirePermission;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/alert/record")
public class AlertRecordController {
    
    @Resource
    private AlertService alertService;
    
    /**
     * 获取所有告警记录
     */
    @GetMapping
    @RequirePermission("job:query")
    public Result<List<AlertRecord>> getAllAlertRecords() {
        try {
            List<AlertRecord> records = alertService.getAllAlertRecords();
            return Result.success(records);
        } catch (Exception e) {
            return Result.error(ResultCode.ERROR, "获取告警记录失败：" + e.getMessage());
        }
    }
    
    /**
     * 根据任务ID获取告警记录
     */
    @GetMapping("/job/{jobId}")
    @RequirePermission("job:query")
    public Result<List<AlertRecord>> getAlertRecordsByJobId(@PathVariable Long jobId) {
        try {
            List<AlertRecord> records = alertService.getAlertRecordsByJobId(jobId);
            return Result.success(records);
        } catch (Exception e) {
            return Result.error(ResultCode.ERROR, "获取告警记录失败：" + e.getMessage());
        }
    }
}
