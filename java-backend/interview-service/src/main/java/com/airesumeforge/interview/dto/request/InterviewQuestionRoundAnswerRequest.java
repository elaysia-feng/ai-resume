package com.airesumeforge.interview.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 面试题目答案提交请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterviewQuestionRoundAnswerRequest {

    /**
     * 用户本轮完整回答，JSON 字符串
     * 例如: {"selectedOption": "A", "supplementText": "因为..."}
     */
    private String userAnswer;

    /**
     * 题目状态，提交答案后固定为 ANSWERED
     */
    private String status;
}
