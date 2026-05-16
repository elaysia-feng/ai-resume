package com.airesumeforge.agent.dto.internal.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 保存 AI 简历版本请求
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiVersionSaveRequest {

    /**
     * 当前 AI 版本来源 run ID
     */
    @NotNull(message = "runId 不能为空")
    private Long runId;

    /**
     * AI 版本名称
     */
    private String versionName;

    /**
     * AI 修改摘要
     */
    private String summary;
}
