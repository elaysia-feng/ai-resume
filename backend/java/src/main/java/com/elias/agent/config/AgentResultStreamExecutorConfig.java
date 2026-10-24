package com.elias.agent.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * 消费 Python 回写结果的专用线程池。
 *
 * <p>这个消费者是一条常驻的阻塞轮询循环，不能和 SSE 代理共用线程池：
 * 代理线程池的核心线程会被慢速的 Python 流式响应长时间占住，
 * 这里单独给一条线程，保证回写结果永远有人消费。</p>
 */
@Configuration
@RequiredArgsConstructor
public class AgentResultStreamExecutorConfig {

    @Bean
    public ThreadPoolTaskExecutor agentResultStreamExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(1);
        executor.setQueueCapacity(0);
        executor.setThreadNamePrefix("agent-result-stream-");
        executor.setWaitForTasksToCompleteOnShutdown(false);
        executor.setAwaitTerminationSeconds(5);
        executor.initialize();
        return executor;
    }
}
