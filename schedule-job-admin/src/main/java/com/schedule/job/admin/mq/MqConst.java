package com.schedule.job.admin.mq;

/**
 * RabbitMQ 拓扑常量。
 *
 * 约定：
 * 1. 业务消息统一走一个 TopicExchange；
 * 2. 不同业务使用不同 queue + routing key；
 * 3. 每个业务 queue 对应自己的死信队列，便于单独排查。
 */
public final class MqConst {

    private MqConst() {
    }

    // 项目启动时发送一条测试消息
    public static final String TEST_EXCHANGE_NAME = "schedule.job.test.exchange";
    public static final String TEST_QUEUE_NAME = "schedule.job.test.queue";
    public static final String TEST_ROUTE_KEY = "test.message";

    // 业务交换机与死信交换机
    public static final String BUSINESS_EXCHANGE_NAME = "schedule.job.topic";
    public static final String DEAD_LETTER_EXCHANGE_NAME = "schedule.job.dlx";

    // 告警消息
    public static final String ALERT_QUEUE_NAME = "schedule.job.alert.request";
    public static final String ALERT_ROUTE_KEY = "alert.request";
    public static final String ALERT_DLX_QUEUE_NAME = "schedule.job.alert.dlq";
    public static final String ALERT_DLX_ROUTE_KEY = "alert.dlq";

    // 任务触发消息
    public static final String JOB_TRIGGER_QUEUE_NAME = "schedule.job.trigger.request";
    public static final String JOB_TRIGGER_ROUTE_KEY = "job.trigger.request";
    public static final String JOB_TRIGGER_DLX_QUEUE_NAME = "schedule.job.trigger.dlq";
    public static final String JOB_TRIGGER_DLX_ROUTE_KEY = "job.trigger.dlq";
}
