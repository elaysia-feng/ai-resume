package com.airesumeforge.agent.dto.internal.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AI 版本保存响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiVersionSaveResponse {

    /**
     * 新版本ID
     */
    private Long versionId;

    /**
     * 新版本名称
     */
    private String versionName;
}
