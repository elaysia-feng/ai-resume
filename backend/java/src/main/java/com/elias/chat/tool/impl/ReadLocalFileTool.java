package com.elias.chat.tool.impl;

import com.elias.chat.tool.BinaryDetector;
import com.elias.chat.tool.ChatTool;
import com.elias.chat.tool.ToolException;
import com.elias.chat.tool.ToolExecutionContext;
import com.elias.chat.tool.ToolResult;
import com.elias.chat.tool.WorkspacePathGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 读取工作区内的文本文件。
 */
@Component
@RequiredArgsConstructor
public class ReadLocalFileTool implements ChatTool {

    public static final String NAME = "read_local_file";

    /**
     * 默认最多读取 200KB
     */
    private static final int DEFAULT_MAX_BYTES = 200 * 1024;

    /**
     * 单次读取上限，防止模型一次把内存吃满
     */
    private static final int MAX_MAX_BYTES = 1024 * 1024;

    private final WorkspacePathGuard pathGuard;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public String description() {
        return "读取工作区内的文本文件内容。路径必须位于工作区根目录内；二进制文件会被拒绝。";
    }

    @Override
    public Map<String, Object> inputSchema() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "path", Map.of("type", "string", "description", "工作区内的文件路径"),
                        "maxBytes", Map.of("type", "integer", "description", "最大读取字节数，默认 200KB")),
                "required", List.of("path"));
    }

    @Override
    public ToolResult execute(ToolExecutionContext context) {
        String rawPath = context.stringArg("path");
        int maxBytes = context.intArg("maxBytes", DEFAULT_MAX_BYTES);
        if (maxBytes <= 0 || maxBytes > MAX_MAX_BYTES) {
            maxBytes = DEFAULT_MAX_BYTES;
        }

        Path target = pathGuard.resolveExisting(rawPath, context.getWorkspaceRoot());
        if (Files.isDirectory(target)) {
            throw new ToolException("目标是一个目录，请改用 list_dir");
        }

        byte[] bytes = readBytes(target, maxBytes + 1);
        boolean truncated = bytes.length > maxBytes;
        byte[] content = truncated ? Arrays.copyOf(bytes, maxBytes) : bytes;
        if (BinaryDetector.isBinary(content)) {
            throw new ToolException("不支持读取二进制文件");
        }
        String text = new String(content, StandardCharsets.UTF_8);
        String summary = "读取了 " + content.length + " 字节"
                + (truncated ? "（内容超过 " + maxBytes + " 字节，已截断）" : "");
        String body = truncated ? text + "\n...[内容已截断]" : text;
        return ToolResult.ok(summary, body);
    }

    /**
     * 最多读取 limit 个字节
     */
    private byte[] readBytes(Path target, int limit) {
        try (InputStream inputStream = Files.newInputStream(target)) {
            return inputStream.readNBytes(limit);
        } catch (IOException e) {
            throw new ToolException("读取文件失败：" + e.getMessage());
        }
    }
}
