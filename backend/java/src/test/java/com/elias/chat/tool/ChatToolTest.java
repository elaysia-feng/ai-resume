package com.elias.chat.tool;

import com.elias.chat.tool.impl.ListDirTool;
import com.elias.chat.tool.impl.ReadLocalFileTool;
import com.elias.chat.tool.impl.ReadWorkspaceTreeTool;
import com.elias.chat.tool.impl.SearchInFilesTool;
import com.elias.chat.tool.impl.ChatToolExecutorImpl;
import com.elias.chat.tool.impl.ToolRegistryImpl;
import com.elias.chat.tool.impl.WorkspacePathGuardImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 文件工具的读写行为和越界拦截。
 */
class ChatToolTest {

    @TempDir
    Path workspace;

    @TempDir
    Path outside;

    private WorkspacePathGuard pathGuard;
    private ToolRegistry toolRegistry;
    private ChatToolExecutor toolExecutor;

    @BeforeEach
    void setUp() {
        pathGuard = new WorkspacePathGuardImpl();
        toolRegistry = new ToolRegistryImpl(List.of(
                new ReadLocalFileTool(pathGuard),
                new ListDirTool(pathGuard),
                new SearchInFilesTool(pathGuard),
                new ReadWorkspaceTreeTool(pathGuard)));
        toolExecutor = new ChatToolExecutorImpl(toolRegistry);
    }

    @Test
    @DisplayName("read_local_file 能读到工作区内的文本内容")
    void readsTextFileInsideWorkspace() throws IOException {
        Files.writeString(workspace.resolve("hello.txt"), "你好，世界", StandardCharsets.UTF_8);

        ToolResult result = toolExecutor.execute("read_local_file", Map.of("path", "hello.txt"),
                1L, workspace.toString());

        assertTrue(result.isOk());
        assertTrue(result.getContent().contains("你好，世界"));
        assertTrue(result.getSummary().contains("字节"));
    }

    @Test
    @DisplayName("read_local_file 拒绝工作区外的文件")
    void rejectsFileOutsideWorkspace() throws IOException {
        Files.writeString(outside.resolve("secret.txt"), "secret", StandardCharsets.UTF_8);

        ToolResult result = toolExecutor.execute("read_local_file",
                Map.of("path", outside.resolve("secret.txt").toString()), 1L, workspace.toString());

        assertFalse(result.isOk());
        assertEquals("路径不在工作区内", result.getSummary());
    }

    @Test
    @DisplayName("read_local_file 遇到二进制文件直接拒绝，不把乱码塞给模型")
    void rejectsBinaryFile() throws IOException {
        Path binary = workspace.resolve("a.bin");
        Files.write(binary, new byte[] {0x00, 0x01, 0x02, 0x03, 0x00, 0x7F});

        ToolResult result = toolExecutor.execute("read_local_file", Map.of("path", "a.bin"),
                1L, workspace.toString());

        assertFalse(result.isOk());
        assertEquals("不支持读取二进制文件", result.getSummary());
    }

    @Test
    @DisplayName("没选工作区时所有文件工具都提示尚未选择工作区目录")
    void reportsMissingWorkspace() {
        ToolResult result = toolExecutor.execute("list_dir", Map.of(), 1L, null);

        assertFalse(result.isOk());
        assertEquals("尚未选择工作区目录", result.getSummary());
    }

    @Test
    @DisplayName("list_dir 排除 node_modules 等目录")
    void excludesNoiseDirectories() throws IOException {
        Files.writeString(workspace.resolve("keep.txt"), "keep", StandardCharsets.UTF_8);
        Files.createDirectories(workspace.resolve("node_modules"));
        Files.writeString(workspace.resolve("node_modules").resolve("pkg.json"), "{}", StandardCharsets.UTF_8);

        ToolResult result = toolExecutor.execute("list_dir", Map.of(), 1L, workspace.toString());

        assertTrue(result.isOk());
        assertTrue(result.getContent().contains("keep.txt"));
        assertFalse(result.getContent().contains("node_modules"));
    }

    @Test
    @DisplayName("search_in_files 能按内容命中工作区内的文件")
    void searchesFileContent() throws IOException {
        Files.writeString(workspace.resolve("a.txt"), "第一行\n关键词在这里\n", StandardCharsets.UTF_8);

        ToolResult result = toolExecutor.execute("search_in_files", Map.of("query", "关键词"),
                1L, workspace.toString());

        assertTrue(result.isOk());
        assertTrue(result.getContent().contains("a.txt"));
        assertTrue(result.getContent().contains("关键词在这里"));
    }

    @Test
    @DisplayName("search_in_files 的文件名匹配要扫全工作区，不能只扫前几千个文件")
    void searchesFileNameBeyondOldScanCap() throws IOException {
        // 真实仓库动辄十万级文件，排在后面的模块曾经永远搜不到（内容扫描上限 5000）
        Path bulk = workspace.resolve("a-bulk");
        Files.createDirectories(bulk);
        for (int i = 0; i < 5200; i++) {
            Files.createFile(bulk.resolve(String.format("filler-%05d.txt", i)));
        }
        Path target = workspace.resolve("b-target");
        Files.createDirectories(target);
        Files.writeString(target.resolve("TemuShopAuthInfoController.java"),
                "class TemuShopAuthInfoController {}\n", StandardCharsets.UTF_8);

        ToolResult result = toolExecutor.execute("search_in_files",
                Map.of("query", "TemuShopAuthInfoController"), 1L, workspace.toString());

        assertTrue(result.isOk());
        assertTrue(result.getContent().contains("TemuShopAuthInfoController.java"),
                "文件名匹配必须覆盖全工作区，实际结果：" + result.getContent());
    }

    @Test
    @DisplayName("search_in_files 的 path 传文件时直接搜这个文件，不报错")
    void acceptsFilePathAsSearchScope() throws IOException {
        Files.writeString(workspace.resolve("only.txt"), "关键词在这里\n", StandardCharsets.UTF_8);

        ToolResult result = toolExecutor.execute("search_in_files",
                Map.of("query", "关键词", "path", "only.txt"), 1L, workspace.toString());

        assertTrue(result.isOk());
        assertTrue(result.getContent().contains("only.txt"));
    }

    @Test
    @DisplayName("未知工具返回失败而不是抛异常")
    void reportsUnknownTool() {
        ToolResult result = toolExecutor.execute("no_such_tool", Map.of(), 1L, workspace.toString());

        assertFalse(result.isOk());
        assertTrue(result.getSummary().contains("未知工具"));
    }
}
