package com.elias.config.common;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import jakarta.annotation.PostConstruct;

/** TODO(to be confirmed): 完成内网访问控制、token 轮换和请求签名校验。
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
@Component
@ConfigurationProperties(prefix = "internal.agent")
public class InternalAgentProperties {

    /**
     * Python 调 Java 内部接口时必须携带的服务令牌
     */
    private String serviceToken;

    /**
     * 令牌没配就启动失败：内部接口靠它挡住外部调用，配错或留空等于没设防。
     */
    @PostConstruct
    public void validate() {
        if (!StringUtils.hasText(serviceToken)) {
            throw new IllegalStateException(
                    "未配置 internal.agent.service-token，请在 .env 或环境变量里设置 INTERNAL_AGENT_SERVICE_TOKEN，"
                            + "并让 Python 侧使用同一个值");
        }
    }
}
