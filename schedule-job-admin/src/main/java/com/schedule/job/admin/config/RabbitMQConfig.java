package com.schedule.job.admin.config;

import com.schedule.job.admin.mq.MqConst;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 配置：连接工厂、RabbitTemplate、JSON 转换器和多业务队列拓扑。
 * RabbitAdmin 会自动发现 Declarables 并在 Broker 上声明 exchange/queue/binding。
 */
@Configuration
@ConditionalOnProperty(name = "mq.practice.enabled", havingValue = "true")
public class RabbitMQConfig {

    @Value("${spring.rabbitmq.host}")
    private String host;
    @Value("${spring.rabbitmq.port}")
    private int port;
    @Value("${spring.rabbitmq.username}")
    private String username;
    @Value("${spring.rabbitmq.password}")
    private String password;

    @Bean
    public ConnectionFactory connectionFactory() {
        CachingConnectionFactory factory = new CachingConnectionFactory(host);
        factory.setPort(port);
        factory.setUsername(username);
        factory.setPassword(password);
        return factory;
    }

    @Bean
    public MessageConverter rabbitMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter rabbitMessageConverter) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(rabbitMessageConverter);
        return rabbitTemplate;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            MessageConverter rabbitMessageConverter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(rabbitMessageConverter);
        factory.setDefaultRequeueRejected(false);
        return factory;
    }

    @Bean
    public AmqpAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
        return new RabbitAdmin(connectionFactory);
    }

    @Bean
    public Declarables rabbitDeclarables() {
        DirectExchange testExchange = new DirectExchange(MqConst.TEST_EXCHANGE_NAME, true, false);
        TopicExchange businessExchange = new TopicExchange(MqConst.BUSINESS_EXCHANGE_NAME, true, false);
        DirectExchange deadLetterExchange = new DirectExchange(MqConst.DEAD_LETTER_EXCHANGE_NAME, true, false);

        Queue testQueue = new Queue(MqConst.TEST_QUEUE_NAME, true);
        Queue alertQueue = QueueBuilder.durable(MqConst.ALERT_QUEUE_NAME)
                .deadLetterExchange(MqConst.DEAD_LETTER_EXCHANGE_NAME)
                .deadLetterRoutingKey(MqConst.ALERT_DLX_ROUTE_KEY)
                .build();
        Queue alertDeadLetterQueue = QueueBuilder.durable(MqConst.ALERT_DLX_QUEUE_NAME).build();
        Queue jobTriggerQueue = QueueBuilder.durable(MqConst.JOB_TRIGGER_QUEUE_NAME)
                .deadLetterExchange(MqConst.DEAD_LETTER_EXCHANGE_NAME)
                .deadLetterRoutingKey(MqConst.JOB_TRIGGER_DLX_ROUTE_KEY)
                .build();
        Queue jobTriggerDeadLetterQueue = QueueBuilder.durable(MqConst.JOB_TRIGGER_DLX_QUEUE_NAME).build();

        Binding testBinding = BindingBuilder.bind(testQueue).to(testExchange).with(MqConst.TEST_ROUTE_KEY);
        Binding alertBinding = BindingBuilder.bind(alertQueue).to(businessExchange).with(MqConst.ALERT_ROUTE_KEY);
        Binding alertDeadLetterBinding = BindingBuilder.bind(alertDeadLetterQueue)
                .to(deadLetterExchange)
                .with(MqConst.ALERT_DLX_ROUTE_KEY);
        Binding jobTriggerBinding = BindingBuilder.bind(jobTriggerQueue)
                .to(businessExchange)
                .with(MqConst.JOB_TRIGGER_ROUTE_KEY);
        Binding jobTriggerDeadLetterBinding = BindingBuilder.bind(jobTriggerDeadLetterQueue)
                .to(deadLetterExchange)
                .with(MqConst.JOB_TRIGGER_DLX_ROUTE_KEY);

        return new Declarables(
                testExchange,
                businessExchange,
                deadLetterExchange,
                testQueue,
                alertQueue,
                alertDeadLetterQueue,
                jobTriggerQueue,
                jobTriggerDeadLetterQueue,
                testBinding,
                alertBinding,
                alertDeadLetterBinding,
                jobTriggerBinding,
                jobTriggerDeadLetterBinding
        );
    }
}
