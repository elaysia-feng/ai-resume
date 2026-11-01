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

    /** 本地数据记录初始化无需令牌；其它业务接口使用本地 API 令牌。 */
    private static final String[] WHITE_LIST = {
            // 桌面端首次启动创建本机数据归属记录。
            "/api/auth/local-profile"
    };

    private final JwtAuthFilter jwtAuthFilter;
    private final InternalAgentTokenFilter internalAgentTokenFilter;

    /**
     * 配置本地 API 与内部服务的无状态鉴权链。
     *
     * @param http Spring Security HTTP 配置器
     * @return 应用安全过滤器链
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        // 1. 关闭浏览器表单和服务端 Session，桌面端请求使用无状态令牌。
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // 2. 使用统一 CORS 配置，并将未授权结果转成 JSON。
                .cors(Customizer.withDefaults())
                .exceptionHandling(ex -> ex.authenticationEntryPoint(unauthorizedEntryPoint()))
                // 3. 放行本地记录初始化、内部 API 和健康检查，其它路由校验本地 API 令牌。
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(WHITE_LIST).permitAll()
                        .requestMatchers("/internal/**").permitAll()
                        .requestMatchers("/actuator/**").permitAll()
                        .anyRequest().authenticated()
                )
                // 4. 内部令牌过滤器先于 JWT 过滤器处理 Python 到 Java 的请求。
                .addFilterBefore(internalAgentTokenFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /** @return 将无效本地 API 令牌转换为统一 401 JSON 的入口 */
    private AuthenticationEntryPoint unauthorizedEntryPoint() {
        return (request, response, authException) -> {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"本地 API 会话无效或已过期\",\"data\":null}");
        };
    }
}
