package com.elias.common.mq;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.retry.MessageRecoverer;
import org.springframework.amqp.rabbit.retry.RepublishMessageRecoverer;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;

@Slf4j
@AutoConfiguration
@RequiredArgsConstructor
public class RabbitMQErrorConfig {
    // TODO 搞懂这个死信交换机怎么消费
    @Bean
    @ConditionalOnBean(RabbitTemplate.class)
    public MessageRecoverer republishMessageRecoverer(RabbitTemplate rabbitTemplate) {
        return new RepublishMessageRecoverer(rabbitTemplate,
                MqConstants.DEAD_LETTER_EXCHANGE, MqConstants.ROUTING_KEY_DEAD);
    }
}