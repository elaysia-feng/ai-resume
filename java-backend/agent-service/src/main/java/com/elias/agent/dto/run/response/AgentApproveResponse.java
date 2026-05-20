package com.elias.agent.dto.run.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Agent patch 应用响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentApproveResponse {

    /**
     * run ID
     */
    private Long runId;

    /**
     * 应用后的 run 状态
     */
    private String status;

    /**
     * 关联简历ID
     */
    private Long resumeId;

    /**
     * 实际应用的 patch 数量
     */
    private Integer appliedPatchCount;

    /**
     * 新保存的 AI 版本ID
     */
    private Long versionId;

    /**
     * 新保存的 AI 版本名称
     */
    private String versionName;
}
