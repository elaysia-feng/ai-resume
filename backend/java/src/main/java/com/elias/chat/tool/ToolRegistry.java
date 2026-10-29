package com.elias.chat.tool;

import com.elias.chat.minimax.MiniMaxTool;

import java.util.List;

/** 当前会话可用工具的查询与模型协议转换契约。 */
public interface ToolRegistry {

    ChatTool find(String name);

    List<MiniMaxTool> toAnthropicTools();
}
