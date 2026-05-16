package com.airesumeforge.dto.agent.internal.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Agent run 单条事件请求
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RunEventRequest {

    /**
     * 当前 run 内递增的事件序号
     */
    @NotNull(message = "eventSeq 不能为空")
    private Integer eventSeq;

    /**
     * 事件类型，例如 run.started / stage.changed
     */
    @NotBlank(message = "eventType 不能为空")
    private String eventType;

    /**
     * 所属 run ID，通常由 path 或 payload 共同确认
     */
    private Long runId;

    /**
     * 所属 Agent 会话ID
     */
    private Long sessionId;

    /**
     * 当前阶段编码，例如 BOOTSTRAP / REWRITER
     */
    private String stageCode;

    /**
     * 前端可展示的简短事件说明
     */
    private String message;

    /**
     * 事件附加业务数据
     */
    private Map<String, Object> payload;
}

