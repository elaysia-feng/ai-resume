package com.airesumeforge.common.dto.agent.internal.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiVersionSaveRequest {

    @NotNull(message = "runId 不能为空")
    private Long runId;

    private String versionName;

    private String summary;
}
