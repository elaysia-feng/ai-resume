package com.elias.resume.config;

import com.elias.common.OssProperties;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/**
 * OSS客户端配置
 */
@Configuration("resumeOssConfig")
public class OssConfig {

    private final OssProperties ossProperties;

    public OssConfig(OssProperties ossProperties) {
        this.ossProperties = ossProperties;
    }

    @Bean(destroyMethod = "shutdown")
    public OSS resumeOssClient() {
        if (!StringUtils.hasText(ossProperties.getEndpoint())
                || !StringUtils.hasText(ossProperties.getAccessKeyId())
                || !StringUtils.hasText(ossProperties.getAccessKeySecret())) {
            throw new IllegalStateException("OSS配置不完整，请检查 oss.endpoint / oss.access-key-id / oss.access-key-secret");
        }

        return new OSSClientBuilder().build(
                ossProperties.getEndpoint(),
                ossProperties.getAccessKeyId(),
                ossProperties.getAccessKeySecret()
        );
    }
}
