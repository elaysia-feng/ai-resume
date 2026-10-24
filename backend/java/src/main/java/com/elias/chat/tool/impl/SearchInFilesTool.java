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
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 在工作区内按文件名或文件内容搜索。
 */
@Component
@RequiredArgsConstructor
public class SearchInFilesTool implements ChatTool {

    public static final String NAME = "search_in_files";

    /**
     * 默认最多返回 100 条命中
     */
    private static final int DEFAULT_MAX_RESULTS = 100;

    /**
     * 文件名匹配要扫全树：类名/接口名往往就在文件名里，
     * 而仓库动辄十万级文件，只扫前面几千个的话排在后面的模块永远搜不到。
     */
    private static final int MAX_NAME_SCANNED_FILES = 300_000;

    /**
     * 内容匹配最多扫描的文件数：读文件内容比读文件名贵得多，限量以免一次调用拖死
     */
    private static final int MAX_CONTENT_SCANNED_FILES = 5000;

    /**
     * 单个文件最多扫描的字节数
     */
    private static final int MAX_FILE_SCAN_BYTES = 64 * 1024;

    /**
     * 搜索时最多返回的匹配行数（单个文件）
     */
    private static final int MAX_MATCHES_PER_FILE = 5;

    private final WorkspacePathGuard pathGuard;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public String description() {
        return "在工作区内按文件名或文件内容搜索关键字。文件名匹配会扫全工作区，"
                + "内容匹配最多扫前 5000 个文件。默认最多 100 条命中，会跳过二进制文件和 node_modules 等目录。";
    }

    @Override
    public Map<String, Object> inputSchema() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "query", Map.of("type", "string", "description", "要搜索的关键字"),
                        "path", Map.of("type", "string", "description", "搜索起始目录，缺省为工作区根目录；也可以是单个文件"),
                        "maxResults", Map.of("type", "integer", "description", "最多命中条数，默认 100")),
                "required", List.of("query"));
    }

    @Override
    public ToolResult execute(ToolExecutionContext context) {
        String query = context.stringArg("query");
        if (query == null || query.isBlank()) {
            throw new ToolException("搜索关键字不能为空");
        }
        int maxResults = context.intArg("maxResults", DEFAULT_MAX_RESULTS);
        if (maxResults <= 0 || maxResults > DEFAULT_MAX_RESULTS) {
            maxResults = DEFAULT_MAX_RESULTS;
        }

        String workspaceRoot = context.getWorkspaceRoot();
        Path start = pathGuard.resolveExisting(context.stringArg("path"), workspaceRoot);
        if (Files.isRegularFile(start)) {
            // 模型经常把文件路径当目录传过来，直接搜这一个文件
            List<String> fileHits = new ArrayList<>();
            matchFile(start, workspaceRoot, query.trim().toLowerCase(Locale.ROOT), fileHits, maxResults);
            return ToolResult.ok("命中 " + fileHits.size() + " 条",
                    fileHits.isEmpty() ? "(没有找到匹配内容)" : String.join("\n", fileHits));
        }
        if (!Files.isDirectory(start)) {
            throw new ToolException("搜索路径必须是目录或文件");
        }

        String keyword = query.trim().toLowerCase(Locale.ROOT);
        List<String> hits = new ArrayList<>();
        scan(start, workspaceRoot, keyword, hits, maxResults);

        String summary = "命中 " + hits.size() + " 条";
        String body = hits.isEmpty() ? "(没有找到匹配内容)" : String.join("\n", hits);
        return ToolResult.ok(summary, body);
    }

    /**
     * 广度优先遍历，分两轮：
     * 第一轮只看文件名（便宜），扫全工作区；
     * 第二轮再按内容匹配，限量扫描。
     */
    private void scan(Path start, String workspaceRoot, String keyword, List<String> hits, int maxResults) {
        List<Path> allFiles = new ArrayList<>();
        collectFiles(start, allFiles, MAX_NAME_SCANNED_FILES);

        for (Path file : allFiles) {
            if (hits.size() >= maxResults) {
                return;
            }
            String fileName = file.getFileName().toString();
            if (fileName.toLowerCase(Locale.ROOT).contains(keyword)) {
                hits.add(pathGuard.relativePath(file, workspaceRoot) + "（文件名匹配）");
            }
        }

        int contentScanned = 0;
        for (Path file : allFiles) {
            if (hits.size() >= maxResults || contentScanned >= MAX_CONTENT_SCANNED_FILES) {
                return;
            }
            contentScanned++;
            matchFileContent(file, pathGuard.relativePath(file, workspaceRoot), keyword, hits, maxResults);
        }
    }

    /**
     * 收集工作区内的所有文件（跳过排除目录），最多 maxFiles 个。
     */
    private void collectFiles(Path start, List<Path> out, int maxFiles) {
        Deque<Path> pending = new ArrayDeque<>();
        pending.add(start);
        while (!pending.isEmpty() && out.size() < maxFiles) {
            Path current = pending.poll();
            List<Path> children = new ArrayList<>();
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(current)) {
                stream.forEach(children::add);
            } catch (IOException e) {
                continue;
            }
            for (Path child : children) {
                if (out.size() >= maxFiles) {
                    return;
                }
                if (Files.isDirectory(child)) {
                    if (!pathGuard.isExcludedDir(child.getFileName().toString())) {
                        pending.add(child);
                    }
                    continue;
                }
                out.add(child);
            }
        }
    }

    /**
     * 单个文件的匹配：先看文件名，再看文件内容
     */
    private void matchFile(Path file, String workspaceRoot, String keyword, List<String> hits, int maxResults) {
        String relativePath = pathGuard.relativePath(file, workspaceRoot);
        String fileName = file.getFileName().toString();
        if (fileName.toLowerCase(Locale.ROOT).contains(keyword)) {
            hits.add(relativePath + "（文件名匹配）");
        }
        matchFileContent(file, relativePath, keyword, hits, maxResults);
    }

    /**
     * 只按内容匹配（文件名那一轮已经单独扫过了，这里不能再加一次，否则会重复）。
     */
    private void matchFileContent(Path file, String relativePath, String keyword,
                                  List<String> hits, int maxResults) {
        byte[] bytes = readHead(file);
        if (bytes == null || BinaryDetector.isBinary(bytes)) {
            return;
        }
        String content = new String(bytes, StandardCharsets.UTF_8);
        String[] lines = content.split("\r?\n", -1);
        int matched = 0;
        for (int i = 0; i < lines.length && matched < MAX_MATCHES_PER_FILE && hits.size() < maxResults; i++) {
            String line = lines[i];
            if (line.toLowerCase(Locale.ROOT).contains(keyword)) {
                String trimmed = line.strip();
                if (trimmed.length() > 200) {
                    trimmed = trimmed.substring(0, 200) + "...";
                }
                hits.add(relativePath + ":" + (i + 1) + ": " + trimmed);
                matched++;
            }
        }
    }

    /**
     * 读取文件头部内容用于匹配，失败返回 null
     */
    private byte[] readHead(Path file) {
        try (InputStream inputStream = Files.newInputStream(file)) {
            return inputStream.readNBytes(MAX_FILE_SCAN_BYTES);
        } catch (IOException e) {
            return null;
        }
    }
}
