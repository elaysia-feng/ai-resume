package com.elias.agent.dto.session.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Agent会话列表项响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentSessionItemResponse {

    /**
     * 会话ID
     */
    private Long id;

    /**
     * 当前会话关联的简历ID
     */
    private Long resumeId;

    /**
     * 会话场景编码，例如 JD_CUSTOMIZE
     */
    private String sceneCode;

    /**
     * 会话标题，用于前端会话列表展示
     */
    private String sessionTitle;

    /**
     * 当前会话复用的目标岗位JD，前端切换会话时可直接回填
     */
    private String jobDescription;

    /**
     * 派生来源会话ID，New Session 复制JD时记录来源
     */
    private Long parentSessionId;

    /**
     * 会话状态：ACTIVE / ARCHIVED / DELETED
     */
    private String status;

    /**
     * 当前会话下仍在进行中的 run ID，用于前端刷新后恢复看板。
     */
    private Long activeRunId;

    /**
     * 当前活跃 run 状态。
     */
    private String activeRunStatus;

    /**
     * 最后一条消息时间，用于会话列表排序
     */
    private LocalDateTime lastMessageAt;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
