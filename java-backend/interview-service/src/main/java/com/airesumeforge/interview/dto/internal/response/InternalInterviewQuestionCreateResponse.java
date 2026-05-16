package com.airesumeforge.interview.dto.internal.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class InternalInterviewQuestionCreateResponse {

    /**
     * 题目第几轮轮次
     */
    private Long roundId;

    /**
     * 第几轮问题，从1开始
     */
    private Integer roundNo;
}
