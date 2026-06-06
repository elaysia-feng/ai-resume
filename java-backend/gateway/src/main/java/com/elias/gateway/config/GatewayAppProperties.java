package com.elias.gateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Data
@RefreshScope
@Component
@ConfigurationProperties(prefix = "gateway")
public class GatewayAppProperties {

    /**
     * 不需要登录态的路径片段。
     */
    private List<String> whiteList = new ArrayList<>(List.of(
            "/api/auth/login",
            "/api/auth/register",
            "/api/auth/send-code",
            "/api/auth/verify-code",
            "/api/auth/set-password",
            "/api/auth/login-by-code",
            "/api/notification/code"
    ));

    private String serviceToken = "change-me";

    private DynamicRouter dynamicRouter = new DynamicRouter();

    @Data
    public static class DynamicRouter {
        private String dataId = "gateway-routers.yaml";
        private String group = "DEFAULT_GROUP";
        private long timeoutMs = 5000;
    }
}
