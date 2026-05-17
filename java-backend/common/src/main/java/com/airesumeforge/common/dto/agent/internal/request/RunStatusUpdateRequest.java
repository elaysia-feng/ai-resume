package com.airesumeforge.common.dto.agent.internal.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Python 回写 Agent run 状态请求
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RunStatusUpdateRequest {

    /**
     * run 状态：RUNNING / WAITING_USER / WAITING_CONFIRM / FAILED 等
     */
    @NotBlank(message = "status 不能为空")
    private String status;

    /**
     * 当前阶段编码
     */
    private String currentStage;

    /**
     * 当前 run 的结果摘要
     */
    private String resultSummary;

    /**
     * Clarifier 追问 payload
     */
    private Map<String, Object> clarificationPayload;

    /**
     * ApprovalPackager 审批 payload
     */
    private Map<String, Object> approvalPayload;

    /**
     * 失败时的错误信息
     */
    private String errorMessage;
}