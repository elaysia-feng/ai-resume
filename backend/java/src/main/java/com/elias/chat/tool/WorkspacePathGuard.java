package com.elias.chat.tool;

import java.nio.file.Path;
import java.util.Set;

/** 限制聊天工具只访问用户当前工作区内允许的路径。 */
public interface WorkspacePathGuard {

    String OUT_OF_WORKSPACE_MESSAGE = "路径不在工作区内";
    String NO_WORKSPACE_MESSAGE = "尚未选择工作区目录";
    Set<String> EXCLUDED_DIR_NAMES = Set.of(
            "node_modules", "target", ".git", ".idea", ".venv", "__pycache__", "dist", "build");

    Path resolve(String rawPath, String workspaceRoot);

    Path resolveExisting(String rawPath, String workspaceRoot);

    boolean isExcludedDir(String name);

    String relativePath(Path path, String workspaceRoot);
}
