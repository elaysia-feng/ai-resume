package com.elias.agent.dto.session.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Agent消息响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentMessageResponse {

    /**
     * 消息ID
     */
    private Long id;

    /**
     * 所属 Agent 会话ID
     */
    private Long sessionId;

    /**
     * 消息角色：USER / ASSISTANT / SYSTEM / TOOL
     */
    private String role;

    /**
     * 消息正文
     */
    private String content;

    /**
     * 内容类型：TEXT / JSON
     */
    private String contentType;

    /**
     * 会话内顺序号
     */
    private Integer seqNo;

    /**
     * 消息状态：SUCCESS / FAILED
     */
    private String status;

    /**
     * 工具名称；普通对话消息为空
     */
    private String toolName;

    /**
     * 扩展信息 JSON，可记录 token、模型名、原始返回等
     */
    private String extraJson;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}
