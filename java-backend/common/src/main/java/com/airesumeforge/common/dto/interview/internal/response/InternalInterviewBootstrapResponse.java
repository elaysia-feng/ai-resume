package com.airesumeforge.common.dto.interview.internal.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class InternalInterviewBootstrapResponse {

    private Long runId;

    private Long sessionId;

    /** 简历快照，类型由调用方决定 */
    private Object resume;

    private String jobDescription;

    private String summary;
}
