package com.elias.config.agent;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Agent 流式代理线程池配置。
 */
@Configuration
public class AgentStreamExecutorConfig {

    /**
     * 给 Java -> Python 的 SSE 代理使用的专用线程池。
     *
     * 每个流式请求独立运行，不设置应用级并发上限或排队上限。
     *
     * @return 不限制并发数的流式任务执行器
     */
    @Bean
    public ThreadPoolTaskExecutor agentStreamTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(0);
        executor.setMaxPoolSize(Integer.MAX_VALUE);
        executor.setQueueCapacity(0);
        executor.setThreadNamePrefix("agent-run-stream-");
        executor.setWaitForTasksToCompleteOnShutdown(false);
        executor.setAwaitTerminationSeconds(10);
        executor.initialize();
        return executor;
    }
}
