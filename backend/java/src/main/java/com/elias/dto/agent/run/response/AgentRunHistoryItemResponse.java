package com.elias.dto.agent.run.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 会话下的 Agent run 历史摘要。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentRunHistoryItemResponse {

    /** 任务标识。 */
    private Long runId;
    /** 所属会话标识。 */
    private Long sessionId;
    /** 关联简历标识。 */
    private Long resumeId;
    /** 任务场景编码。 */
    private String sceneCode;
    /** 任务状态。 */
    private String status;
    /** 当前或结束时的业务阶段。 */
    private String currentStage;
    /** 任务结果摘要。 */
    private String resultSummary;
    /** 创建时间。 */
    private LocalDateTime createdAt;
    /** 最近更新时间。 */
    private LocalDateTime updatedAt;
    /** 完成时间；未结束时为空。 */
    private LocalDateTime completedAt;
}
