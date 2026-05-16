package com.airesumeforge.dto.interview.internal.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author 爱门
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class InternalInterviewBootstrapRequest {
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

}
