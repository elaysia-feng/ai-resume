package com.airesumeforge.agent.dto.internal.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Agent 使用的历史消息快照
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HistoryMessageResponse {

    /**
     * 消息角色：USER / ASSISTANT / SYSTEM / TOOL
     */
    private String role;

    /**
     * 消息正文
     */
    private String content;
}
