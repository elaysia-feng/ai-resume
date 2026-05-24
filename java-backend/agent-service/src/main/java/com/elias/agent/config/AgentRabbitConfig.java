package com.elias.agent.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Agent RabbitMQ 基础设施配置。
 *
 * 这组 Bean 只解决三件事：
 * 1. Java 创建 run 后把任务投递到主交换机。
 * 2. Python resume / interview worker 分别从自己的 durable 队列消费任务。
 * 3. 坏消息被 reject 后进入 DLQ，避免卡住正常任务。
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
// TODO 统一mq
public class AgentRabbitConfig {

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter
            agentRabbitMessageConverter) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(agentRabbitMessageConverter);

        // publisher returns — 路由失败时回调
        rabbitTemplate.setReturnsCallback(returned -> {
            log.error("消息路由失败: exchange={}, routingKey={}, replyCode={}, replyText={}",
                    returned.getExchange(),
                    returned.getRoutingKey(),
                    returned.getReplyCode(),
                    returned.getReplyText());
        });

        return rabbitTemplate;
    }



    private final AgentRabbitProperties agentRabbitProperties;

    @Bean
    public DirectExchange agentRunExchange() {
        // 主交换机：只承载 Agent run 任务，不和其它业务队列混用。
        return new DirectExchange(agentRabbitProperties.getRunExchange(), true, false);
    }

    @Bean
    public DirectExchange agentRunDeadLetterExchange() {
        // 死信交换机：格式错误、无法处理的消息会被路由到这里。
        return new DirectExchange(agentRabbitProperties.getDeadLetterExchange(), true, false);
    }
    // 简历优化 Agent 运行的消息队列
    @Bean
    public Queue agentResumeRunQueue() {
        return QueueBuilder.durable(agentRabbitProperties.getResumeRunQueue())
                // worker reject 且不 requeue 时，消息会进入 DLQ，方便排查坏消息。
                .deadLetterExchange(agentRabbitProperties.getDeadLetterExchange())
                .deadLetterRoutingKey(agentRabbitProperties.getDeadLetterRoutingKey())
                // 成为lazyQueue 这样就可以写入到磁盘里, 且可以持久化(其实在很多写入的时候, 这个方法更快, 因为mq对IO进行了优化)
                .lazy()
                .build();
    }

    // 面试模拟 Agent 运行的消息队列
    @Bean
    public Queue agentInterviewRunQueue() {
        return QueueBuilder.durable(agentRabbitProperties.getInterviewRunQueue())
                .deadLetterExchange(agentRabbitProperties.getDeadLetterExchange())
                .deadLetterRoutingKey(agentRabbitProperties.getDeadLetterRoutingKey())
                .lazy()
                .build();
    }

    @Bean
    public Queue agentRunDeadLetterQueue() {
        // DLQ 只用于排查问题，不参与正常消费链路。
        return QueueBuilder.durable(agentRabbitProperties.getDeadLetterQueue()).build();
    }

    @Bean
    public Binding agentResumeRunStartBinding() {
        return BindingBuilder.bind(agentResumeRunQueue())
                .to(agentRunExchange())
                .with(agentRabbitProperties.getResumeStartRoutingKey());
    }

    @Bean
    public Binding agentResumeRunContinueBinding() {
        return BindingBuilder.bind(agentResumeRunQueue())
                .to(agentRunExchange())
                .with(agentRabbitProperties.getResumeContinueRoutingKey());
    }

    @Bean
    public Binding agentInterviewRunStartBinding() {
        return BindingBuilder.bind(agentInterviewRunQueue())
                .to(agentRunExchange())
                .with(agentRabbitProperties.getInterviewStartRoutingKey());
    }

    @Bean
    public Binding agentInterviewRunContinueBinding() {
        return BindingBuilder.bind(agentInterviewRunQueue())
                .to(agentRunExchange())
                .with(agentRabbitProperties.getInterviewContinueRoutingKey());
    }

    @Bean
    public Binding agentRunDeadLetterBinding() {
        // reject(requeue=false) 的坏消息最终会绑定到这条 DLQ。
        return BindingBuilder.bind(agentRunDeadLetterQueue())
                .to(agentRunDeadLetterExchange())
                .with(agentRabbitProperties.getDeadLetterRoutingKey());
    }

    @Bean
    public MessageConverter agentRabbitMessageConverter(ObjectMapper objectMapper) {
        // 统一使用项目 ObjectMapper，避免 Java 字段和 Python alias 序列化不一致。
        Jackson2JsonMessageConverter jackson2JsonMessageConverter = new Jackson2JsonMessageConverter(objectMapper);
        jackson2JsonMessageConverter.setCreateMessageIds(true);

        return jackson2JsonMessageConverter;
    }
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            MessageConverter agentRabbitMessageConverter
    ) {
        // 目前 Java 主要是生产者；保留监听工厂，后续加 Java consumer 时沿用同一套 JSON 转换。
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(agentRabbitMessageConverter);
        return factory;
    }

    @Bean
    public Queue agentResultQueue() {
        return QueueBuilder.durable(agentRabbitProperties.getRunResultQueue())
                .deadLetterExchange(agentRabbitProperties.getDeadLetterExchange())
                .deadLetterRoutingKey(agentRabbitProperties.getDeadLetterRoutingKey())
                .lazy()
                .build();
    }

}
