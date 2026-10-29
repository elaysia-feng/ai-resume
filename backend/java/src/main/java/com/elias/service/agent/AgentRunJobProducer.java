package com.elias.service.agent;

import com.elias.dto.agent.run.request.AgentRunJobMessage;
import com.elias.dto.common.interview.internal.request.InterviewAgentRunJobMessage;

/** 将简历与模拟面试 run 投递到 Python worker 队列。 */
public interface AgentRunJobProducer {

    void publish(AgentRunJobMessage message);

    void publish(InterviewAgentRunJobMessage message);
}
