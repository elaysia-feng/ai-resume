package com.airesumeforge.interview.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 历史轮次响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterviewRoundResponse {

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
     * Python 对本轮回答的分析结果
     */
    private String analysis;

    /**
     * 当前轮次状态：WAITING_ANSWER / ANSWERED / FINISHED
     */
    private String status;
}
