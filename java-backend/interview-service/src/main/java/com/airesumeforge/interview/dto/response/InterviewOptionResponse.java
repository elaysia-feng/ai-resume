package com.airesumeforge.interview.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 面试选项响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterviewOptionResponse {

    /**
     * 选项标识，例如 A / B / C / D
     */
    private String key;

    /**
     * 选项内容
     */
    private String text;
}
