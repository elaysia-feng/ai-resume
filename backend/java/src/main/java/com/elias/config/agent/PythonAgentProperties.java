package com.elias.config.agent;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Java 调 Python Agent 的配置
 * @author 爱门
 */
@Data
@Component
@ConfigurationProperties(prefix = "python.agent")
public class PythonAgentProperties {

    /**
     * Python Agent 服务地址
     */
    private String baseUrl;

    /**
     * 流式请求读取超时时间，单位秒
     */
    private Integer readTimeoutSeconds;
}
