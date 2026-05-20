package com.elias.agent.dto.run.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 启动 Agent run 请求
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AgentRunStartRequest {

    /**
     * Agent 场景编码，例如 JD_CUSTOMIZE
     */
    @NotBlank(message = "sceneCode 不能为空")
    private String sceneCode;

    /**
     * 本次 Agent 要处理的简历ID
     */
    @NotNull(message = "resumeId 不能为空")
    private Long resumeId;

    /**
     * 用户输入的自然语言任务描述
     */
    private String userInput;

    /**
     * 目标岗位 JD，可为空；为空时 Agent 可能进入追问
     */
    private String jobDescription;

    /**
     * 本次只允许 Agent 处理的目标简历模块ID
     */
    @NotNull(message = "targetSectionId 不能为空")
    private Long targetSectionId;

    /**
     * 客户端幂等请求ID
     */
    private String clientRequestId;
}
