package com.elias.agent.service;

import com.elias.agent.dto.run.request.AgentRunJobMessage;
import com.elias.common.dto.interview.internal.request.InterviewAgentRunJobMessage;
import com.elias.common.stream.AgentStreamClient;
import com.elias.common.stream.AgentStreamProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Agent run 任务队列生产者。
 * 这里只做一件事：把已经落库的 run 投递到任务队列（agent_task 表）。
 * run 的创建、状态修改、事务边界都在 AgentRunServiceImpl 里控制。
 *
 * <p>原来投递到 RabbitMQ 的 exchange + routingKey，现在按任务流区分：
 * 简历优化走 resume 流，面试模拟走 interview 流，
 * START / CONTINUE 的区分放进消息体的 jobType 字段。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentRunJobProducer {

    private final AgentStreamClient agentStreamClient;
    private final AgentStreamProperties agentStreamProperties;

    /**
     * 投递简历优化任务。
     */
    public void publish(AgentRunJobMessage message) {
        String stream = agentStreamProperties.getResumeRunStream();
        Long taskId = agentStreamClient.publish(stream, message);
        log.info("投递简历优化任务成功 - stream: {}, taskId: {}, jobType: {}, runId: {}",
                stream, taskId, message.getJobType(), message.getRunId());
    }

    /**
     * 投递面试模拟任务。
     */
    public void publish(InterviewAgentRunJobMessage message) {
        String stream = agentStreamProperties.getInterviewRunStream();
        Long taskId = agentStreamClient.publish(stream, message);
        log.info("投递面试模拟任务成功 - stream: {}, taskId: {}, jobType: {}, runId: {}",
                stream, taskId, message.getJobType(), message.getRunId());
    }
}
