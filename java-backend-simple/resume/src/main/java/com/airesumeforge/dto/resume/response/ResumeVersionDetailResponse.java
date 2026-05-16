package com.airesumeforge.dto.resume.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 简历版本详情响应
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResumeVersionDetailResponse {

    private Long id;
    private Long resumeId;
    private Integer versionNo;
    private String versionName;
    private String resumeTitle;
    private String resumeTemplate;
    private String source;
    private LocalDateTime createdAt;
    private List<ResumeVersionSectionResponse> sections;
}

