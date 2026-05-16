package com.airesumeforge.security;

import com.airesumeforge.config.InternalAgentProperties;
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
 * Python Agent 内部接口令牌过滤器
 * 只拦截 /internal/**，避免内部接口被前端或第三方直接调用
 */
@Component
@RequiredArgsConstructor
public class InternalAgentTokenFilter extends OncePerRequestFilter {

    /**
     * 内部接口路径前缀，只保护 Python/Java 内部通信接口。
     */
    private static final String INTERNAL_PATH_PREFIX = "/internal/";

    /**
     * Python 调 Java 内部接口时携带的令牌 header。
     */
    private static final String TOKEN_HEADER = "X-Internal-Service-Token";

    private final InternalAgentProperties internalAgentProperties;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // 非 /internal/** 请求继续走正常用户登录鉴权，不在这里处理。
        String path = request.getRequestURI();
        return path == null || !path.startsWith(INTERNAL_PATH_PREFIX);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String configuredToken = internalAgentProperties.getServiceToken();
        String requestToken = request.getHeader(TOKEN_HEADER);

        if (!StringUtils.hasText(configuredToken) || !configuredToken.equals(requestToken)) {
            // 内部接口不返回登录页，直接给 JSON，方便 Python 侧识别鉴权失败。
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"内部服务令牌无效\",\"data\":null}");
            return;
        }

        filterChain.doFilter(request, response);
    }
}
