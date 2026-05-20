package com.elias.agent.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * Agent 流式代理线程池配置。
 */
@Configuration
@RequiredArgsConstructor
public class AgentStreamExecutorConfig {

    private final PythonAgentProperties pythonAgentProperties;

    /**
     * 给 Java -> Python 的 SSE 代理使用的专用线程池。
     *
     * 设计目标：
     * 1. 不再每个 run 都 new Thread，避免线程数无上限增长。
     * 2. 和 Tomcat 请求线程隔离，Python 流式响应慢时不占满普通请求线程。
     * 3. 用队列做短时间缓冲，超过容量后拒绝，让前端明确知道系统繁忙。
     * 4. 线程名前缀固定为 agent-run-stream-，方便日志和线程 dump 定位。
     */
    @Bean
    public ThreadPoolTaskExecutor agentStreamTaskExecutor() {
        int corePoolSize = resolvePositive(pythonAgentProperties.getStreamCorePoolSize(), 8);
        int maxPoolSize = Math.max(resolvePositive(pythonAgentProperties.getStreamMaxPoolSize(), 32), corePoolSize);

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(resolveNonNegative(pythonAgentProperties.getStreamQueueCapacity()));
        executor.setThreadNamePrefix("agent-run-stream-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(false);
        executor.setAwaitTerminationSeconds(10);
        executor.initialize();
        return executor;
    }

    private int resolvePositive(Integer value, int defaultValue) {
        return value == null || value <= 0 ? defaultValue : value;
    }

    private int resolveNonNegative(Integer value) {
        return value == null || value < 0 ? 0 : value;
    }
}
