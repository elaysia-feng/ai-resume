package com.airesumeforge.common.dto.interview.internal.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterviewAgentRunJobMessage {

    private String jobType;

    private Long runId;

    private Long sessionId;

    private Long resumeId;

    private String sceneCode;

    private String jobDescription;
}
