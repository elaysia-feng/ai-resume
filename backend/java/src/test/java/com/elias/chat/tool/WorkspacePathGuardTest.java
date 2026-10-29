package com.elias.chat.tool;

import com.elias.chat.tool.impl.WorkspacePathGuardImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 工作区路径守卫。
 *
 * <p>这三条规则是文件工具的安全底线：越界一律拒绝，且不能泄露路径是否存在。</p>
 */
class WorkspacePathGuardTest {

    @TempDir
    Path workspace;

    @TempDir
    Path outside;

    private WorkspacePathGuard pathGuard;

    @BeforeEach
    void setUp() {
        pathGuard = new WorkspacePathGuardImpl();
    }

    @Test
    @DisplayName("工作区外的路径被拒绝，且不暴露路径是否存在")
    void rejectsPathOutsideWorkspace() throws IOException {
        Path secret = outside.resolve("secret.txt");
        Files.writeString(secret, "secret", StandardCharsets.UTF_8);

        ToolException exception = assertThrows(ToolException.class,
                () -> pathGuard.resolveExisting(secret.toString(), workspace.toString()));

        assertEquals("路径不在工作区内", exception.getMessage());
    }

    @Test
    @DisplayName("路径里含 .. 直接拒绝")
    void rejectsParentTraversal() throws IOException {
        Path secret = outside.resolve("secret.txt");
        Files.writeString(secret, "secret", StandardCharsets.UTF_8);

        ToolException exception = assertThrows(ToolException.class,
                () -> pathGuard.resolveExisting("../" + outside.getFileName() + "/secret.txt",
                        workspace.toString()));

        assertEquals("路径不在工作区内", exception.getMessage());
    }

    @Test
    @DisplayName("工作区内的相对路径能正常解析")
    void resolvesPathInsideWorkspace() throws IOException {
        Files.writeString(workspace.resolve("hello.txt"), "你好", StandardCharsets.UTF_8);

        Path resolved = pathGuard.resolveExisting("hello.txt", workspace.toString());

        assertTrue(Files.exists(resolved));
        assertEquals("hello.txt", resolved.getFileName().toString());
    }

    @Test
    @DisplayName("没选工作区时直接提示尚未选择工作区目录")
    void reportsMissingWorkspace() {
        ToolException exception = assertThrows(ToolException.class,
                () -> pathGuard.resolve("hello.txt", null));

        assertEquals("尚未选择工作区目录", exception.getMessage());
    }

    @Test
    @DisplayName("工作区外的绝对路径即使拼在根目录后面也会被拒绝")
    void rejectsSiblingDirectoryWithSamePrefix() throws IOException {
        Path sibling = workspace.resolveSibling(workspace.getFileName() + "-other");
        Files.createDirectories(sibling);

        ToolException exception = assertThrows(ToolException.class,
                () -> pathGuard.resolveExisting(sibling.toString(), workspace.toString()));

        assertEquals("路径不在工作区内", exception.getMessage());
    }
}
