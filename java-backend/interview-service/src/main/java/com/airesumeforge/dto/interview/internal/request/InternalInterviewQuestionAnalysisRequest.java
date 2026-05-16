package com.airesumeforge.dto.interview.internal.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class InternalInterviewQuestionAnalysisRequest {

    /**
     * Python 对本轮回答的分析结果
     */
    private Object analysis;

    /**
     * 当前轮次状态：ANALYZED / FINISHED
     */
    private String status;
}
