package com.airesumeforge.common.dto.agent.internal.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiVersionSaveResponse {

    private Long versionId;

    private String versionName;
}
