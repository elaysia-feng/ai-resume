package com.elias.agent.dto.run.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Agent run 入队响应。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentRunQueueResponse {

    /**
     * Java 侧 run ID，前端后续用它订阅事件流。
     */
    private Long runId;

    /**
     * 当前状态，创建或继续后通常是 QUEUED。
     */
    private String status;
}
