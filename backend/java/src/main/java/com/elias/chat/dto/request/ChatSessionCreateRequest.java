package com.elias.chat.dto.request;

import lombok.Data;

/**
 * 创建对话会话请求。
 */
@Data
public class ChatSessionCreateRequest {

    /**
     * 会话标题，可为空，为空时用首条消息摘要兜底
     */
    private String title;

    /**
     * 关联简历ID，可为空
     */
    private Long resumeId;
}
