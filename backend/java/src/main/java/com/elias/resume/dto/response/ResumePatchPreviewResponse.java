package com.elias.resume.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 简历 patch 预览响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResumePatchPreviewResponse {

    /**
     * 简历ID
     */
    private Long resumeId;

    /**
     * patch 预览列表，包含 before / after / diff 等 service 组装字段
     */
    private List<Map<String, Object>> previews;
}
