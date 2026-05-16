package com.airesumeforge.resume.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 简历版本模块响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResumeVersionSectionResponse {

    private Long id;
    private Long versionId;
    private String sectionCode;
    private String sectionTitle;
    private String sectionType;
    private String schemaType;
    private String contentJson;
    private Boolean visible;
    private Integer sortOrder;
    private LocalDateTime createdAt;
}
