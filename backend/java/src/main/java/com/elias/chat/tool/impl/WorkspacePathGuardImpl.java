package com.elias.chat.tool.impl;
import com.elias.chat.tool.WorkspacePathGuard;
import com.elias.chat.tool.ToolException;

import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.InvalidPathException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Set;

/**
 * 工作区路径守卫。
 *
 * <p>契约第 4 节要求的三条路径安全规则全部在这里落地：</p>
 * <ol>
 *   <li>解析成绝对路径后必须落在工作区根目录内；</li>
 *   <li>路径里含 {@code ..} 一律拒绝；</li>
 *   <li>再用 {@code toRealPath()} 校验一次，防符号链接逃逸。</li>
 * </ol>
 *
 * <p>越界时统一回「路径不在工作区内」，不区分路径是否存在，避免泄露本机目录结构。</p>
 */
@Component
public class WorkspacePathGuardImpl implements WorkspacePathGuard {




    /**
     * 解析并做词法校验，得到一个绝对路径。
     *
     * @param rawPath       模型给的路径，可为空（表示工作区根目录）
     * @param workspaceRoot 工作区根目录
     * @return 绝对路径（未做符号链接解析）
     */
    public Path resolve(String rawPath, String workspaceRoot) {
        if (workspaceRoot == null || workspaceRoot.isBlank()) {
            throw new ToolException(NO_WORKSPACE_MESSAGE);
        }
        Path root = toAbsolutePath(workspaceRoot);
        if (rawPath == null || rawPath.isBlank()) {
            return root;
        }
        if (containsParentTraversal(rawPath)) {
            throw new ToolException(OUT_OF_WORKSPACE_MESSAGE);
        }
        Path candidate;
        try {
            candidate = Paths.get(rawPath);
        } catch (InvalidPathException e) {
            throw new ToolException(OUT_OF_WORKSPACE_MESSAGE);
        }
        if (!candidate.isAbsolute()) {
            candidate = root.resolve(candidate);
        }
        candidate = candidate.toAbsolutePath().normalize();
        if (!isUnder(root, candidate)) {
            throw new ToolException(OUT_OF_WORKSPACE_MESSAGE);
        }
        return candidate;
    }

    /**
     * 解析路径并确认它真实存在且仍落在工作区内。
     *
     * @param rawPath       模型给的路径
     * @param workspaceRoot 工作区根目录
     * @return 已解析符号链接的真实路径
     */
    public Path resolveExisting(String rawPath, String workspaceRoot) {
        // 先过 resolve，里面会校验工作区是否已设置，避免 workspaceRoot 为 null 时直接 NPE
        Path candidate = resolve(rawPath, workspaceRoot);
        Path root = toAbsolutePath(workspaceRoot);
        Path realRoot;
        try {
            realRoot = root.toRealPath();
        } catch (IOException e) {
            throw new ToolException("工作区目录不存在，请重新选择工作区目录");
        }
        Path realCandidate;
        try {
            realCandidate = candidate.toRealPath();
        } catch (NoSuchFileException e) {
            throw new ToolException("文件或目录不存在");
        } catch (IOException e) {
            throw new ToolException("无法访问该路径");
        }
        if (!isUnder(realRoot, realCandidate)) {
            throw new ToolException(OUT_OF_WORKSPACE_MESSAGE);
        }
        return realCandidate;
    }

    /**
     * 判断目录名是否需要排除
     */
    public boolean isExcludedDir(String name) {
        return EXCLUDED_DIR_NAMES.contains(name);
    }

    /**
     * 把绝对路径转成相对工作区根目录的展示路径
     */
    public String relativePath(Path path, String workspaceRoot) {
        try {
            Path root = toAbsolutePath(workspaceRoot);
            return root.relativize(path.toAbsolutePath().normalize()).toString();
        } catch (Exception e) {
            return path.getFileName() == null ? path.toString() : path.getFileName().toString();
        }
    }

    /**
     * 判断路径是否落在根目录内（Windows 下做大小写兜底）
     */
    private boolean isUnder(Path base, Path candidate) {
        Path normalizedBase = base.toAbsolutePath().normalize();
        Path normalizedCandidate = candidate.toAbsolutePath().normalize();
        if (normalizedCandidate.startsWith(normalizedBase)) {
            return true;
        }
        if (File.separatorChar == '\\') {
            String baseText = normalizedBase.toString().toLowerCase(Locale.ROOT);
            String candidateText = normalizedCandidate.toString().toLowerCase(Locale.ROOT);
            return candidateText.equals(baseText) || candidateText.startsWith(baseText + File.separator);
        }
        return false;
    }

    /**
     * 路径里出现 {@code ..} 段一律拒绝
     */
    private boolean containsParentTraversal(String rawPath) {
        String normalized = rawPath.replace('\\', '/');
        for (String part : normalized.split("/")) {
            if ("..".equals(part)) {
                return true;
            }
        }
        return false;
    }

    private Path toAbsolutePath(String path) {
        try {
            return Paths.get(path).toAbsolutePath().normalize();
        } catch (InvalidPathException e) {
            throw new ToolException(OUT_OF_WORKSPACE_MESSAGE);
        }
    }
}
