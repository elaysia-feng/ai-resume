package com.airesumeforge.common.dto.interview.internal.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class InternalInterviewBootstrapRequest {

    @NotNull(message = "runId 不能为空")
    private Long runId;

    @NotNull(message = "sessionId 不能为空")
    private Long sessionId;

    @NotNull(message = "resumeId 不能为空")
    private Long resumeId;

    private String sceneCode;
}
