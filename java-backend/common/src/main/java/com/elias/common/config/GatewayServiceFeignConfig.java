package com.elias.common.config;

import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Configuration
public class GatewayServiceFeignConfig {

    @Bean
    public RequestInterceptor gatewayServiceTokenRequestInterceptor(GatewayServiceProperties gatewayServiceProperties) {
        return template -> {
            String serviceToken = gatewayServiceProperties.getServiceToken();
            if (StringUtils.hasText(serviceToken)) {
                template.header(GatewayHeader.SERVICE_TOKEN.getName(), serviceToken);
            }
        };
    }
}
