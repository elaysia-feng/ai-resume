package com.elias.chat.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 会话列表项。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatSessionItemResponse {

    /**
     * 会话ID
     */
    private Long id;

    /**
     * 会话标题
     */
    private String title;

    /**
     * 最后一条消息时间
     */
    private LocalDateTime lastMessageAt;

    /**
     * 消息条数
     */
    private Long messageCount;
}
