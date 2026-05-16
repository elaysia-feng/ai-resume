package com.airesumeforge.dto.agent.internal.response;

import com.airesumeforge.dto.resume.response.ResumeSnapshotResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Python Agent 启动上下文响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InternalBootstrapResponse {

    /**
     * run ID
     */
    private Long runId;

    /**
     * 会话ID
     */
    private Long sessionId;

    /**
     * 简历快照
     */
    private ResumeSnapshotResponse resume;

    /**
     * 当前会话复用的目标岗位JD
     */
    private String jobDescription;

    /**
     * 当前会话长期记忆摘要
     */
    private String summary;

    /**
     * sectionCode -> JSON schema
     */
    private Map<String, Map<String, Object>> schemas;

    /**
     * 最近历史消息
     */
    private List<HistoryMessageResponse> messages;

    /**
     * 允许 Agent 编辑的 sectionId 列表
     */
    private List<Long> editableSectionIds;

    /**
     * 本轮 Agent 约束
     */
    private BootstrapConstraintsResponse constraints;
}


