package com.elias.chat.minimax;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * MiniMax（Anthropic 兼容端点）配置。
 *
 * <p>api-key 允许为空：缺配置时应用照常启动，只有真正调用模型时才报错，
 * 这样桌面端在用户还没填 Key 时也能正常打开。</p>
 *
 * <p>前缀必须声明成 minimax，否则 application.yaml 里的 minimax.* 绑不进来，
 * 运行时只会拿到这里的默认值。</p>
 *
 * <p>密钥属性叫 anthropic-api-key 而不是 api-key：Spring 的 relaxed binding 会把
 * {@code minimax.api-key} 映射到操作系统的环境变量 {@code MINIMAX_API_KEY}，
 * 而操作系统环境变量的优先级高于 .env，本机那个变量是只对原生 chatcompletion
 * 端点有效的旧 key，会把 .env 里的正确 key 盖掉（调用 Anthropic 兼容端点会 401）。</p>
 */
@Data
@ConfigurationProperties(prefix = "minimax")
public class MiniMaxProperties {

    /**
     * Anthropic 兼容端点前缀，实际请求地址为 {base-url}/v1/messages
     */
    private String baseUrl = "https://api.minimaxi.com/anthropic";

    /**
     * MiniMax API Token，同时作为 x-api-key 和 authorization 头
     */
    private String anthropicApiKey = "";

    /**
     * 模型名，已实测 MiniMax-M3.1-Flash 可用
     */
    private String model = "MiniMax-M3.1-Flash";

    /**
     * 单次请求最大输出 token 数
     */
    private int maxTokens = 4096;
}
