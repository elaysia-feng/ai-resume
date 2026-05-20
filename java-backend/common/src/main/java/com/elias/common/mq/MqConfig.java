package com.elias.common.mq;

import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@AutoConfiguration
public class MqConfig {

    // ======================== 交换机 ========================

    /**
     * 订单状态变更交换机
     */
    @Bean
    public DirectExchange orderExchange() {
        return ExchangeBuilder
                .directExchange(MqConstants.ORDER_EXCHANGE)
                .delayed()
                .durable(true)
                .build();

    }

    /**
     * 死信交换机
     */
    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(MqConstants.DEAD_LETTER_EXCHANGE, true, false);
    }

    // ======================== 队列 ========================

    /**
     * 支付成功队列
     */
    @Bean
    public Queue paidQueue() {
        return QueueBuilder.durable(MqConstants.PAID_QUEUE)
                .lazy()
                .withArgument("x-dead-letter-exchange", MqConstants.DEAD_LETTER_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", MqConstants.ROUTING_KEY_DEAD)
                .build();
    }

    /**
     * 订单过期队列
     */
    @Bean
    public Queue expiredQueue() {
        return QueueBuilder.durable(MqConstants.EXPIRED_QUEUE)
                .lazy()
                .withArgument("x-dead-letter-exchange", MqConstants.DEAD_LETTER_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", MqConstants.ROUTING_KEY_DEAD)
                .build();
    }

    /**
     * 订单取消队列
     */
    @Bean
    public Queue cancelledQueue() {
        return QueueBuilder.durable(MqConstants.CANCELLED_QUEUE)
                .lazy()
                .withArgument("x-dead-letter-exchange", MqConstants.DEAD_LETTER_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", MqConstants.ROUTING_KEY_DEAD)
                .build();
    }

    /**
     * 订单延迟队列
     */
    @Bean
    public Queue delayQueue(){
        return QueueBuilder.durable(MqConstants.ORDER_DELAY_QUEUE)
                .lazy()
                .build();
    }


    /**
     * 死信队列
     */
    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(MqConstants.DEAD_LETTER_QUEUE).build();
    }

    // ======================== 绑定 ========================

    @Bean
    public Binding paidBinding() {
        return BindingBuilder.bind(paidQueue())
                .to(orderExchange())
                .with(MqConstants.ROUTING_KEY_PAID);
    }

    @Bean
    public Binding expiredBinding() {
        return BindingBuilder.bind(expiredQueue())
                .to(orderExchange())
                .with(MqConstants.ROUTING_KEY_EXPIRED);
    }

    @Bean
    public Binding cancelledBinding() {
        return BindingBuilder.bind(cancelledQueue())
                .to(orderExchange())
                .with(MqConstants.ROUTING_KEY_CANCELLED);
    }

    @Bean
    public Binding deadLetterBinding() {
        return BindingBuilder.bind(deadLetterQueue())
                .to(deadLetterExchange())
                .with(MqConstants.ROUTING_KEY_DEAD);
    }

    @Bean
    public Binding orderDelayBinding() {
        return BindingBuilder.bind(delayQueue())
                .to(orderExchange())
                .with(MqConstants.ROUTING_KEY_DELAY);
    }

    // ======================== RabbitTemplate ========================

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(new Jackson2JsonMessageConverter());

        // 路由失败回调
        rabbitTemplate.setReturnsCallback(returned -> {
            log.error("消息路由失败: exchange={}, routingKey={}, replyCode={}, replyText={}",
                    returned.getExchange(),
                    returned.getRoutingKey(),
                    returned.getReplyCode(),
                    returned.getReplyText());
        });

        return rabbitTemplate;
    }
}
