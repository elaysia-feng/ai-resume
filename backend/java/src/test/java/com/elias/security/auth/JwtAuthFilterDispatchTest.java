package com.elias.security.auth;

import com.elias.entity.auth.User;
import com.elias.mapper.auth.UserMapper;
import com.elias.security.common.JwtUtil;
import com.elias.security.common.impl.JwtUtilImpl;
import jakarta.servlet.DispatcherType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * JwtAuthFilter 的异步/错误派发行为。
 *
 * <p>SSE 流式接口走 ASYNC 派发，出错时走 ERROR 派发。Spring Security 在这两种派发里会重新鉴权，
 * 而 OncePerRequestFilter 默认直接跳过，导致 SecurityContext 为空——合法 token 被判未登录、
 * 真实错误状态码被改写成 401。这里锁死「ASYNC / ERROR 派发必须继续解析 token」。</p>
 */
class JwtAuthFilterDispatchTest {

    private static final String SECRET = "test-secret-key-for-unit-test-only-32bytes";

    private JwtUtil jwtUtil;
    private UserMapper userMapper;
    private JwtAuthFilter filter;
    private String token;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtilImpl(SECRET, 60_000L);
        userMapper = mock(UserMapper.class);
        when(userMapper.selectOne(any())).thenReturn(User.builder()
                .id(9L)
                .username("alice")
                .email("alice@example.com")
                .enabled(true)
                .build());
        filter = new JwtAuthFilter(jwtUtil, userMapper);
        token = jwtUtil.generateToken("alice", 9L);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("异步派发和错误派发都不能跳过过滤")
    void doesNotSkipAsyncAndErrorDispatch() {
        assertFalse(filter.shouldNotFilterAsyncDispatch());
        assertFalse(filter.shouldNotFilterErrorDispatch());
    }

    @Test
    @DisplayName("ASYNC 派发时依然解析 token 并写入 SecurityContext")
    void authenticatesOnAsyncDispatch() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/agent/runs/1/events");
        request.setDispatcherType(DispatcherType.ASYNC);
        request.addHeader("Authorization", "Bearer " + token);

        boolean[] authenticated = new boolean[1];
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) ->
                authenticated[0] = SecurityContextHolder.getContext().getAuthentication() != null);

        assertTrue(authenticated[0], "ASYNC 派发时 SecurityContext 里应当有认证信息");
    }

    @Test
    @DisplayName("ERROR 派发时依然解析 token")
    void authenticatesOnErrorDispatch() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/resumes/1");
        request.setDispatcherType(DispatcherType.ERROR);
        request.setAttribute("jakarta.servlet.error.request_uri", "/api/resumes/1");
        request.addHeader("Authorization", "Bearer " + token);

        boolean[] authenticated = new boolean[1];
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) ->
                authenticated[0] = SecurityContextHolder.getContext().getAuthentication() != null);

        assertTrue(authenticated[0], "ERROR 派发时 SecurityContext 里应当有认证信息");
    }
}
