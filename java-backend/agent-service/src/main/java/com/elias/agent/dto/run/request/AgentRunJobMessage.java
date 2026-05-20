package com.elias.agent.dto.run.request;

import com.elias.agent.dto.run.request.ClarificationAnswerRequest;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * RabbitMQ 中传递的 Agent run 任务消息。
 *
 * 注意：
 * 1. 这是 Java -> Python 的跨服务契约，字段名不要随意改。
 * 2. Python worker 会按 jobType 选择 START 或 CONTINUE 入口。
 * 3. 真正的简历详情仍由 Python bootstrap 再向 Java 拉取，这里只放任务启动所需的关键 ID 和输入。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentRunJobMessage {

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
     * 本轮只允许优化的模块 ID，v1 不拆多模块子任务。
     */
    private Long targetSectionId;

    /**
     * 用户本轮输入，Python 侧作为 Agent 初始意图。
     */
    private String userInput;

    /**
     * 当前会话 JD，避免 Python worker 消费时还要猜用户目标岗位。
     */
    private String jobDescription;

    /**
     * CONTINUE 时的追问答案；START 时通常为空。
     */
    @Builder.Default
    private List<ClarificationAnswerRequest> answers = new ArrayList<>();
}
