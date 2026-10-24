package com.elias.interview.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 当前题目响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterviewQuestionResponse {

    /**
     * 轮次记录ID
     */
    private Long roundId;

    /**
     * 第几轮问题，从1开始
     */
    private Integer roundNo;

    /**
     * 题干
     */
    private String questionText;

    /**
     * 选项列表，按顺序对应 A/B/C/D
     */
    private List<InterviewOptionResponse> options;

    /**
     * 用户本轮完整回答，JSON 字符串
     */
    private String userAnswer;

    /**
     * 当前题目状态：WAITING_ANSWER / ANSWERED / FINISHED
     */
    private String status;
}
