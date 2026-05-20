package com.elias.agent.dto.run.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 确认应用 Agent patch 请求
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AgentApproveRequest {

    /**
     * 用户确认应用的 patchId 列表；为空时可由 service 解释为全部应用
     */
    private List<String> approvedPatchIds;

    /**
     * 用户拒绝应用的 patchId 列表
     */
    private List<String> rejectedPatchIds;

    /**
     * 用户审批备注
     */
    private String approvalComment;

    /**
     * 客户端幂等请求ID
     */
    private String clientRequestId;
}
