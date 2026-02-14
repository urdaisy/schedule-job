package com.schedule.job.alarm.service;

import com.schedule.job.alarm.domain.AlertConfig;
import com.schedule.job.alarm.domain.AlertRecord;
import com.schedule.job.alarm.domain.JobInfoProvider;
import com.schedule.job.alarm.entity.AlertConfigEntity;
import com.schedule.job.alarm.entity.AlertRecordEntity;
import com.schedule.job.alarm.entity.EntityConvert;
import com.schedule.job.alarm.repository.AlertConfigRepository;
import com.schedule.job.alarm.repository.AlertRecordRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AlertService {
    
    private static final String ALERT_TYPE_EMAIL = "EMAIL";
    private static final int ALERT_STATUS_PENDING = 0; // 待发送
    private static final int ALERT_STATUS_SUCCESS = 1; // 发送成功
    private static final int ALERT_STATUS_FAILED = 2; // 发送失败
    
    @Autowired
    private AlertConfigRepository alertConfigRepository;
    
    @Autowired
    private AlertRecordRepository alertRecordRepository;
    
    @Autowired
    private EmailService emailService;
    
    @Autowired(required = false)
    private JobInfoProvider jobInfoProvider;
    
    /**
     * 触发任务失败告警
     * 
     * @param jobId 任务ID
     * @param jobLogId 任务日志ID
     * @param errorMsg 错误信息
     */
    public void triggerAlert(Long jobId, Long jobLogId, String errorMsg) {
        try {
            // 查询任务的告警配置
            List<AlertConfig> alertConfigs = alertConfigRepository.findByJobId(jobId);
            
            if (alertConfigs.isEmpty()) {
                log.debug("任务[{}]没有配置告警，跳过告警", jobId);
                return;
            }
            
            // 获取任务信息（通过接口获取，解耦对admin模块的依赖）
            if (jobInfoProvider == null) {
                log.warn("JobInfoProvider未配置，无法获取任务信息，跳过告警");
                return;
            }
            
            String jobName = jobInfoProvider.getJobName(jobId);
            String jobGroup = jobInfoProvider.getJobGroup(jobId);
            
            if (jobName == null || jobGroup == null) {
                log.warn("任务[{}]不存在，无法发送告警", jobId);
                return;
            }
            
            // 遍历告警配置，发送告警
            for (AlertConfig config : alertConfigs) {
                if (config.getEnabled() == null || config.getEnabled() != 1) {
                    log.debug("告警配置[{}]已禁用，跳过", config.getId());
                    continue;
                }
                
                // 创建告警记录（待发送状态）
                AlertRecord alertRecord = new AlertRecord();
                alertRecord.setJobId(jobId);
                alertRecord.setJobLogId(jobLogId);
                alertRecord.setAlertType(config.getAlertType());
                alertRecord.setReceiver(config.getReceiverEmail());
                alertRecord.setAlertStatus(ALERT_STATUS_PENDING);
                alertRecord.setAlertTime(LocalDateTime.now());
                alertRecord.setCreateTime(LocalDateTime.now());
                
                AlertRecord savedRecord = alertRecordRepository.saveAlertRecord(alertRecord);
                
                // 根据告警类型发送告警
                if (ALERT_TYPE_EMAIL.equals(config.getAlertType())) {
                    sendEmailAlert(config, jobName, jobGroup, errorMsg, savedRecord);
                } else {
                    log.warn("不支持的告警类型: {}", config.getAlertType());
                    alertRecordRepository.updateAlertRecordStatus(
                            savedRecord.getId(), 
                            ALERT_STATUS_FAILED, 
                            "不支持的告警类型: " + config.getAlertType()
                    );
                }
            }
            
        } catch (Exception e) {
            log.error("触发告警失败，jobId: {}, jobLogId: {}", jobId, jobLogId, e);
        }
    }
    
    /**
     * 发送邮件告警
     */
    private void sendEmailAlert(AlertConfig config, String jobName, String jobGroup, String errorMsg, AlertRecord alertRecord) {
        try {
            String subject = String.format("【任务失败告警】%s/%s", jobName, jobGroup);
            String executeTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            String content = emailService.buildAlertEmailContent(jobName, jobGroup, errorMsg, executeTime);
            
            boolean success = emailService.sendAlertEmail(config.getReceiverEmail(), subject, content);
            
            // 更新告警记录状态
            if (success) {
                alertRecordRepository.updateAlertRecordStatus(
                        alertRecord.getId(), 
                        ALERT_STATUS_SUCCESS, 
                        null
                );
                log.info("邮件告警发送成功，jobId: {}, receiver: {}", alertRecord.getJobId(), config.getReceiverEmail());
            } else {
                alertRecordRepository.updateAlertRecordStatus(
                        alertRecord.getId(), 
                        ALERT_STATUS_FAILED, 
                        "邮件发送失败"
                );
                log.error("邮件告警发送失败，jobId: {}, receiver: {}", alertRecord.getJobId(), config.getReceiverEmail());
            }
            
        } catch (Exception e) {
            log.error("发送邮件告警异常，jobId: {}", alertRecord.getJobId(), e);
            alertRecordRepository.updateAlertRecordStatus(
                    alertRecord.getId(), 
                    ALERT_STATUS_FAILED, 
                    "发送异常: " + e.getMessage()
            );
        }
    }
    
    /**
     * 保存或更新告警配置
     */
    public AlertConfig saveAlertConfig(AlertConfig alertConfig) {
        if (alertConfig.getId() == null) {
            // 新增
            alertConfig.setCreateTime(LocalDateTime.now());
            alertConfig.setUpdateTime(LocalDateTime.now());
            if (alertConfig.getEnabled() == null) {
                alertConfig.setEnabled(1); // 默认启用
            }
            return alertConfigRepository.saveAlertConfig(alertConfig);
        } else {
            // 更新
            alertConfig.setUpdateTime(LocalDateTime.now());
            return alertConfigRepository.updateAlertConfig(alertConfig);
        }
    }
    
    /**
     * 根据任务ID查询告警配置
     */
    public List<AlertConfig> getAlertConfigsByJobId(Long jobId) {
        return alertConfigRepository.findByJobId(jobId);
    }
    
    /**
     * 删除告警配置
     */
    public boolean deleteAlertConfig(Long id) {
        return alertConfigRepository.deleteAlertConfig(id);
    }
    
    /**
     * 查询所有告警配置
     */
    public List<AlertConfig> getAllAlertConfigs() {
        try {
            // 使用Repository的findAll方法，然后转换为领域对象并排序
            List<AlertConfigEntity> entities = alertConfigRepository.findAll();
            List<AlertConfig> configs = entities.stream()
                    .map(EntityConvert::convertToAlertConfig)
                    .collect(Collectors.toList());
            // 按创建时间倒序排序
            configs.sort((a, b) -> {
                if (a.getCreateTime() == null || b.getCreateTime() == null) {
                    return 0;
                }
                return b.getCreateTime().compareTo(a.getCreateTime());
            });
            return configs;
        } catch (Exception e) {
            log.error("查询所有告警配置失败", e);
            throw new RuntimeException("查询所有告警配置失败", e);
        }
    }
    
    /**
     * 查询所有告警记录
     */
    public List<AlertRecord> getAllAlertRecords() {
        try {
            // 使用Repository的findAll方法，然后转换为领域对象并排序
            List<AlertRecordEntity> entities = alertRecordRepository.findAll();
            List<AlertRecord> records = entities.stream()
                    .map(EntityConvert::convertToAlertRecord)
                    .collect(Collectors.toList());
            // 按告警时间倒序排序，并限制数量
            records.sort((a, b) -> {
                if (a.getAlertTime() == null || b.getAlertTime() == null) {
                    return 0;
                }
                return b.getAlertTime().compareTo(a.getAlertTime());
            });
            // 限制返回1000条
            if (records.size() > 1000) {
                return records.subList(0, 1000);
            }
            return records;
        } catch (Exception e) {
            log.error("查询所有告警记录失败", e);
            throw new RuntimeException("查询所有告警记录失败", e);
        }
    }
    
    /**
     * 根据任务ID查询告警记录
     */
    public List<AlertRecord> getAlertRecordsByJobId(Long jobId) {
        return alertRecordRepository.findByJobId(jobId);
    }
}
