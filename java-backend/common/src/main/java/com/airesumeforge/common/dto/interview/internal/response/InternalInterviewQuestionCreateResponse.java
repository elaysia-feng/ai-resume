package com.airesumeforge.common.dto.interview.internal.response;

import com.airesumeforge.common.dto.interview.response.InterviewOptionResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 面试题目创建响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InternalInterviewQuestionCreateResponse {

    /**
     * 题目记录ID
     */
    private Long roundId;

    /**
     * 轮次编号
     */
    private Integer roundNo;
}