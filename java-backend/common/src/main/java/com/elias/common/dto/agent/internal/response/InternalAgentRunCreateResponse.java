package com.elias.common.dto.agent.internal.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 内部创建 Agent run 响应。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InternalAgentRunCreateResponse {

    private Long runId;

    private String status;
}
