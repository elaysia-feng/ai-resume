package com.airesumeforge.interview.dto.internal.request;

import com.airesumeforge.interview.dto.response.InterviewOptionResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Python 创建面试题请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InternalInterviewQuestionCreateRequest {

    /**
     * 题干
     */
    private String questionText;

    /**
     * 选项列表，按顺序对应 A/B/C/D
     */
    private List<InterviewOptionResponse> options;
}
