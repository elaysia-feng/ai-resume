package com.elias.gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
public class CorsResponseHeaderFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
            dedupe(exchange.getResponse().getHeaders(), HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN);
            dedupe(exchange.getResponse().getHeaders(), HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS);
        }));
    }

    private void dedupe(HttpHeaders headers, String name) {
        List<String> values = headers.get(name);
        if (values != null && values.size() > 1) {
            headers.set(name, values.get(0));
        }
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
