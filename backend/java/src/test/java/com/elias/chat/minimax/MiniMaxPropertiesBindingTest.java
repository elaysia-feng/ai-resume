package com.elias.chat.minimax;

import com.elias.chat.minimax.MiniMaxProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * minimax 配置绑定。
 *
 * <p>配置前缀必须声明成 minimax，否则 application.yaml 里的 minimax.* 根本绑不进来，
 * 运行时永远拿的是代码里的默认值。这里用真实的 Spring 绑定链路验证。</p>
 */
class MiniMaxPropertiesBindingTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(MiniMaxPropertiesBindingTest.TestConfig.class)
            // 本机环境变量里有 MINIMAX_API_KEY，去掉系统环境变量源，保证断言与机器无关
            .withInitializer(context -> {
                context.getEnvironment().getPropertySources().remove("systemEnvironment");
                context.getEnvironment().getPropertySources().remove("systemProperties");
            });

    @Test
    @DisplayName("minimax.* 能绑到 MiniMaxProperties 上")
    void bindsMinimaxProperties() {
        contextRunner
                .withPropertyValues(
                        "minimax.base-url=https://example.com/anthropic",
                        "minimax.anthropic-api-key=secret-token",
                        "minimax.model=custom-model",
                        "minimax.max-tokens=1024")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    MiniMaxProperties properties = context.getBean(MiniMaxProperties.class);
                    assertEquals("https://example.com/anthropic", properties.getBaseUrl());
                    assertEquals("secret-token", properties.getAnthropicApiKey());
                    assertEquals("custom-model", properties.getModel());
                    assertEquals(1024, properties.getMaxTokens());
                });
    }

    @Test
    @DisplayName("没配 minimax.* 时用默认值，不影响启动")
    void fallsBackToDefaults() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            MiniMaxProperties properties = context.getBean(MiniMaxProperties.class);
            assertEquals("https://api.minimaxi.com/anthropic", properties.getBaseUrl());
            assertEquals("", properties.getAnthropicApiKey());
            assertEquals("MiniMax-M3.1-Flash", properties.getModel());
            assertEquals(4096, properties.getMaxTokens());
        });
    }

    @Test
    @DisplayName("必须声明 minimax 配置前缀")
    void declaresMinimaxPrefix() {
        ConfigurationProperties annotation =
                MiniMaxProperties.class.getAnnotation(ConfigurationProperties.class);

        assertNotNull(annotation, "MiniMaxProperties 缺少 @ConfigurationProperties 注解");
        assertEquals("minimax", annotation.prefix());
    }

    /**
     * 锁死密钥属性名。
     *
     * <p>Spring 的 relaxed binding 会把 {@code minimax.api-key} 映射到操作系统环境变量
     * {@code MINIMAX_API_KEY}，而操作系统环境变量优先级高于 .env。本机那个变量是只对
     * 原生 chatcompletion 端点有效的旧 key，一旦属性名改回 api-key，.env 里能用的 key
     * 就会被盖掉，调用 Anthropic 兼容端点直接 401。</p>
     */
    @Test
    @DisplayName("密钥属性名必须是 anthropic-api-key，不能是 api-key")
    void usesAnthropicApiKeyPropertyName() {
        assertThat(MiniMaxProperties.class.getDeclaredFields())
                .extracting("name")
                .contains("anthropicApiKey")
                .doesNotContain("apiKey");
    }

    @Configuration
    @EnableConfigurationProperties(MiniMaxProperties.class)
    static class TestConfig {
    }
}
