package com.elias.dto.chat.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 会话内的一条消息。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageResponse {

    /**
     * 消息ID
     */
    private Long id;

    /**
     * 角色：USER / ASSISTANT / SYSTEM / TOOL
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
     * 工具名称，仅工具消息有值
     */
    private String toolName;

    /**
     * 会话内顺序号
     */
    private Integer seqNo;

    /**
     * 消息状态：SUCCESS / FAILED
     */
    private String status;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}
