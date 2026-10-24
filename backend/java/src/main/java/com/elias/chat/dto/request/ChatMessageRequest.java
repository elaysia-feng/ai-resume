package com.elias.chat.dto.request;

import lombok.Data;

/**
 * 发送消息请求。
 */
@Data
public class ChatMessageRequest {

    /**
     * 用户输入
     */
    private String content;

    /**
     * 工作区根目录，可为空；为空时沿用用户已保存的工作区
     */
    private String workspacePath;
}
