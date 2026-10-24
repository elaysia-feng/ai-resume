package com.elias.common.storage;

import com.elias.common.LocalStorageProperties;
import com.elias.common.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 本地磁盘存储的行为约束。
 *
 * <p>三个关键点：① 文件按 userId 分目录，数据库只存相对路径；
 * ② 读文件时按当前登录用户定位目录，别人读不到；
 * ③ 路径穿越和非图片后缀一律拒绝。</p>
 */
class LocalFileStorageTest {

    @TempDir
    Path tempRoot;

    private LocalFileStorage storage;

    @BeforeEach
    void setUp() {
        LocalStorageProperties properties = new LocalStorageProperties();
        properties.setRoot(tempRoot.toString());
        storage = new LocalFileStorage(properties);
    }

    private MultipartFile pngFile(String name) {
        return new MockMultipartFile("file", name, "image/png", new byte[]{1, 2, 3});
    }

    @Test
    @DisplayName("存储后返回相对路径，文件落在 root/{userId}/{kind}/ 下")
    void storesUnderUserScopedDirectory() throws Exception {
        String relative = storage.store(7L, "avatar", pngFile("me.png"));

        assertTrue(relative.startsWith("avatar/"), "返回值应是相对路径，实际：" + relative);
        assertFalse(relative.contains("\\"), "相对路径应统一用 / 分隔");

        Path stored = tempRoot.resolve("7").resolve("avatar").resolve(relative.substring("avatar/".length()));
        assertTrue(Files.exists(stored), "文件应落在 root/7/avatar/ 下");
        assertEquals(3L, Files.size(stored));
    }

    @Test
    @DisplayName("非图片后缀直接拒绝")
    void rejectsNonImageSuffix() {
        MultipartFile script = new MockMultipartFile("file", "evil.sh", "image/png", new byte[]{1});

        BusinessException ex = assertThrows(BusinessException.class, () -> storage.store(7L, "avatar", script));
        assertEquals(400, ex.getCode());
    }

    @Test
    @DisplayName("不支持的子目录直接拒绝")
    void rejectsUnknownKind() {
        BusinessException ex = assertThrows(BusinessException.class, () -> storage.store(7L, "etc", pngFile("a.png")));
        assertEquals(400, ex.getCode());
    }

    @Test
    @DisplayName("读文件按当前用户定位目录，读不到别人的文件")
    void cannotReadOtherUsersFile() {
        String relative = storage.store(7L, "avatar", pngFile("me.png"));
        String name = relative.substring("avatar/".length());

        Path mine = storage.resolveForRead(7L, "avatar", name);
        assertTrue(Files.exists(mine));

        BusinessException ex = assertThrows(BusinessException.class, () -> storage.resolveForRead(8L, "avatar", name));
        assertEquals(404, ex.getCode());
    }

    @Test
    @DisplayName("文件名里带路径分隔符或 .. 一律拒绝")
    void rejectsPathTraversal() {
        assertEquals(400, assertThrows(BusinessException.class,
                () -> storage.resolveForRead(7L, "avatar", "../7/avatar/x.png")).getCode());
        assertEquals(400, assertThrows(BusinessException.class,
                () -> storage.resolveForRead(7L, "avatar", "sub/x.png")).getCode());
    }

    @Test
    @DisplayName("文件不存在返回 404")
    void missingFileIsNotFound() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> storage.resolveForRead(7L, "avatar", "missing.png"));
        assertEquals(404, ex.getCode());
    }
}
