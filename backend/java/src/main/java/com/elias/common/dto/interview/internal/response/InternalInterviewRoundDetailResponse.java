package com.elias.common.dto.interview.internal.response;

import com.elias.common.dto.interview.response.InterviewOptionResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 面试题目答案详情响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InternalInterviewRoundDetailResponse {

    /**
     * 题目记录ID
     */
    private Long roundId;

    /**
     * run ID
     */
    private Long runId;

    /**
     * 轮次编号
     */
    private Integer roundNo;

    /**
     * 题干
     */
    private String questionText;

    /**
     * 选项列表
     */
    private List<InterviewOptionResponse> options;

    /**
     * 用户回答
     */
    private String userAnswer;

    /**
     * 分析结果 JSON
     */
    private String analysisJson;

    /**
     * 状态
     */
    private String status;
}