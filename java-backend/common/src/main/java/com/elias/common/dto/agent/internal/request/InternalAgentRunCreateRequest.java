package com.elias.common.dto.agent.internal.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 内部创建 Agent run 请求。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InternalAgentRunCreateRequest {

    private Long userId;

    private Long sessionId;

    private Long resumeId;

    private String sceneCode;

    private String status;

    private String currentStage;

    private String userInput;

    private String jobDescription;

    private Long targetSectionId;

    private String clientRequestId;
}
