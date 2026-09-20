package com.schedule.job.admin.mq.rabbit;

import com.schedule.job.admin.entity.AlertMessageMq;
import com.schedule.job.admin.entity.JobTriggerMessageMq;
import com.schedule.job.admin.mq.MqConst;
import com.schedule.job.admin.service.JobManagerService;
import com.schedule.job.alarm.service.AlertService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * RabbitMQ 多业务消费者：
 * 1. 测试消息；
 * 2. 失败告警消息；
 * 3. 任务触发消息。
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "mq.practice.enabled", havingValue = "true")
public class RabbitConsumer {
    @Autowired
    private AlertService alertService;

    @Autowired
    private JobManagerService jobManagerService;

    @RabbitListener(queues = MqConst.TEST_QUEUE_NAME)
    public void receiveTestMessage(String message) {
        log.info("Rabbit 测试消息已消费：{}", message);
    }

    @RabbitListener(queues = MqConst.ALERT_QUEUE_NAME)
    public void receiveAlertMessage(AlertMessageMq message) {
        Long jobId = message.getJobId();
        Long jobLogId = message.getJobLogId();
        String errorMsg = message.getErrorMessage();
        try {
            alertService.triggerAlert(jobId, jobLogId, errorMsg);
        } catch (Exception e) {
            log.error("触发告警失败，jobId={}, jobLogId={}", jobId, jobLogId, e);
            throw new AmqpRejectAndDontRequeueException("告警消息消费失败，已投递至死信队列", e);
        }
    }

    @RabbitListener(queues = MqConst.JOB_TRIGGER_QUEUE_NAME)
    public void receiveJobTriggerMessage(JobTriggerMessageMq message) {
        if (message == null || !StringUtils.hasText(message.getJobName()) || !StringUtils.hasText(message.getJobGroup())) {
            throw new AmqpRejectAndDontRequeueException("任务触发消息缺少 jobName 或 jobGroup");
        }

        try {
            jobManagerService.runJobNow(message.getJobName(), message.getJobGroup(), message.getJobParam());
            log.info("RabbitMQ 触发任务成功：jobName={}, jobGroup={}", message.getJobName(), message.getJobGroup());
        } catch (Exception e) {
            log.error("RabbitMQ 触发任务失败：jobName={}, jobGroup={}", message.getJobName(), message.getJobGroup(), e);
            throw new AmqpRejectAndDontRequeueException("任务触发消息消费失败，已投递至死信队列", e);
        }
    }
}
