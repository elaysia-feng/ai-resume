package com.airesumeforge.dto.interview.internal.request;

import com.airesumeforge.agent.dto.run.request.ClarificationAnswerRequest;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterviewAgentRunJobMessage {

    /**
     * START：新 run；CONTINUE：用户补充信息后继续旧 run。
     */
    private String jobType;

    /**
     * Java ai_agent_run 主键，也是 LangGraph thread_id 的来源。
     */
    private Long runId;

    /**
     * 会话 ID，用于 Python bootstrap 拉取历史消息和 JD。
     */
    private Long sessionId;

    /**
     * 简历 ID，用于 Python bootstrap 拉取简历快照。
     */
    private Long resumeId;

    /**
     * 场景编码，例如 JD_CUSTOMIZE。
     */
    private String sceneCode;

    /**
     * 当前会话 JD，避免 Python worker 消费时还要猜用户目标岗位。
     */
    private String jobDescription;

}
