package com.elias.security.common;

import com.elias.config.common.InternalAgentProperties;
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

    /**
     * 只在内部 API 路径上校验 Python 服务令牌。
     *
     * @param request 当前 HTTP 请求
     * @return 非内部 API 请求返回 {@code true}
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // 非 /internal/** 请求继续走本地 API JWT 校验，不在这里处理。
        String path = request.getRequestURI();
        return path == null || !path.startsWith(INTERNAL_PATH_PREFIX);
    }

    /**
     * 校验内部服务令牌，通过后继续处理请求。
     *
     * @param request 当前 HTTP 请求
     * @param response 当前 HTTP 响应
     * @param filterChain 后续过滤器链
     * @throws ServletException 过滤器链处理失败时抛出
     * @throws IOException 请求或响应读写失败时抛出
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String configuredToken = internalAgentProperties.getServiceToken();
        String requestToken = request.getHeader(TOKEN_HEADER);

        if (!StringUtils.hasText(configuredToken) || !configuredToken.equals(requestToken)) {
            // 内部接口直接返回 JSON，方便 Python 侧识别令牌错误。
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"内部服务令牌无效\",\"data\":null}");
            return;
        }

        filterChain.doFilter(request, response);
    }
}
