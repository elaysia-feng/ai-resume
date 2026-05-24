package com.elias.common.dto.agent.internal.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 内部 Agent run 详情响应。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InternalAgentRunDetailResponse {

    private Long runId;

    private Long userId;

    private Long sessionId;

    private Long resumeId;

    private String sceneCode;

    private String status;

    private String currentStage;

    private String jobDescription;

    private String resultSummary;

    private String errorMessage;
}
