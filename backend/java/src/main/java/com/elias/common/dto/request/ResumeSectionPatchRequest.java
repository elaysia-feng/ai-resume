package com.elias.common.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 简历 section patch 请求
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResumeSectionPatchRequest {

    /**
     * patch 唯一标识，用于前端确认或拒绝
     */
    @NotBlank(message = "patchId 不能为空")
    private String patchId;

    /**
     * 要修改的简历模块ID
     */
    @NotNull(message = "sectionId 不能为空")
    private Long sectionId;

    /**
     * 模块编码，例如 PROJECTS / EXPERIENCE
     */
    private String sectionCode;

    /**
     * 模块标题，用于前端展示
     */
    private String sectionTitle;

    /**
     * patch 操作类型，P1 固定为 REPLACE_SECTION_CONTENT
     */
    @NotBlank(message = "operation 不能为空")
    private String operation;

    /**
     * Agent 生成该 patch 的原因
     */
    private String reason;

    /**
     * 修改前内容，用于冲突检测和 diff 展示
     */
    private Map<String, Object> beforeJson;

    /**
     * 修改后内容，确认后写入 resume_sections.content_json
     */
    private Map<String, Object> afterJson;

    /**
     * 本条 patch 的变化摘要
     */
    private String changeSummary;

    /**
     * 风险等级：LOW / MEDIUM / HIGH
     */
    private String riskLevel;
}
