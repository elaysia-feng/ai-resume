package com.elias.dto.chat.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 创建对话会话响应。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatSessionCreateResponse {

    /**
     * 新建的会话ID
     */
    private Long sessionId;
}
