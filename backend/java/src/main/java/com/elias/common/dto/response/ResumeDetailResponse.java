package com.elias.common.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 简历详情响应（含所有模块）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResumeDetailResponse {
    private Long id;
    private String userId;
    private String title;
    private String template;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<SectionResponse> sections;
}
