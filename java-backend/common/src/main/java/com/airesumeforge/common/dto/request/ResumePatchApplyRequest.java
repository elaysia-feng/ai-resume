package com.airesumeforge.common.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 简历 patch 应用请求
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResumePatchApplyRequest {

    /**
     * 当前 patch 来源 run ID
     */
    @NotNull(message = "runId 不能为空")
    private Long runId;

    /**
     * 需要实际应用的 patch 列表
     */
    @Valid
    private List<ResumeSectionPatchRequest> patches = new ArrayList<>();
}
