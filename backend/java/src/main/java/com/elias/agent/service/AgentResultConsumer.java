package com.elias.agent.service;

import com.elias.common.AgentRunStatus;
import com.elias.common.dto.agent.internal.request.RunEventBatchRequest;
import com.elias.common.dto.agent.internal.request.RunStatusUpdateRequest;
import com.elias.common.stream.AgentStreamClient;
import com.elias.common.stream.AgentStreamProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 消费 Python 回写的 Agent run 事件和状态。
 *
 * <p>原来是从 RabbitMQ 的 agent.run.result 队列消费，现在改成读 SQLite 任务队列
 * 的 agent.run.result 流，处理完用 XACK 确认。</p>
 *
 * <p>消费失败的处理沿用原来的语义：重投回去并带上 retryCount，
 * 超过最大重试次数就转投死信流。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentResultConsumer {

    /**
     * 没有新消息时的轮询间隔（毫秒）。
     */
    private static final long POLL_INTERVAL_MS = 500L;

    /**
     * 启动后多久把遗留的未确认消息收回来（毫秒）。
     */
    private static final long RECLAIM_MIN_IDLE_MS = 60_000L;

    private final ObjectMapper objectMapper;
    private final InternalAgentSupportService internalAgentSupportService;
    private final AgentStreamProperties agentStreamProperties;
    private final AgentStreamClient agentStreamClient;
    private final ThreadPoolTaskExecutor agentResultStreamExecutor;

    @PostConstruct
    public void init() {
        String stream = agentStreamProperties.getRunResultStream();
        String group = agentStreamProperties.getResultConsumerGroup();
        // 消费组必须先存在，否则 XREADGROUP 会报 NOGROUP。
        agentStreamClient.ensureGroup(stream, group);
        agentResultStreamExecutor.execute(this::consumeLoop);
    }

    /**
     * 消费循环：读一批新消息，逐条处理并确认。
     */
    private void consumeLoop() {
        String stream = agentStreamProperties.getRunResultStream();
        String group = agentStreamProperties.getResultConsumerGroup();
        String consumer = agentStreamProperties.getResultConsumerName();

        reclaimPending(stream, group, consumer);

        while (!Thread.currentThread().isInterrupted()) {
            try {
                List<AgentStreamClient.StreamMessage> messages = agentStreamClient.read(
                        stream, group, consumer, agentStreamProperties.getResultBatchSize());
                if (messages.isEmpty()) {
                    Thread.sleep(POLL_INTERVAL_MS);
                    continue;
                }
                for (AgentStreamClient.StreamMessage message : messages) {
                    handle(stream, group, message);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } catch (Exception e) {
                log.error("消费 Python 回写结果失败", e);
                try {
                    Thread.sleep(POLL_INTERVAL_MS);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }

    /**
     * 把上一次进程崩溃遗留的未确认消息收回来重新处理。
     *
     * <p>Redis Stream 没有 RabbitMQ 那种「连接断了自动重投」的行为，
     * 未确认的消息会留在消费组的 pending 列表里，这里启动时统一 claim 一次。</p>
     */
    private void reclaimPending(String stream, String group, String consumer) {
        try {
            agentStreamClient.claimPending(stream, group, consumer, RECLAIM_MIN_IDLE_MS,
                    agentStreamProperties.getResultBatchSize());
        } catch (Exception e) {
            log.warn("回收遗留未确认消息失败，stream={}, group={}", stream, group, e);
        }
    }

    private void handle(String stream, String group, AgentStreamClient.StreamMessage message) {
        int attempts = message.retryCount() + 1;
        try {
            Map<String, Object> payload = objectMapper.readValue(message.payload(), Map.class);
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
                default -> log.warn("未知的回写动作, action={}, runId={}", action, runId);
            }
            agentStreamClient.acknowledge(stream, group, message.id());
        } catch (Exception e) {
            if (attempts < agentStreamProperties.getMaxConsumeRetries()) {
                log.warn("消费失败，第 {} 次重试", attempts, e);
                agentStreamClient.republish(stream, group, message, attempts);
            } else {
                log.error("重试 {} 次后仍失败，转投死信流", attempts, e);
                agentStreamClient.publishDeadLetter(
                        agentStreamProperties.getDeadLetterStream(), message, e.getMessage());
            }
            agentStreamClient.acknowledge(stream, group, message.id());
        }
    }
}
