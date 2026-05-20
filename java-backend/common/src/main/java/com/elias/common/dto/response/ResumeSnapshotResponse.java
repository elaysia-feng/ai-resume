package com.elias.common.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Agent 使用的简历快照
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResumeSnapshotResponse {

    /**
     * 简历ID
     */
    private Long id;

    /**
     * 简历标题
     */
    private String title;

    /**
     * 简历模板
     */
    private String template;

    /**
     * 简历模块快照
     */
    private List<SectionResponse> sections;
}