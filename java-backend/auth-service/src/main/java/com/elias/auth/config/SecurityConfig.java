package com.elias.auth.config;

import com.elias.auth.security.JwtAuthFilter;
import com.elias.common.security.GatewayServiceTokenFilter;
import com.elias.common.security.InternalAgentTokenFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security配置类
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final InternalAgentTokenFilter internalAgentTokenFilter;

    /**
     * 配置安全过滤器链
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                  GatewayServiceTokenFilter gatewayServiceTokenFilter) throws Exception {
        http
                // 禁用CSRF（前后端分离项目不需要）
                .csrf(csrf -> csrf.disable())
                // 禁用Session（使用JWT无状态认证）
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // 配置请求授权
                .authorizeHttpRequests(auth -> auth
                        // 认证接口无需授权
                        .requestMatchers("/api/auth/**").permitAll()
                        // 内部接口由 InternalAgentTokenFilter 校验 X-Internal-Service-Token
                        .requestMatchers("/internal/**").permitAll()
                        // 其他接口需要认证
                        .anyRequest().authenticated()
                )
                .addFilterBefore(gatewayServiceTokenFilter, UsernamePasswordAuthenticationFilter.class)
                // 内部接口令牌校验放在 JWT 前面，避免 Python 内部调用被 JWT 拦截
                .addFilterBefore(internalAgentTokenFilter, UsernamePasswordAuthenticationFilter.class)
                // 将JWT过滤器添加到用户名密码过滤器之前
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
