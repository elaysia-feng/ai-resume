package com.elias.service.auth.impl;

import com.elias.config.auth.AuthMailProperties;
import com.elias.dto.auth.request.LocalProfileRequest;
import com.elias.dto.auth.response.AuthResponse;
import com.elias.entity.auth.User;
import com.elias.mapper.auth.UserMapper;
import com.elias.security.common.JwtUtil;
import com.elias.storage.LocalFileStorage;
import com.elias.service.notification.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 本地身份创建的单元测试。
 *
 * <p>重点覆盖幂等：同一个本地身份接口调两次，必须拿到同一个 userId，
 * 而且只往库里插一条用户记录。</p>
 */
class AuthServiceImplLocalProfileTest {

    private UserMapper userMapper;
    private PasswordEncoder passwordEncoder;
    private JwtUtil jwtUtil;
    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        userMapper = mock(UserMapper.class);
        passwordEncoder = mock(PasswordEncoder.class);
        jwtUtil = mock(JwtUtil.class);

        when(passwordEncoder.encode(anyString())).thenAnswer(invocation -> "encoded:" + invocation.getArgument(0));
        when(jwtUtil.generateToken(anyString(), anyLong()))
                .thenAnswer(invocation -> "jwt-for-" + invocation.getArgument(1));
        when(userMapper.exists(any())).thenReturn(false);

        authService = new AuthServiceImpl(
                userMapper,
                passwordEncoder,
                jwtUtil,
                mock(NotificationService.class),
                mock(LocalFileStorage.class),
                mock(AuthMailProperties.class));
    }

    @Test
    @DisplayName("连续调两次本地身份接口，拿到同一个 userId 且只建一次号")
    void createLocalProfileIsIdempotent() {
        // 第一次查不到本地身份，第二次查得到
        User created = new User();
        created.setId(7L);
        created.setUsername("小王");
        created.setEmail("local@local.invalid");
        when(userMapper.selectOne(any())).thenReturn(null, created);
        doAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(7L);
            return 1;
        }).when(userMapper).insert(any(User.class));

        AuthResponse first = authService.createLocalProfile(request("小王"));
        AuthResponse second = authService.createLocalProfile(request("小王"));

        assertNotNull(first);
        assertEquals(7L, first.getUserId());
        assertEquals(first.getUserId(), second.getUserId());
        assertEquals("小王", first.getUsername());
        assertEquals("local@local.invalid", first.getEmail());
        assertEquals("Bearer", first.getType());
        assertNotNull(first.getToken());

        verify(userMapper, times(1)).insert(any(User.class));
    }

    @Test
    @DisplayName("昵称为空或超长时直接报错，不建号")
    void invalidDisplayNameRejected() {
        assertThrows(RuntimeException.class, () -> authService.createLocalProfile(request("   ")));
        assertThrows(RuntimeException.class, () -> authService.createLocalProfile(request("昵".repeat(33))));

        verify(userMapper, times(0)).insert(any(User.class));
    }

    private LocalProfileRequest request(String displayName) {
        LocalProfileRequest request = new LocalProfileRequest();
        request.setDisplayName(displayName);
        return request;
    }
}
