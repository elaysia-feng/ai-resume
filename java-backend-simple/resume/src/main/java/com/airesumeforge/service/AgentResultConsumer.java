package com.airesumeforge.service;

import com.airesumeforge.common.AgentRunStatus;
import com.airesumeforge.config.AgentRabbitProperties;
import com.airesumeforge.dto.agent.internal.request.RunEventBatchRequest;
import com.airesumeforge.dto.agent.internal.request.RunStatusUpdateRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 消费 Python 回写的 Agent run 事件和状态。
 * 替代原来 Python → Java 的 HTTP 回写接口，解耦为 MQ。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentResultConsumer {

    private final ObjectMapper objectMapper;
    private final InternalAgentSupportService internalAgentSupportService;
    private final AgentRabbitProperties agentRabbitProperties;
    private final RabbitTemplate rabbitTemplate;

    @RabbitListener(queues = "#{agentRabbitProperties.runResultQueue}")
    public void onMessage(Message amqpMessage) {
        MessageProperties props = amqpMessage.getMessageProperties();
        Integer retryCount = props.getHeader("retry-count");
        int attempts = (retryCount != null ? retryCount : 0) + 1;

        try {
            Map<String, Object> payload = objectMapper.readValue(
                    amqpMessage.getBody(), Map.class);
            String action = (String) payload.get("action");
            Long runId = ((Number) payload.get("runId")).longValue();

            switch (action) {
                case "PUSH_EVENTS" -> {
                    RunEventBatchRequest request = objectMapper.convertValue(
                            payload.get("events"), RunEventBatchRequest.class);
                    internalAgentSupportService.saveRunEvents(runId, request);
                }
                case "UPDATE_STATUS" -> {
                    RunStatusUpdateRequest request = objectMapper.convertValue(
                            payload, RunStatusUpdateRequest.class);
                    internalAgentSupportService.updateRunStatus(runId, request);
                }
                case "NOTIFY_CANCELLED" -> {
                    RunStatusUpdateRequest request = new RunStatusUpdateRequest();
                    request.setStatus(AgentRunStatus.CANCELLED.getCode());
                    internalAgentSupportService.updateRunStatus(runId, request);
                }
            }
        } catch (Exception e) {
            long delay = (long) (agentRabbitProperties.getRetryInitialInterval()
                    * Math.pow(agentRabbitProperties.getRetryMultiplier(), attempts - 1));
            delay = Math.min(delay, agentRabbitProperties.getRetryMaxInterval());

            if (attempts < agentRabbitProperties.getMaxConsumeRetries()) {
                log.warn("消费失败，第 {} 次重试，{}ms 后重试", attempts, delay, e);
                props.setHeader("retry-count", attempts);
                rabbitTemplate.send(amqpMessage);
            } else {
                log.error("重试 {} 次后仍失败，reject 到 DLQ", attempts, e);
                rabbitTemplate.send(amqpMessage);  // reject → DLQ
            }
        }
    }
}

