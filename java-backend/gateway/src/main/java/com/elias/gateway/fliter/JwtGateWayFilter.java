package com.elias.gateway.fliter;

import com.elias.gateway.security.JwtUtil;
import com.elias.gateway.config.GatewayHeader;
import com.elias.gateway.config.GatewayAppProperties;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class JwtGateWayFilter implements GlobalFilter, Ordered {

    private final JwtUtil jwtUtil;
    private final GatewayAppProperties gatewayProperties;

    JwtGateWayFilter(JwtUtil jwtUtil, GatewayAppProperties gatewayProperties) {
        this.jwtUtil = jwtUtil;
        this.gatewayProperties = gatewayProperties;
    }
    // TODO 可能还有个临时身份验证没写
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        // 放行白名单
        String path = request.getPath().toString();
        if (isWhiteListed(path)) {
            return chain.filter(exchange);
        }

        // 获取请求头
        HttpHeaders headers = request.getHeaders();

        // 获取token
        String authHeader = headers.getFirst("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }
        // 去掉 "Bearer " 前缀，拿到纯 token
        String token = authHeader.substring(7);

        // 验证token, TODO 管理接口做角色
        jwtUtil.validateToken(token);

        Long userId = jwtUtil.getUserIdFromToken(token);

        // 把用户信息写进请求头, 然后传递给下游
        ServerHttpRequest mutate = request.mutate()
                .header(GatewayHeader.USER_ID.getName(), userId.toString())
                .build();

        return chain.filter(exchange.mutate().request(mutate).build());

    }

    // 是不是在白名单里面
    private boolean isWhiteListed(String path) {
        return gatewayProperties.getWhiteList().stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .anyMatch(path::contains);
    }


    // 只需要比
    @Override
    public int getOrder() {
        return 0;
    }
}
