package com.elias.auth.security;

import com.elias.common.context.UserContext;
import com.elias.auth.entity.User;
import com.elias.auth.mapper.UserMapper;
import com.elias.common.security.JwtUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;

/**
 * JWT认证过滤器
 * 每次请求都检查Header中的Authorization字段
 * 如果有有效的Bearer Token，则将用户信息存入SecurityContext和UserContext
 */
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    /**
     * 文件下载走 URL 直链（<img src> 没法带请求头），
     * 只在文件接口上允许用 query 参数传 token。
     */
    private static final String FILE_API_PREFIX = "/api/files/";

    private final JwtUtil jwtUtil;
    private final UserMapper userMapper;

    /**
     * 异步派发也要重新解析 Token。
     *
     * <p>Spring Security 的 AuthorizationFilter 在 ASYNC / ERROR 派发时会重新鉴权，而
     * OncePerRequestFilter 默认跳过这两种派发，于是 SecurityContext 为空，
     * SSE 流式接口会被判成未登录、真实错误状态码也会被改写成 401。</p>
     */
    @Override
    protected boolean shouldNotFilterAsyncDispatch() {
        return false;
    }

    @Override
    protected boolean shouldNotFilterErrorDispatch() {
        return false;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String token = resolveToken(request);

            if (token != null && jwtUtil.validateToken(token)) {
                String username = jwtUtil.getUsernameFromToken(token);
                Long userId = jwtUtil.getUserIdFromToken(token);

                UserContext.setUserId(String.valueOf(userId));

                User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
                if (user != null) {
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(user, null, new ArrayList<>());
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }

            filterChain.doFilter(request, response);
        } finally {
            UserContext.clear();
        }
    }

    /**
     * 取本次请求的 token。
     *
     * <p>优先用 Authorization 头；只有文件下载接口才允许退化到 query 参数，
     * 因为前端图片标签带不了请求头。其它接口仍然只能用请求头。</p>
     */
    private String resolveToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }

        String path = request.getRequestURI();
        if (path == null) {
            return null;
        }
        String contextPath = request.getContextPath();
        if (contextPath != null && !contextPath.isEmpty() && path.startsWith(contextPath)) {
            path = path.substring(contextPath.length());
        }
        if (path.startsWith(FILE_API_PREFIX)) {
            return request.getParameter("token");
        }
        return null;
    }
}
