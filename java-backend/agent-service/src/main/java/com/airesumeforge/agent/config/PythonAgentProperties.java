package com.airesumeforge.agent.config;

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
    private Integer readTimeoutSeconds = 300;

    /**
     * Agent SSE 代理线程池核心线程数。
     * 这部分线程会阻塞等待 Python 流式响应，所以必须和普通 Web 请求线程隔离。
     */
    private Integer streamCorePoolSize = 8;

    /**
     * Agent SSE 代理线程池最大线程数。
     * 高于该值的 run 不再无限创建线程，避免高并发时拖垮 JVM。
     */
    private Integer streamMaxPoolSize = 32;

    /**
     * Agent SSE 代理线程池队列容量。
     * 当运行中的 stream 达到上限时，短时间排队；队列满后直接拒绝，提示稍后重试。
     */
    private Integer streamQueueCapacity = 100;
}
