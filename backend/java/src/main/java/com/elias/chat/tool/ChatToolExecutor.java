package com.elias.chat.tool;

import java.util.Map;

/** 执行已注册的聊天工具并返回统一结果。 */
public interface ChatToolExecutor {

    ToolResult execute(String name, Map<String, Object> input, Long userId, String workspaceRoot);
}
