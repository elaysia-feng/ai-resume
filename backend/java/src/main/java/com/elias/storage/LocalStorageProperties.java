package com.elias.storage;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 本地文件存储配置。
 *
 * <p>头像这类文件直接存本机磁盘，不再走对象存储：
 * 目录结构是 {root}/{userId}/{子目录}/{文件名}。</p>
 */
@Component
@ConfigurationProperties(prefix = "storage")
@Data
public class LocalStorageProperties {

    /**
     * 本地存储根目录。留空时用「用户主目录/Documents/AI-Resume-Forge」。
     */
    private String root;
}
