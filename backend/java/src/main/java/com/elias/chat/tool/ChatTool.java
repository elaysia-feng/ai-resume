package com.elias.chat.tool;

import com.elias.common.minimax.MiniMaxTool;

import java.util.Map;

/**
 * 对话可用的本地工具。
 *
 * <p>每个工具只负责一件事：校验参数 + 读文件 + 返回结果。
 * 路径安全由 {@link WorkspacePathGuard} 统一兜底。</p>
 */
public interface ChatTool {

    /**
     * 工具名，模型通过它发起调用
     */
    String name();

    /**
     * 工具描述，告诉模型这个工具能干什么
     */
    String description();

    /**
     * 入参 JSON Schema
     */
    Map<String, Object> inputSchema();

    /**
     * 执行工具
     */
    ToolResult execute(ToolExecutionContext context);

    /**
     * 转成协议层的工具声明
     */
    default MiniMaxTool toAnthropicTool() {
        return new MiniMaxTool(name(), description(), inputSchema());
    }
}
