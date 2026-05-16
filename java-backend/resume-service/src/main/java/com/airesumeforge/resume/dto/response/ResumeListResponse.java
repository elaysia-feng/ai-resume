package com.airesumeforge.resume.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResumeListResponse {
    // 简历的序号
    private Long id;
    // 简历的名称
    private String title;

    // 简历的模版类型
    private String template;
    // 简历最后一次更新时间
    private LocalDateTime updatedAt;
}
