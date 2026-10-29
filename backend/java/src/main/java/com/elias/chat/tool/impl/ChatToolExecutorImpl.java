package com.elias.chat.tool.impl;
import com.elias.chat.tool.ChatToolExecutor;
import com.elias.chat.tool.ChatTool;
import com.elias.chat.tool.ToolExecutionContext;
import com.elias.chat.tool.ToolException;
import com.elias.chat.tool.ToolRegistry;
import com.elias.chat.tool.ToolResult;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 工具执行器。
 *
 * <p>按名字分发到具体工具，并把异常统一转成失败的 {@link ToolResult}，
 * 让模型能读到失败原因并自己纠正，而不是让整轮对话崩掉。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatToolExecutorImpl implements ChatToolExecutor {

    private final ToolRegistry toolRegistry;

    /**
     * 执行一个工具调用
     *
     * @param name          工具名
     * @param input         工具入参
     * @param userId        当前用户ID
     * @param workspaceRoot 工作区根目录，可为空
     * @return 工具执行结果
     */
    public ToolResult execute(String name, Map<String, Object> input, Long userId, String workspaceRoot) {
        ChatTool tool = toolRegistry.find(name);
        if (tool == null) {
            return ToolResult.fail("未知工具：" + name);
        }
        ToolExecutionContext context = ToolExecutionContext.builder()
                .userId(userId)
                .workspaceRoot(workspaceRoot)
                .input(input == null ? Map.of() : input)
                .build();
        try {
            ToolResult result = tool.execute(context);
            return result == null ? ToolResult.fail("工具没有返回结果") : result;
        } catch (ToolException e) {
            return ToolResult.fail(e.getMessage());
        } catch (Exception e) {
            log.warn("工具执行失败, tool={}", name, e);
            return ToolResult.fail("工具执行失败：" + e.getMessage());
        }
    }
}
