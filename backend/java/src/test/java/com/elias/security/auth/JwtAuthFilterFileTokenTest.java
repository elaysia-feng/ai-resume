package com.elias.security.auth;

import com.elias.entity.auth.User;
import com.elias.mapper.auth.UserMapper;
import com.elias.security.common.JwtUtil;
import com.elias.security.common.impl.JwtUtilImpl;
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
 * JwtAuthFilter 的 query 参数取 token 行为。
 *
 * <p>头像这类文件走 {@code <img src>} 直链，标签带不了 Authorization 头，
 * 所以文件接口额外允许 {@code ?token=}。但这条口子只能开在文件接口上，
 * 其它接口仍然只认请求头，否则等于把长效 token 暴露在 URL 里。</p>
 */
class JwtAuthFilterFileTokenTest {

    private static final String SECRET = "test-secret-key-for-unit-test-only-32bytes";

    private JwtAuthFilter filter;
    private String token;

    @BeforeEach
    void setUp() {
        JwtUtil jwtUtil = new JwtUtilImpl(SECRET, 60_000L);
        UserMapper userMapper = mock(UserMapper.class);
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

    private boolean authenticate(MockHttpServletRequest request) throws Exception {
        boolean[] authenticated = new boolean[1];
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, (req, res) ->
                authenticated[0] = SecurityContextHolder.getContext().getAuthentication() != null);
        return authenticated[0];
    }

    @Test
    @DisplayName("文件接口允许用 query 参数传 token")
    void acceptsTokenFromQueryParameterOnFileApi() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/files/avatar/a.png");
        request.setParameter("token", token);

        assertTrue(authenticate(request), "文件接口应当接受 query 里的 token");
    }

    @Test
    @DisplayName("非文件接口不认 query 参数里的 token")
    void rejectsTokenFromQueryParameterOnOtherApi() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/resumes/1");
        request.setParameter("token", token);

        assertFalse(authenticate(request), "非文件接口不应接受 query 里的 token");
    }

    @Test
    @DisplayName("文件接口仍然优先用请求头")
    void headerStillWorksOnFileApi() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/files/avatar/a.png");
        request.addHeader("Authorization", "Bearer " + token);

        assertTrue(authenticate(request), "文件接口应当照常支持 Authorization 头");
    }

    @Test
    @DisplayName("文件接口没有 token 时不放行")
    void rejectsFileApiWithoutToken() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/files/avatar/a.png");

        assertFalse(authenticate(request), "文件接口缺少凭证时不应通过认证");
    }
}
