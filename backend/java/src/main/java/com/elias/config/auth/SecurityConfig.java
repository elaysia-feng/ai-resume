package com.elias.config.auth;

import com.elias.security.auth.JwtAuthFilter;
import com.elias.security.common.InternalAgentTokenFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import jakarta.servlet.http.HttpServletResponse;

/**
 * 单体应用统一安全配置
 * 合并原网关的 JWT 过滤规则与各子服务自己的白名单，整个应用只保留这一条过滤器链
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    /**
     * 无需登录即可访问的接口白名单，与原网关 JwtGateWayFilter.DEFAULT_WHITE_LIST 保持一致
     */
    private static final String[] WHITE_LIST = {
            "/api/auth/login",
            "/api/auth/register",
            "/api/auth/send-code",
            "/api/auth/verify-code",
            "/api/auth/set-password",
            "/api/auth/login-by-code",
            // 桌面端首次启动创建本地身份：匿名接口，只对应本机唯一的一个本地账号
            "/api/auth/local-profile",
            // 支付宝服务端回调没有登录态，靠 outTradeNo + 回调验签识别，不能被鉴权拦掉
            "/api/pay/callback"
    };

    private final JwtAuthFilter jwtAuthFilter;
    private final InternalAgentTokenFilter internalAgentTokenFilter;

    /**
     * 配置安全过滤器链
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 禁用CSRF（前后端分离项目不需要）
                .csrf(csrf -> csrf.disable())
                // 禁用Session（使用JWT无状态认证）
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // 跨域配置由 CorsConfig 提供，CorsFilter 需在鉴权之前处理预检请求
                .cors(Customizer.withDefaults())
                // 未登录或 Token 无效时返回 401，与原网关 JwtGateWayFilter 的行为保持一致
                .exceptionHandling(ex -> ex.authenticationEntryPoint(unauthorizedEntryPoint()))
                // 配置请求授权
                .authorizeHttpRequests(auth -> auth
                        // 白名单接口无需授权
                        .requestMatchers(WHITE_LIST).permitAll()
                        // 内部接口由 InternalAgentTokenFilter 校验 X-Internal-Service-Token
                        .requestMatchers("/internal/**").permitAll()
                        // 健康检查与监控端点无需授权
                        .requestMatchers("/actuator/**").permitAll()
                        // 其他接口需要认证
                        .anyRequest().authenticated()
                )
                // 内部接口令牌校验放在 JWT 前面，避免 Python 内部调用被 JWT 拦截
                .addFilterBefore(internalAgentTokenFilter, UsernamePasswordAuthenticationFilter.class)
                // 将JWT过滤器添加到用户名密码过滤器之前
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * 认证失败入口：统一返回 401 JSON。
     */
    private AuthenticationEntryPoint unauthorizedEntryPoint() {
        return (request, response, authException) -> {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"未登录或登录已过期\",\"data\":null}");
        };
    }
}
