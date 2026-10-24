package com.elias.agent.dto.run.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Agent run 事件响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentEventResponse {

    /**
     * 事件ID
     */
    private Long id;

    /**
     * 所属 run ID
     */
    private Long runId;

    /**
     * run 内递增事件序号
     */
    private Integer eventSeq;

    /**
     * 事件类型
     */
    private String eventType;

    /**
     * 阶段编码
     */
    private String stageCode;

    /**
     * 展示消息
     */
    private String message;

    /**
     * 事件附加数据
     */
    private Map<String, Object> payload;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}
