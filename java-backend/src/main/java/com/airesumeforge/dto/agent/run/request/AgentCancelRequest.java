package com.airesumeforge.dto.agent.run.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 取消 Agent run 请求
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AgentCancelRequest {

    /**
     * 客户端幂等请求ID
     */
    private String clientRequestId;
}

