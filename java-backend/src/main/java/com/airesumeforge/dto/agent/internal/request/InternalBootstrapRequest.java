package com.airesumeforge.dto.agent.internal.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Python 请求 Java 加载 Agent 启动上下文
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InternalBootstrapRequest {

    /**
     * Java 侧 ai_agent_run 主键
     */
    @NotNull(message = "runId 不能为空")
    private Long runId;

    /**
     * 当前 run 所属 Agent 会话ID
     */
    @NotNull(message = "sessionId 不能为空")
    private Long sessionId;

    /**
     * 当前 run 关联的简历ID
     */
    @NotNull(message = "resumeId 不能为空")
    private Long resumeId;

    /**
     * 场景编码，例如 JD_CUSTOMIZE
     */
    private String sceneCode;

    /**
     * 本次只允许 Agent 修改的目标模块ID
     */
    @NotNull(message = "targetSectionId 不能为空")
    private Long targetSectionId;
}

