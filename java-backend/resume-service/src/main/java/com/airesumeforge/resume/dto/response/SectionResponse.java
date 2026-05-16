package com.airesumeforge.resume.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 简历模块响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SectionResponse {

    private Long id;
    private Long resumeId;
    private String sectionCode;
    private String sectionTitle;
    private String sectionType;
    private String schemaType;
    private String contentJson;
    private Boolean visible;
    private Integer sortOrder;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
