package com.elias.common.security;

import com.elias.common.config.GatewayHeader;
import com.elias.common.config.GatewayServiceProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 保护业务 API 只能通过网关或受信任的服务间调用进入。
 */
@Component
@RequiredArgsConstructor
public class GatewayServiceTokenFilter extends OncePerRequestFilter {

    private static final String API_PATH_PREFIX = "/api/";

    private final GatewayServiceProperties gatewayServiceProperties;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return "OPTIONS".equalsIgnoreCase(request.getMethod())
                || path == null
                || !path.startsWith(API_PATH_PREFIX);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String configuredToken = gatewayServiceProperties.getServiceToken();
        String requestToken = request.getHeader(GatewayHeader.SERVICE_TOKEN.getName());

        if (!StringUtils.hasText(configuredToken) || !configuredToken.equals(requestToken)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"网关服务令牌无效\",\"data\":null}");
            return;
        }

        filterChain.doFilter(request, response);
    }
}
