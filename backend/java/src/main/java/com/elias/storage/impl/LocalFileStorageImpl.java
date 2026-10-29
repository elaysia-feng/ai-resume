package com.elias.storage.impl;
import com.elias.storage.LocalFileStorage;

import com.elias.storage.LocalStorageProperties;
import com.elias.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * 本地磁盘文件存储。
 *
 * <p>目录结构：{storage.root}/{userId}/{kind}/{文件名}。
 * 数据库里只存相对路径（kind/文件名），不存绝对路径，也不存完整 URL。</p>
 *
 * <p>读取时必须做归属校验：目标用户从登录上下文取，
 * 不接受前端传 userId，避免有人改个参数就读到别人的头像。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LocalFileStorageImpl implements LocalFileStorage {

    /**
     * 允许的子目录白名单，防止目录穿越。
     */
    private static final Set<String> ALLOWED_KINDS = Set.of("avatar", "resume-avatar");

    /**
     * 允许的图片后缀
     */
    private static final Set<String> ALLOWED_SUFFIXES = Set.of(".png", ".jpg", ".jpeg", ".gif", ".webp", ".bmp");

    private final LocalStorageProperties properties;

    /**
     * 存储根目录。配置没给就落在「用户主目录/Documents/AI-Resume-Forge」。
     */
    public Path root() {
        String configured = properties.getRoot();
        if (StringUtils.hasText(configured)) {
            return Paths.get(configured.trim()).toAbsolutePath().normalize();
        }
        return Paths.get(System.getProperty("user.home"), "Documents", "AI-Resume-Forge")
                .toAbsolutePath().normalize();
    }

    /**
     * 保存上传的文件。
     *
     * @param userId 所属用户
     * @param kind   子目录，avatar / resume-avatar
     * @param file   上传文件
     * @return 相对路径，形如 avatar/xxx.png
     */
    public String store(Long userId, String kind, MultipartFile file) {
        String dir = resolveKind(kind);
        Path targetDir = root().resolve(String.valueOf(userId)).resolve(dir).normalize();
        if (!targetDir.startsWith(root())) {
            throw BusinessException.forbidden("存储路径不合法");
        }

        String fileName = UUID.randomUUID() + resolveSuffix(file.getOriginalFilename());
        Path target = targetDir.resolve(fileName).normalize();
        if (!target.startsWith(targetDir)) {
            throw BusinessException.forbidden("存储路径不合法");
        }

        try {
            Files.createDirectories(targetDir);
            try (var inputStream = file.getInputStream()) {
                Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            log.error("[本地存储] 写文件失败, userId={}, kind={}, error={}", userId, kind, e.getMessage(), e);
            throw BusinessException.business("保存文件失败，请稍后重试");
        }
        return dir + "/" + fileName;
    }

    /**
     * 解析出可读取的文件路径。
     *
     * @param userId 所属用户（从登录态取，不接受前端传）
     * @param kind   子目录
     * @param name   文件名，不含路径分隔符
     * @return 绝对路径
     */
    public Path resolveForRead(Long userId, String kind, String name) {
        String dir = resolveKind(kind);
        if (!StringUtils.hasText(name) || name.contains("/") || name.contains("\\") || name.contains("..")) {
            throw BusinessException.badRequest("文件名不合法");
        }

        Path base = root();
        Path targetDir = base.resolve(String.valueOf(userId)).resolve(dir).normalize();
        Path target = targetDir.resolve(name).normalize();
        if (!target.startsWith(targetDir) || !target.startsWith(base)) {
            throw BusinessException.forbidden("文件路径不合法");
        }
        if (!Files.exists(target) || !Files.isRegularFile(target)) {
            throw BusinessException.notFound("文件不存在");
        }
        return target;
    }

    /**
     * 子目录白名单校验。
     */
    private String resolveKind(String kind) {
        if (!StringUtils.hasText(kind)) {
            throw BusinessException.badRequest("文件类型不能为空");
        }
        String normalized = kind.trim().toLowerCase(Locale.ROOT);
        if (!ALLOWED_KINDS.contains(normalized)) {
            throw BusinessException.badRequest("不支持的文件类型：" + kind);
        }
        return normalized;
    }

    /**
     * 取文件后缀，只允许图片类型。
     */
    private String resolveSuffix(String originalName) {
        if (!StringUtils.hasText(originalName)) {
            return ".png";
        }
        int dotIndex = originalName.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == originalName.length() - 1) {
            return ".png";
        }
        String suffix = originalName.substring(dotIndex).toLowerCase(Locale.ROOT);
        if (!ALLOWED_SUFFIXES.contains(suffix)) {
            throw BusinessException.badRequest("头像仅支持图片文件");
        }
        return suffix;
    }
}
