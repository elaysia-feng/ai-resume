package com.airesumeforge.resume.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 简历版本列表响应
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResumeVersionItemResponse {

    private Long id;
    private Integer versionNo;
    private String versionName;
    private String source;
    private LocalDateTime createdAt;
}
