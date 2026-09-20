package com.schedule.job.admin.mq.rabbit;

import com.schedule.job.admin.entity.AlertMessageMq;
import com.schedule.job.admin.entity.JobTriggerMessageMq;
import com.schedule.job.admin.mq.MqConst;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Slf4j
@Service
@ConditionalOnProperty(name = "mq.practice.enabled", havingValue = "true")
public class RabbitProducer {

    private final RabbitTemplate rabbitTemplate;

    @Autowired
    public RabbitProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    /**
     * 项目启动时开始测试任务 - 发送 RabbitMQ 测试消息
     */
    public void publishTestMessage(String message) {
        rabbitTemplate.convertAndSend(MqConst.TEST_EXCHANGE_NAME, MqConst.TEST_ROUTE_KEY, message);
    }

    /**
     * 触发任务失败告警 - 发送 RabbitMQ 消息
     */
    public void sendAlertMessage(Long jobId, Long jobLogId, String errorMsg) {
        try {
            AlertMessageMq alert = new AlertMessageMq();
            alert.setJobId(jobId);
            alert.setJobLogId(jobLogId);
            alert.setErrorMessage(errorMsg);
            alert.setAlarmTime(LocalDateTime.now());
            rabbitTemplate.convertAndSend(MqConst.BUSINESS_EXCHANGE_NAME, MqConst.ALERT_ROUTE_KEY, alert);
            log.info("任务失败告警已发送至RabbitMQ：jobId={}, jobLogId={}", jobId, jobLogId);
        } catch (Exception e) {
            log.error("发送RabbitMQ告警消息失败", e);
        }
    }

    /**
     * 发送任务触发消息，用于消息驱动触发任务。
     */
    public void sendJobTriggerMessage(String jobName, String jobGroup, String jobParam) {
        if (!StringUtils.hasText(jobName) || !StringUtils.hasText(jobGroup)) {
            throw new IllegalArgumentException("jobName 和 jobGroup 不能为空");
        }
        try {
            JobTriggerMessageMq triggerMessage = new JobTriggerMessageMq();
            triggerMessage.setJobName(jobName);
            triggerMessage.setJobGroup(jobGroup);
            triggerMessage.setJobParam(jobParam);
            triggerMessage.setTriggerSource("job-controller");
            triggerMessage.setTriggerTime(LocalDateTime.now());
            rabbitTemplate.convertAndSend(
                    MqConst.BUSINESS_EXCHANGE_NAME,
                    MqConst.JOB_TRIGGER_ROUTE_KEY,
                    triggerMessage
            );
            log.info("任务触发消息已发送至RabbitMQ：jobName={}, jobGroup={}", jobName, jobGroup);
        } catch (Exception e) {
            log.error("发送RabbitMQ任务触发消息失败：jobName={}, jobGroup={}", jobName, jobGroup, e);
            throw new IllegalStateException("发送任务触发消息失败", e);
        }
    }
}
