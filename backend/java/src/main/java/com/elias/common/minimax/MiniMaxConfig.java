package com.elias.common.minimax;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * MiniMax 客户端配置。
 */
@Configuration
@EnableConfigurationProperties(MiniMaxProperties.class)
public class MiniMaxConfig {
}
