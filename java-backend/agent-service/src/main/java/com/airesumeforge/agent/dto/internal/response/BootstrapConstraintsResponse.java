package com.airesumeforge.agent.dto.internal.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Agent 启动约束
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BootstrapConstraintsResponse {

    /**
     * 是否允许 Agent 创建新模块
     */
    private Boolean allowCreateNewSection;

    /**
     * 是否允许 Agent 删除模块
     */
    private Boolean allowDeleteSection;

    /**
     * 允许的 patch 操作类型列表
     */
    private List<String> allowedPatchOperation;
}
