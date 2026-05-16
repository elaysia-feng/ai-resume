package com.airesumeforge.agent.dto.run.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Agent run 详情响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentRunDetailResponse {

    /**
     * run ID
     */
    private Long runId;

    /**
     * 所属 Agent 会话ID
     */
    private Long sessionId;

    /**
     * 关联简历ID
     */
    private Long resumeId;

    /**
     * 场景编码
     */
    private String sceneCode;

    /**
     * run 当前状态
     */
    private String status;

    /**
     * 当前阶段编码
     */
    private String currentStage;

    /**
     * 结果摘要
     */
    private String resultSummary;

    /**
     * 追问 payload
     */
    private Map<String, Object> clarificationPayload;

    /**
     * 审批 payload
     */
    private Map<String, Object> approvalPayload;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    /**
     * 完成时间
     */
    private LocalDateTime completedAt;
}
