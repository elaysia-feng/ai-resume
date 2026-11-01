package com.elias.common.stream;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Agent 任务队列配置（SQLite 版，顶替原来的 RabbitMQ / Redis Stream）。
 *
 * <p>默认值和 Python worker 保持一致；部署时可以用环境变量覆盖。
 * 只要 Java 和 Python 指向同一组 stream / 消费组名，就能跨进程共享任务队列。</p>
 */
@Data
@Component
@ConfigurationProperties(prefix = "agent.stream")
public class AgentStreamProperties {

    /**
     * 简历优化任务流，Java 投递、Python resume worker 消费。
     */
    private String resumeRunStream;

    /**
     * 面试模拟任务流，Java 投递、Python interview worker 消费。
     */
    private String interviewRunStream;

    /**
     * Python 回写 Java 的结果流。
     */
    private String runResultStream;

    /**
     * 死信流，坏消息统一转投到这里，供人工排查。
     */
    private String deadLetterStream;

    /**
     * 简历优化任务流的消费组（Python resume worker）。
     */
    private String resumeWorkerGroup;

    /**
     * 面试模拟任务流的消费组（Python interview worker）。
     */
    private String interviewWorkerGroup;

    /**
     * 结果流的消费组（Java 端消费 Python 回写）。
     */
    private String resultConsumerGroup;

    /**
     * 死信流的消费组（只写不读，留给人工排查）。
     */
    private String deadLetterGroup = "java-dlq-reader";

    /**
     * Java 端消费者名，同组多实例部署时要区分。
     */
    private String resultConsumerName;

    /**
     * 单次拉取的消息条数。
     */
    private int resultBatchSize;

    /**
     * 消费失败最大重试次数。
     */
    private int maxConsumeRetries;

    /**
     * 初始重试间隔（毫秒）。
     */
    private long retryInitialInterval;

    /**
     * 重试间隔倍数。
     */
    private double retryMultiplier;

    /**
     * 最大重试间隔（毫秒）。
     */
    private long retryMaxInterval;

    /**
     * 由流名推导消费组。
     *
     * <p>生产者和消费者必须对「这条流归哪个组」达成一致，
     * 否则投递出去的消息没有消费者认领。</p>
     *
     * @param stream 任务流名称
     * @return 对应的消费组名称
     */
    public String groupOf(String stream) {
        // 1. 按任务流映射生产者与消费者约定的消费组。
        if (resumeRunStream != null && resumeRunStream.equals(stream)) {
            return resumeWorkerGroup;
        }
        if (interviewRunStream != null && interviewRunStream.equals(stream)) {
            return interviewWorkerGroup;
        }
        if (runResultStream != null && runResultStream.equals(stream)) {
            return resultConsumerGroup;
        }
        if (deadLetterStream != null && deadLetterStream.equals(stream)) {
            return deadLetterGroup;
        }
        // 2. 未配置的流不能静默入队，避免任务没有消费者。
        throw new IllegalArgumentException("未知的任务流: " + stream);
    }
}
