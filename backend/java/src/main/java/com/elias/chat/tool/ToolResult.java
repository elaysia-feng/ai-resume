package com.elias.chat.tool;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 工具执行结果。
 *
 * <p>summary 只给前端（推 SSE 的 tool_result 事件），content 才回喂给模型。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToolResult {

    /**
     * 是否执行成功
     */
    private boolean ok;

    /**
     * 简短摘要，给前端展示
     */
    private String summary;

    /**
     * 完整内容，回喂给模型
     */
    private String content;

    public static ToolResult ok(String summary, String content) {
        return ToolResult.builder().ok(true).summary(summary).content(content).build();
    }

    public static ToolResult fail(String message) {
        return ToolResult.builder().ok(false).summary(message).content(message).build();
    }
}
