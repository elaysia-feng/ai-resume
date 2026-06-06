package com.elias.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

@Data
@RefreshScope
@Component
@ConfigurationProperties(prefix = "gateway")
public class GatewayServiceProperties {

    /**
     * 网关转发到业务服务时携带的服务令牌。
     */
    private String serviceToken = "change-me";
}
