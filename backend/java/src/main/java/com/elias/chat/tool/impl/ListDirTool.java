package com.elias.chat.tool.impl;

import com.elias.chat.tool.ChatTool;
import com.elias.chat.tool.ToolException;
import com.elias.chat.tool.ToolExecutionContext;
import com.elias.chat.tool.ToolResult;
import com.elias.chat.tool.WorkspacePathGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.Map;

/**
 * 列出工作区内的目录内容。
 */
@Component
@RequiredArgsConstructor
public class ListDirTool implements ChatTool {

    public static final String NAME = "list_dir";

    /**
     * 默认最多返回 500 条
     */
    private static final int DEFAULT_MAX_ENTRIES = 500;

    /**
     * 递归时的最大深度
     */
    private static final int MAX_DEPTH = 8;

    private final WorkspacePathGuard pathGuard;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public String description() {
        return "列出工作区内的目录内容。路径必须位于工作区根目录内；默认非递归，最多 500 条。";
    }

    @Override
    public Map<String, Object> inputSchema() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "path", Map.of("type", "string", "description", "工作区内的目录路径，缺省为工作区根目录"),
                        "recursive", Map.of("type", "boolean", "description", "是否递归，默认 false"),
                        "maxEntries", Map.of("type", "integer", "description", "最多返回条目数，默认 500")),
                "required", List.of());
    }

    @Override
    public ToolResult execute(ToolExecutionContext context) {
        String rawPath = context.stringArg("path");
        boolean recursive = context.booleanArg("recursive", false);
        int maxEntries = context.intArg("maxEntries", DEFAULT_MAX_ENTRIES);
        if (maxEntries <= 0 || maxEntries > DEFAULT_MAX_ENTRIES) {
            maxEntries = DEFAULT_MAX_ENTRIES;
        }

        Path root = pathGuard.resolveExisting(rawPath, context.getWorkspaceRoot());
        if (!Files.isDirectory(root)) {
            throw new ToolException("目标不是目录");
        }

        List<String> lines = new ArrayList<>();
        if (recursive) {
            collectRecursive(root, lines, maxEntries);
        } else {
            collectDirect(root, lines, maxEntries);
        }

        String summary = "列出 " + lines.size() + " 个条目";
        String body = lines.isEmpty() ? "(目录为空)" : String.join("\n", lines);
        return ToolResult.ok(summary, body);
    }

    /**
     * 列出直接子项
     */
    private void collectDirect(Path root, List<String> lines, int maxEntries) {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(root)) {
            List<Path> children = new ArrayList<>();
            stream.forEach(children::add);
            children.sort(Comparator.comparing(path -> path.getFileName().toString().toLowerCase()));
            for (Path child : children) {
                if (lines.size() >= maxEntries) {
                    break;
                }
                if (Files.isDirectory(child) && pathGuard.isExcludedDir(child.getFileName().toString())) {
                    continue;
                }
                lines.add(describe(child));
            }
        } catch (IOException e) {
            throw new ToolException("读取目录失败：" + e.getMessage());
        }
    }

    /**
     * 递归收集，跳过排除目录
     */
    private void collectRecursive(Path root, List<String> lines, int maxEntries) {
        Deque<Path> pending = new ArrayDeque<>();
        pending.push(root);
        while (!pending.isEmpty() && lines.size() < maxEntries) {
            Path current = pending.pop();
            List<Path> children = new ArrayList<>();
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(current)) {
                stream.forEach(children::add);
            } catch (IOException e) {
                continue;
            }
            children.sort(Comparator.comparing(path -> path.getFileName().toString().toLowerCase()));
            for (Path child : children) {
                if (lines.size() >= maxEntries) {
                    break;
                }
                if (Files.isDirectory(child)) {
                    if (pathGuard.isExcludedDir(child.getFileName().toString())) {
                        continue;
                    }
                    lines.add(describe(child));
                    if (depthOf(root, child) < MAX_DEPTH) {
                        pending.push(child);
                    }
                } else {
                    lines.add(describe(child));
                }
            }
        }
    }

    /**
     * 相对根目录的层级深度
     */
    private int depthOf(Path root, Path child) {
        return root.relativize(child).getNameCount();
    }

    /**
     * 单行描述
     */
    private String describe(Path path) {
        String name = path.getFileName() == null ? path.toString() : path.getFileName().toString();
        if (Files.isDirectory(path)) {
            return "[DIR ] " + name;
        }
        long size = 0;
        try {
            size = Files.size(path);
        } catch (IOException e) {
            size = -1;
        }
        return "[FILE] " + name + (size < 0 ? "" : "  (" + size + " 字节)");
    }
}
