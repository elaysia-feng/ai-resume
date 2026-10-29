package com.elias.storage;

import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;

/** 头像等用户文件的本地存储边界。 */
public interface LocalFileStorage {

    Path root();

    String store(Long userId, String kind, MultipartFile file);

    Path resolveForRead(Long userId, String kind, String name);
}
