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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * 输出工作区目录树。
 */
@Component
@RequiredArgsConstructor
public class ReadWorkspaceTreeTool implements ChatTool {

    public static final String NAME = "read_workspace_tree";

    /**
     * 默认深度
     */
    private static final int DEFAULT_DEPTH = 2;

    /**
     * 最大深度
     */
    private static final int MAX_DEPTH = 6;

    /**
     * 最多输出的条目数
     */
    private static final int MAX_ENTRIES = 500;

    private final WorkspacePathGuard pathGuard;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public String description() {
        return "列出工作区的目录树结构。默认深度 2，会跳过 node_modules 等目录。";
    }

    @Override
    public Map<String, Object> inputSchema() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "path", Map.of("type", "string", "description", "起始目录，缺省为工作区根目录"),
                        "depth", Map.of("type", "integer", "description", "展开深度，默认 2")),
                "required", List.of());
    }

    @Override
    public ToolResult execute(ToolExecutionContext context) {
        int depth = context.intArg("depth", DEFAULT_DEPTH);
        if (depth <= 0 || depth > MAX_DEPTH) {
            depth = DEFAULT_DEPTH;
        }
        String workspaceRoot = context.getWorkspaceRoot();
        Path root = pathGuard.resolveExisting(context.stringArg("path"), workspaceRoot);
        if (!Files.isDirectory(root)) {
            throw new ToolException("目标不是目录");
        }

        List<String> lines = new ArrayList<>();
        lines.add(pathGuard.relativePath(root, workspaceRoot));
        appendChildren(root, 1, depth, lines);
        if (lines.size() > MAX_ENTRIES) {
            lines = lines.subList(0, MAX_ENTRIES);
        }

        String summary = "目录树共 " + lines.size() + " 项";
        return ToolResult.ok(summary, String.join("\n", lines));
    }

    /**
     * 递归追加子项
     */
    private void appendChildren(Path directory, int level, int maxDepth, List<String> lines) {
        if (level > maxDepth || lines.size() > MAX_ENTRIES) {
            return;
        }
        List<Path> children = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory)) {
            stream.forEach(children::add);
        } catch (IOException e) {
            return;
        }
        children.sort(Comparator.comparing(path -> path.getFileName().toString().toLowerCase()));
        for (Path child : children) {
            if (lines.size() > MAX_ENTRIES) {
                return;
            }
            String name = child.getFileName() == null ? child.toString() : child.getFileName().toString();
            if (Files.isDirectory(child)) {
                if (pathGuard.isExcludedDir(name)) {
                    continue;
                }
                lines.add(indent(level) + name + "/");
                appendChildren(child, level + 1, maxDepth, lines);
            } else {
                lines.add(indent(level) + name);
            }
        }
    }

    /**
     * 缩进
     */
    private String indent(int level) {
        return "  ".repeat(Math.max(0, level - 1));
    }
}
