package com.elias.common.context;

import com.elias.common.config.GatewayHeader;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.catalina.User;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
// TODO 其它服务没加入这个包的扫描
public class UserContextFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        try{
            UserContext.setUserId(request.getHeader(GatewayHeader.USER_ID.getName()));
            filterChain.doFilter(request, response);
        }
        finally {
            // Always clear ThreadLocal to avoid data leak across reused threads.
            UserContext.clear();
        }
    }
}
