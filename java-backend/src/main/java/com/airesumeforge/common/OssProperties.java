package com.airesumeforge.common;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * OSS配置
 */
@Component
@ConfigurationProperties(prefix = "oss")
@Data
public class OssProperties {
    private String accessKeyId;
    private String accessKeySecret;
    private String bucketName;
    private String endpoint;
    private String domain;

    /**
     * 头像目录前缀
     */
    private String avatarDir;

    /**
     * 简历头像目录前缀
     */
    private String resumeAvatarDir;
}
