package com.airesumeforge.dto.resume.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 简历 patch 应用响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResumePatchApplyResponse {

    /**
     * run ID
     */
    private Long runId;

    /**
     * 简历ID
     */
    private Long resumeId;

    /**
     * 实际应用的 patch 数量
     */
    private Integer appliedPatchCount;
}

