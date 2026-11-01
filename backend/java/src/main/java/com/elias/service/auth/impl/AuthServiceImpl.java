package com.elias.service.auth.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.elias.common.UserInfoDTO;
import com.elias.common.context.UserContext;
import com.elias.dto.auth.request.LocalProfileRequest;
import com.elias.dto.auth.response.AuthResponse;
import com.elias.entity.auth.User;
import com.elias.exception.BusinessException;
import com.elias.mapper.auth.UserMapper;
import com.elias.security.common.JwtUtil;
import com.elias.service.auth.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** 本机数据归属记录的创建与读取。 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final String LOCAL_IDENTITY_EMAIL = "local@local.invalid";
    private static final int LOCAL_DISPLAY_NAME_MAX_LENGTH = 32;

    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;

    /**
     * 创建或复用本地记录。重复调用保持同一个 userId，供本机简历与会话关联。
     *
     * @param request 本机记录展示名称
     * @return 本地数据编号与请求令牌
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public AuthResponse createLocalProfile(LocalProfileRequest request) {
        // 1. 校验本机记录的展示名称。
        String displayName = request.getDisplayName() == null ? "" : request.getDisplayName().trim();
        if (displayName.isEmpty()) {
            throw BusinessException.badRequest("本地记录名称不能为空");
        }
        if (displayName.length() > LOCAL_DISPLAY_NAME_MAX_LENGTH) {
            throw BusinessException.badRequest("本地记录名称最多 " + LOCAL_DISPLAY_NAME_MAX_LENGTH + " 个字符");
        }

        // 2. 固定邮箱是本机记录的唯一键；已有记录直接签发本地 API 令牌。
        User localUser = findLocalUser();
        if (localUser != null) {
            return buildAuthResponse(localUser);
        }

        // 3. 首次启动创建数据归属记录；并发命中唯一键时回查并复用。
        User user = new User();
        user.setUsername(resolveLocalUsername(displayName));
        user.setEmail(LOCAL_IDENTITY_EMAIL);
        // 历史 users.password 是非空列；本地工作区没有密码登录，写入空占位值。
        user.setPassword("");
        user.setEnabled(true);

        try {
            userMapper.insert(user);
        } catch (DuplicateKeyException e) {
            User existing = findLocalUser();
            if (existing == null) {
                throw e;
            }
            return buildAuthResponse(existing);
        }

        return buildAuthResponse(user);
    }

    /**
     * 根据固定邮箱读取本机唯一的数据归属记录。
     *
     * @return 本地记录；尚未创建时返回 {@code null}
     */
    private User findLocalUser() {
        return userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getEmail, LOCAL_IDENTITY_EMAIL));
    }

    /**
     * 签发供本机 API 使用的无状态令牌。
     *
     * @param user 本地数据归属记录
     * @return 本地 API 会话响应
     */
    private AuthResponse buildAuthResponse(User user) {
        String token = jwtUtil.generateToken(user.getUsername(), user.getId());
        return new AuthResponse(token, "Bearer", user.getId(), user.getUsername(), user.getEmail());
    }

    private String resolveLocalUsername(String displayName) {
        // 1. 先尝试展示名称，再检查带数字后缀的唯一用户名。
        String candidate = displayName;
        for (int index = 2; index <= 20; index++) {
            if (!userMapper.exists(new LambdaQueryWrapper<User>().eq(User::getUsername, candidate))) {
                return candidate;
            }
            candidate = displayName + "-" + index;
        }
        // 2. 少见的多重冲突用随机后缀兜底。
        return displayName + "-" + UUID.randomUUID().toString().substring(0, 6);
    }

    /**
     * 返回当前请求令牌对应的本地数据记录。
     *
     * @return 当前本地记录
     */
    @Override
    public UserInfoDTO getCurrentUser() {
        // 1. 从 JWT 过滤器写入的上下文读取本地数据编号。
        Long userId = UserContext.getUserIdLong();
        if (userId == null) {
            throw BusinessException.forbidden("本地 API 会话无效或已过期");
        }

        // 2. 回查记录供本地客户端检查当前令牌。
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw BusinessException.notFound("本地数据记录不存在");
        }

        return UserInfoDTO.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .avatarUrl(user.getAvatarUrl())
                .enabled(user.getEnabled())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
