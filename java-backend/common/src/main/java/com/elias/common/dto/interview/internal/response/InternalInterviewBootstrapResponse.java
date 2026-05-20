package com.elias.common.dto.interview.internal.response;

import com.elias.common.dto.response.ResumeSnapshotResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 面试 Agent 启动上下文响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InternalInterviewBootstrapResponse {

    /**
     * run ID
     */
    private Long runId;

    /**
     * 会话ID
     */
    private Long sessionId;

    /**
     * 当前会话复用的目标岗位JD
     */
    private String jobDescription;

    /**
     * 简历快照
     */
    private ResumeSnapshotResponse resume;

    /**
     * 当前会话长期记忆摘要
     */
    private String summary;
}