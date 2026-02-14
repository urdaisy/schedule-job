package com.schedule.job.alarm.controller;

import com.schedule.job.alarm.domain.AlertConfig;
import com.schedule.job.alarm.service.AlertService;
import com.schedule.job.common.enums.ResultCode;
import com.schedule.job.common.exception.Result;
import com.schedule.job.security.annotation.RequirePermission;
import jakarta.annotation.Resource;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 告警配置管理API
 */
@RestController
@RequestMapping("/api/alert/config")
public class AlertConfigController {
    
    @Resource
    private AlertService alertService;
    
    /**
     * 获取所有告警配置列表
     */
    @GetMapping
    @RequirePermission("job:query")
    public Result<List<AlertConfig>> getAllAlertConfigs() {
        try {
            List<AlertConfig> configs = alertService.getAllAlertConfigs();
            return Result.success(configs);
        } catch (Exception e) {
            return Result.error(ResultCode.ERROR, "获取告警配置失败：" + e.getMessage());
        }
    }
    
    /**
     * 获取任务的告警配置列表
     */
    @GetMapping("/job/{jobId}")
    @RequirePermission("job:query")
    public Result<List<AlertConfig>> getAlertConfigsByJobId(@PathVariable Long jobId) {
        try {
            List<AlertConfig> configs = alertService.getAlertConfigsByJobId(jobId);
            return Result.success(configs);
        } catch (Exception e) {
            return Result.error(ResultCode.ERROR, "获取告警配置失败：" + e.getMessage());
        }
    }
    
    /**
     * 创建或更新告警配置
     */
    @PostMapping
    @RequirePermission("job:update")
    public Result<AlertConfig> saveAlertConfig(
            @RequestParam Long jobId,
            @RequestParam String alertType,
            @RequestParam String receiverEmail,
            @RequestParam(required = false, defaultValue = "1") Integer enabled) {
        try {
            // 参数验证
            if (jobId == null) {
                return Result.error(ResultCode.PARAM_ERROR, "任务ID不能为空");
            }
            if (StringUtils.isEmpty(alertType)) {
                return Result.error(ResultCode.PARAM_ERROR, "告警类型不能为空");
            }
            if (StringUtils.isEmpty(receiverEmail)) {
                return Result.error(ResultCode.PARAM_ERROR, "接收人邮箱不能为空");
            }
            
            // 验证邮箱格式（简单验证）
            String[] emails = receiverEmail.split(",");
            for (String email : emails) {
                String trimmed = email.trim();
                if (!trimmed.contains("@") || !trimmed.contains(".")) {
                    return Result.error(ResultCode.PARAM_ERROR, "邮箱格式不正确：" + trimmed);
                }
            }
            
            AlertConfig config = new AlertConfig();
            config.setJobId(jobId);
            config.setAlertType(alertType);
            config.setReceiverEmail(receiverEmail.trim());
            config.setEnabled(enabled != null && enabled == 1 ? 1 : 0);
            
            AlertConfig saved = alertService.saveAlertConfig(config);
            return Result.success("告警配置保存成功", saved);
        } catch (Exception e) {
            return Result.error(ResultCode.ERROR, "保存告警配置失败：" + e.getMessage());
        }
    }
    
    /**
     * 更新告警配置
     */
    @PutMapping("/{id}")
    @RequirePermission("job:update")
    public Result<AlertConfig> updateAlertConfig(
            @PathVariable Long id,
            @RequestParam(required = false) String receiverEmail,
            @RequestParam(required = false) Integer enabled) {
        try {
            // 这里需要先查询配置，然后更新
            // 为了简化，我们要求传入完整的配置信息
            return Result.error(ResultCode.ERROR, "请使用POST接口创建或更新告警配置");
        } catch (Exception e) {
            return Result.error(ResultCode.ERROR, "更新告警配置失败：" + e.getMessage());
        }
    }
    
    /**
     * 删除告警配置
     */
    @DeleteMapping("/{id}")
    @RequirePermission("job:update")
    public Result<String> deleteAlertConfig(@PathVariable Long id) {
        try {
            boolean deleted = alertService.deleteAlertConfig(id);
            if (deleted) {
                return Result.success("告警配置删除成功");
            } else {
                return Result.error(ResultCode.ERROR, "告警配置不存在或删除失败");
            }
        } catch (Exception e) {
            return Result.error(ResultCode.ERROR, "删除告警配置失败：" + e.getMessage());
        }
    }
}
