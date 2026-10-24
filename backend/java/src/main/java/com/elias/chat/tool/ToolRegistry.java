package com.elias.chat.tool;

import com.elias.common.minimax.MiniMaxTool;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 工具注册表。
 *
 * <p>所有 {@link ChatTool} 实现按名字注册进来，
 * 对话循环统一从这里取工具声明并按名字分发。</p>
 */
@Component
public class ToolRegistry {

    /**
     * 工具名 -> 工具实现
     */
    private final Map<String, ChatTool> tools;

    public ToolRegistry(List<ChatTool> chatTools) {
        Map<String, ChatTool> registry = new LinkedHashMap<>();
        for (ChatTool tool : chatTools) {
            registry.put(tool.name(), tool);
        }
        this.tools = Collections.unmodifiableMap(registry);
    }

    /**
     * 按名字找工具
     */
    public ChatTool find(String name) {
        return name == null ? null : tools.get(name);
    }

    /**
     * 转成协议层的工具声明列表
     */
    public List<MiniMaxTool> toAnthropicTools() {
        return tools.values().stream().map(ChatTool::toAnthropicTool).toList();
    }
}
