package com.elias.controller.common;

import com.elias.common.context.UserContext;
import com.elias.storage.LocalFileStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Path;
import java.time.Duration;

/**
 * 本地文件读取接口。
 *
 * <p>头像存在服务所在机器的磁盘上，前端不能再直接拿文件路径，
 * 统一走这里按「当前本地用户 + 子目录 + 文件名」取，
 * 归属从 API 请求上下文取，不接受前端指定 userId。</p>
 */
@Slf4j
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final LocalFileStorage localFileStorage;

    /**
     * 读取当前用户的本地文件。
     *
     * <p>路径里的文件名带扩展名，用 {@code :.+} 约束保证后缀不会被吞掉。</p>
     *
     * @param kind 文件分类
     * @param name 文件名
     * @return 当前本地用户对应的文件内容
     */
    @GetMapping("/{kind}/{name:.+}")
    public ResponseEntity<Resource> read(@PathVariable String kind, @PathVariable String name) {
        Long userId = UserContext.verifyGetUserId();
        Path file = localFileStorage.resolveForRead(userId, kind, name);

        return ResponseEntity.ok()
                .contentType(guessContentType(file.getFileName().toString()))
                .cacheControl(CacheControl.maxAge(Duration.ofHours(1)).cachePrivate())
                .body(new FileSystemResource(file));
    }

    private MediaType guessContentType(String fileName) {
        // 1. 根据常见图片扩展名返回准确的媒体类型。
        String lower = fileName.toLowerCase();
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
            return MediaType.IMAGE_JPEG;
        }
        if (lower.endsWith(".gif")) {
            return MediaType.IMAGE_GIF;
        }
        if (lower.endsWith(".webp")) {
            return MediaType.parseMediaType("image/webp");
        }
        if (lower.endsWith(".bmp")) {
            return MediaType.parseMediaType("image/bmp");
        }
        // 2. 未匹配的文件按 PNG 处理，兼容已有头像文件。
        return MediaType.IMAGE_PNG;
    }
}
