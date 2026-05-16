package com.airesumeforge.agent.dto.run.request;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 继续 Agent run 请求
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AgentContinueRequest {

    /**
     * 用户对 Clarifier 追问的回答列表
     */
    @Valid
    private List<ClarificationAnswerRequest> answers = new ArrayList<>();

    /**
     * 客户端幂等请求ID
     */
    private String clientRequestId;
}
