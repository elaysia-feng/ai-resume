package com.elias.interview.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class InterviewAnswerResponse {
    /**
     * 题目轮次记录ID
     */
    private Long roundId;

    /**
     * 当前状态
     */
    private String status;
}
