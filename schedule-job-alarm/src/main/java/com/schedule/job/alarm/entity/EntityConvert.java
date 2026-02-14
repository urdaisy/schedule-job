package com.schedule.job.alarm.entity;

import com.schedule.job.alarm.domain.AlertConfig;
import com.schedule.job.alarm.domain.AlertRecord;

/**
 * 实体转换工具类
 */
public class EntityConvert {
    public static AlertConfigEntity convertToAlertConfigEntity(AlertConfig alertConfig) {
        AlertConfigEntity entity = new AlertConfigEntity();
        entity.setId(alertConfig.getId());
        entity.setJobId(alertConfig.getJobId());
        entity.setAlertType(alertConfig.getAlertType());
        entity.setReceiverEmail(alertConfig.getReceiverEmail());
        entity.setEnabled(alertConfig.getEnabled());
        entity.setCreateTime(alertConfig.getCreateTime());
        entity.setUpdateTime(alertConfig.getUpdateTime());
        return entity;
    }

    public static AlertConfig convertToAlertConfig(AlertConfigEntity entity) {
        AlertConfig alertConfig = new AlertConfig();
        alertConfig.setId(entity.getId());
        alertConfig.setJobId(entity.getJobId());
        alertConfig.setAlertType(entity.getAlertType());
        alertConfig.setReceiverEmail(entity.getReceiverEmail());
        alertConfig.setEnabled(entity.getEnabled());
        alertConfig.setCreateTime(entity.getCreateTime());
        alertConfig.setUpdateTime(entity.getUpdateTime());
        return alertConfig;
    }

    // ========== 告警记录转换 ==========
    public static AlertRecordEntity convertToAlertRecordEntity(AlertRecord alertRecord) {
        AlertRecordEntity entity = new AlertRecordEntity();
        entity.setId(alertRecord.getId());
        entity.setJobId(alertRecord.getJobId());
        entity.setJobLogId(alertRecord.getJobLogId());
        entity.setAlertType(alertRecord.getAlertType());
        entity.setReceiver(alertRecord.getReceiver());
        entity.setAlertStatus(alertRecord.getAlertStatus());
        entity.setAlertTime(alertRecord.getAlertTime());
        entity.setErrorMsg(alertRecord.getErrorMsg());
        entity.setCreateTime(alertRecord.getCreateTime());
        return entity;
    }

    public static AlertRecord convertToAlertRecord(AlertRecordEntity entity) {
        AlertRecord alertRecord = new AlertRecord();
        alertRecord.setId(entity.getId());
        alertRecord.setJobId(entity.getJobId());
        alertRecord.setJobLogId(entity.getJobLogId());
        alertRecord.setAlertType(entity.getAlertType());
        alertRecord.setReceiver(entity.getReceiver());
        alertRecord.setAlertStatus(entity.getAlertStatus());
        alertRecord.setAlertTime(entity.getAlertTime());
        alertRecord.setErrorMsg(entity.getErrorMsg());
        alertRecord.setCreateTime(entity.getCreateTime());
        return alertRecord;
    }
}
