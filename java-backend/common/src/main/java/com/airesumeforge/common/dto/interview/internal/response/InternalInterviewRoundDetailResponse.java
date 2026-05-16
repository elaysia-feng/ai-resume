package com.airesumeforge.common.dto.interview.internal.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class InternalInterviewRoundDetailResponse {
    private Long roundId;
    private Long runId;
    private Integer roundNo;
    private String questionText;
    private List<Object> options;
    private String userAnswer;
    private String status;
    private String analysisJson;
}
