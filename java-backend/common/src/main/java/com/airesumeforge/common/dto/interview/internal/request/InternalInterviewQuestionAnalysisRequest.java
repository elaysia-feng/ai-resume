package com.airesumeforge.common.dto.interview.internal.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class InternalInterviewQuestionAnalysisRequest {

    private Object analysis;

    private String status;
}
