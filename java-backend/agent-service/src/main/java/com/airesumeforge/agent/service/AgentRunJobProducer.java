package com.airesumeforge.agent.service;

import com.airesumeforge.agent.config.AgentRabbitProperties;
import com.airesumeforge.agent.dto.run.request.AgentRunJobMessage;
import com.airesumeforge.common.dto.interview.internal.request.InterviewAgentRunJobMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

/**
 * Agent run MQ 生产者。
 * 这里只做一件事：把已经落库的 run 投递到 RabbitMQ。
 * run 的创建、状态修改、事务边界都在 AgentRunServiceImpl 里控制。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentRunJobProducer {

    private final RabbitTemplate rabbitTemplate;
    private final AgentRabbitProperties agentRabbitProperties;

    public void publish(AgentRunJobMessage message) {
        // START / CONTINUE 进入同一条队列，但路由键分开，方便后续按类型拆队列或监控。
            String routingKey = "CONTINUE".equals(message.getJobType())
                    ? agentRabbitProperties.getContinueRoutingKey()
                    : agentRabbitProperties.getStartRoutingKey();

            // TODO 这里先简单处理, 就是没条消息失败了, 我打印日志
            CorrelationData correlationData = new CorrelationData();
            correlationData.getFuture().whenComplete((confirm, throwable) -> {
                if (throwable != null) {
                    // 异常情况
                    log.error("确认异常", throwable);
                    return;
                }
                if (confirm != null) {
                    if (confirm.isAck()) {
                        log.info("消息确认成功");
                    } else {
                        log.error("消息被 Nack，原因: {}", confirm.getReason());
                    }
                }
            }
            );

            rabbitTemplate.convertAndSend(agentRabbitProperties.getRunExchange(), routingKey, message, correlationData);

    }
    public void publish(InterviewAgentRunJobMessage message) {
        // START / CONTINUE 进入同一条队列，但路由键分开，方便后续按类型拆队列或监控。
        String routingKey = "CONTINUE".equals(message.getJobType())
                ? agentRabbitProperties.getContinueRoutingKey()
                : agentRabbitProperties.getStartRoutingKey();

        // TODO 这里先简单处理, 就是没条消息失败了, 我打印日志
        CorrelationData correlationData = new CorrelationData();
        correlationData.getFuture().whenComplete((confirm, throwable) -> {
                    if (throwable != null) {
                        // 异常情况
                        log.error("确认异常", throwable);
                        return;
                    }
                    if (confirm != null) {
                        if (confirm.isAck()) {
                            log.info("消息确认成功");
                        } else {
                            log.error("消息被 Nack，原因: {}", confirm.getReason());
                        }
                    }
                }
        );
        rabbitTemplate.convertAndSend(agentRabbitProperties.getRunExchange(), routingKey, message, correlationData);

    }
}