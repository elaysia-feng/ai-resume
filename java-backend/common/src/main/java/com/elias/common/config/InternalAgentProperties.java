package com.elias.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

/** TODO
 * 只允许内网访问
 *
 * Nginx / 网关层限制 /internal/**
 * 只让 Python 服务所在机器或内网网段访问
 * token 定期轮换
 *
 * token 放环境变量
 * 不写死在代码里
 * 定期更换
 * HMAC 签名 + timestamp
 *
 * Python 请求时带：
 * X-Agent-Timestamp
 * X-Agent-Signature
 * Java 校验签名和时间窗口
 * 防止 token 泄露后被长期重放
 * 业务边界校验
 *
 * roundId 必须存在
 * 对应 run.sceneCode 必须是 INTERVIEW
 * run 不能是 SUCCESS / FAILED / CANCELLED
 */

/**
 * Agent 内部接口配置
 * @author 爱门
 */
@Data
@RefreshScope
@Component
@ConfigurationProperties(prefix = "internal.agent")
public class InternalAgentProperties {

    /**
     * Python 调 Java 内部接口时必须携带的服务令牌
     */
    private String serviceToken;
}
