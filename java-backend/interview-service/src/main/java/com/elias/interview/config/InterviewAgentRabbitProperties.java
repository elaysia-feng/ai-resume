package com.elias.interview.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

@Data
@RefreshScope
@Component
@ConfigurationProperties(prefix = "agent.rabbitmq")
public class InterviewAgentRabbitProperties {
    private String runExchange = "agent.run.exchange";
    private String interviewStartRoutingKey = "agent.interview.run.start";
    private String interviewContinueRoutingKey = "agent.interview.run.continue";
}
