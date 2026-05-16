package com.airesumeforge.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Agent RabbitMQ 队列配置。
 *
 * 默认值和 Python worker 保持一致；部署时可以用环境变量覆盖。
 * 只要 Java 和 Python 指向同一组 exchange / queue / routingKey，就能跨实例共享任务队列。
 */
@Data
@Component
@ConfigurationProperties(prefix = "agent.rabbitmq")
public class AgentRabbitProperties {

    /**
     * Agent run 主交换机，Java 生产任务，Python worker 消费任务。
     */
    private String runExchange = "agent.run.exchange";

    /**
     * Agent run 主队列，START 和 CONTINUE 都进入这一条队列。
     */
    private String runQueue = "agent.run.queue";

    /**
     * 启动 run 的路由键。
     */
    private String startRoutingKey = "agent.run.start";

    /**
     * 继续 run 的路由键。
     */
    private String continueRoutingKey = "agent.run.continue";

    /**
     * 死信交换机，用于接收格式错误或被 worker reject 的消息。
     */
    private String deadLetterExchange = "agent.run.dlx";

    /**
     * 死信队列，排查坏消息时直接看这里。
     */
    private String deadLetterQueue = "agent.run.dlq";

    /**
     * 死信路由键。
     */
    private String deadLetterRoutingKey = "agent.run.dead";


    /**
     *  返回Agent的result的队列
     */
    private String runResultQueue = "agent.run.result";


    /** 消费失败最大重试次数 */
    private int maxConsumeRetries = 3;

    /** 初始重试间隔（毫秒） */
    private long retryInitialInterval = 1000;

    /** 重试间隔倍数 */
    private double retryMultiplier = 2.0;

    /** 最大重试间隔（毫秒） */
    private long retryMaxInterval = 30000;
}
