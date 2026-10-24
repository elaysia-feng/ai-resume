package com.elias.auth.service.impl;

import com.elias.notification.service.NotificationService;
import com.elias.common.UserInfoDTO;
import com.elias.common.context.UserContext;
import com.elias.common.storage.LocalFileStorage;
import com.elias.auth.dto.request.LocalProfileRequest;
import com.elias.auth.dto.request.LoginRequest;
import com.elias.auth.dto.request.RegisterRequest;
import com.elias.auth.entity.User;
import com.elias.auth.config.AuthMailProperties;
import com.elias.common.exception.BusinessException;
import com.elias.auth.mapper.UserMapper;
import com.elias.common.security.JwtUtil;
import com.elias.auth.service.AuthService;
import com.elias.auth.dto.response.AuthResponse;
import com.elias.auth.dto.response.CurrentUserResponse;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.Locale;
import java.util.UUID;

/**
 * 认证服务实现类
 * 处理用户注册、登录和头像上传逻辑
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final long MAX_AVATAR_SIZE = 100 * 1024 * 1024L;

    /**
     * 本地身份的固定占位邮箱。
     *
     * <p>本机只允许存在一个本地身份，这个邮箱就是它的唯一标识（users.email 上有唯一索引）。
     * {@code .invalid} 是 RFC 2606 保留顶级域，永远不可能收到邮件，
     * 所以验证码登录这条路对本地身份也是死的。</p>
     */
    private static final String LOCAL_IDENTITY_EMAIL = "local@local.invalid";

    /** 昵称最大长度（去空白后） */
    private static final int LOCAL_DISPLAY_NAME_MAX_LENGTH = 32;

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final NotificationService notificationService;
    private final LocalFileStorage localFileStorage;
    private final AuthMailProperties authMailProperties;

    /**
     * 用户注册
     *
     * 加事务：插入用户和签发 token 必须在同一个事务里。
     * 签发 token 失败时（比如 JWT 密钥不合法）如果不回滚，
     * 用户已经写进库了，用户却拿不到 token，重试只会得到
     * "Username already exists" —— 半成品数据留在库里。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public AuthResponse register(RegisterRequest request) {
        log.info("[注册] 收到注册请求, username={}, email={}", request.getUsername(), request.getEmail());

        // 本地身份保留邮箱，不能被网页端注册占用，否则本地身份会被别人用密码登录
        rejectLocalIdentity(request.getEmail());

        if (userMapper.exists(new LambdaQueryWrapper<User>().eq(User::getUsername, request.getUsername()))) {
            log.warn("[注册] 用户名已存在: {}", request.getUsername());
            throw BusinessException.conflict("Username already exists");
        }

        if (userMapper.exists(new LambdaQueryWrapper<User>().eq(User::getEmail, request.getEmail()))) {
            log.warn("[注册] 邮箱已存在: {}", request.getEmail());
            throw BusinessException.conflict("Email already exists");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEnabled(true);

        userMapper.insert(user);
        log.info("[注册] 用户创建成功, userId={}, username={}", user.getId(), user.getUsername());


        String token = jwtUtil.generateToken(user.getUsername(), user.getId());
        return new AuthResponse(token, "Bearer", user.getId(), user.getUsername(), user.getEmail());
    }

    /**
     * 创建（或复用）桌面端的本地身份。
     *
     * <p>幂等：本地身份由固定邮箱 {@link #LOCAL_IDENTITY_EMAIL} 唯一确定，
     * 第一次调用建号，之后每次都直接复用同一个 userId，不会新建用户。</p>
     *
     * <p>昵称写进 users.username —— users 表没有独立的 nickname 列，
     * 而建表脚本全是 CREATE TABLE IF NOT EXISTS，给已有数据库加列不会生效，
     * 所以这里复用 username 承载可展示的昵称。</p>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public AuthResponse createLocalProfile(LocalProfileRequest request) {
        String displayName = request.getDisplayName() == null ? "" : request.getDisplayName().trim();
        if (displayName.isEmpty()) {
            throw BusinessException.badRequest("昵称不能为空，请填写一个在应用内展示的名字");
        }
        if (displayName.length() > LOCAL_DISPLAY_NAME_MAX_LENGTH) {
            throw BusinessException.badRequest("昵称最多 " + LOCAL_DISPLAY_NAME_MAX_LENGTH + " 个字符，当前是 "
                    + displayName.length() + " 个");
        }

        User localUser = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getEmail, LOCAL_IDENTITY_EMAIL));
        if (localUser != null) {
            log.info("[本地身份] 已存在，直接复用, userId={}, username={}", localUser.getId(), localUser.getUsername());
            return buildAuthResponse(localUser);
        }

        User user = new User();
        user.setUsername(resolveLocalUsername(displayName));
        user.setEmail(LOCAL_IDENTITY_EMAIL);
        // 本地身份不走密码登录：这里写一个谁都不知道的随机值
        user.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
        user.setEnabled(true);

        try {
            userMapper.insert(user);
        } catch (DuplicateKeyException e) {
            // 并发调用时另一个请求先插进去了：按固定邮箱再查一次，复用同一个账号
            User existing = userMapper.selectOne(
                    new LambdaQueryWrapper<User>().eq(User::getEmail, LOCAL_IDENTITY_EMAIL));
            if (existing == null) {
                throw e;
            }
            log.info("[本地身份] 并发创建命中唯一索引，复用已有账号, userId={}", existing.getId());
            return buildAuthResponse(existing);
        }

        log.info("[本地身份] 创建成功, userId={}, username={}", user.getId(), user.getUsername());
        return buildAuthResponse(user);
    }

    /**
     * 生成签发 token 的认证响应。
     */
    private AuthResponse buildAuthResponse(User user) {
        String token = jwtUtil.generateToken(user.getUsername(), user.getId());
        return new AuthResponse(token, "Bearer", user.getId(), user.getUsername(), user.getEmail());
    }

    /**
     * 昵称落在 users.username 上，而 username 有唯一索引。
     * 撞上网页端注册过的同名用户时加数字后缀，
     * 免得整个建身份流程因为一个唯一约束直接失败。
     */
    private String resolveLocalUsername(String displayName) {
        String candidate = displayName;
        for (int index = 2; index <= 20; index++) {
            if (!userMapper.exists(new LambdaQueryWrapper<User>().eq(User::getUsername, candidate))) {
                return candidate;
            }
            candidate = displayName + "-" + index;
        }
        return displayName + "-" + UUID.randomUUID().toString().substring(0, 6);
    }

    /**
     * 本地身份是桌面端自用的账号，不允许通过任何登录入口进来。
     */
    private void rejectLocalIdentity(String email) {
        if (email != null && LOCAL_IDENTITY_EMAIL.equalsIgnoreCase(email.trim())) {
            throw BusinessException.badRequest("本地身份不支持登录，请在应用内通过「创建本地身份」进入");
        }
    }

    /**
     * 用户登录
     * 支持密码登录或邮箱+验证码登录
     */
    @Override
    public AuthResponse login(LoginRequest request) {
        log.info("[登录] 收到登录请求, loginMode={}, username={}, email={}",
                request.getLoginMode(), request.getUsername(), request.getEmail());

        if ("code".equals(request.getLoginMode())) {
            log.info("[登录] 验证码登录模式, email={}", request.getEmail());
            return loginByCode(request.getEmail(), request.getCode());
        }

        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        User user = null;

        if (StringUtils.hasText(request.getUsername())) {
            user = userMapper.selectOne(queryWrapper.eq(User::getUsername, request.getUsername()));
        } else if (StringUtils.hasText(request.getEmail())) {
            user = userMapper.selectOne(queryWrapper.eq(User::getEmail, request.getEmail()));
        }

        if (user == null) {
            log.warn("[登录] 用户不存在, username={}, email={}", request.getUsername(), request.getEmail());
            throw BusinessException.badRequest("Invalid username or password");
        }

        // 本地身份只能通过「创建本地身份」进入，不接受密码登录
        rejectLocalIdentity(user.getEmail());

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            log.warn("[登录] 密码错误, username={}, email={}", request.getUsername(), request.getEmail());
            throw BusinessException.badRequest("Invalid username or password");
        }

        log.info("[登录] 登录成功, userId={}, username={}", user.getId(), user.getUsername());
        String token = jwtUtil.generateToken(user.getUsername(), user.getId());
        return new AuthResponse(token, "Bearer", user.getId(), user.getUsername(), user.getEmail());
    }

    /**
     * 邮箱+验证码登录
     */
    @Override
    public AuthResponse loginByCode(String email, String code) {
        log.info("[验证码登录] 收到请求, email={}", email);

        // 本地身份的邮箱是保留域名，永远收不到验证码，这里直接挡掉
        rejectLocalIdentity(email);

        if (!notificationService.verify(email, code)) {
            log.warn("[验证码登录] 验证码错误或已过期, email={}", email);
            throw BusinessException.badRequest("Invalid or expired verification code");
        }

        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getEmail, email));
        if (user == null) {
            log.warn("[验证码登录] 用户不存在, email={}", email);
            throw BusinessException.badRequest("Email not registered");
        }

        log.info("[验证码登录] 登录成功, userId={}, username={}", user.getId(), user.getUsername());
        String token = jwtUtil.generateToken(user.getUsername(), user.getId());
        return new AuthResponse(token, "Bearer", user.getId(), user.getUsername(), user.getEmail());
    }

    /**
     * 发送验证码到邮箱
     */
    @Override
    public void sendVerificationCode(String email, String type) {
        log.info("[发送验证码] 收到请求, email={}, type={}", email, type);

        // 本地身份没有真实邮箱，发验证码只会得到一个看不懂的 SMTP 报错
        rejectLocalIdentity(email);

        if ("login".equals(type)) {
            User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getEmail, email));
            if (user == null) {
                log.warn("[发送验证码] 登录模式邮箱未注册, email={}", email);
                throw BusinessException.badRequest("Email not registered");
            }
        }

        notificationService.sendCode(email, authMailProperties.getUsername());
        log.info("[发送验证码] 发送成功, email={}, type={}", email, type);
    }

    /**
     * 验证验证码并返回验证凭证
     */
    @Override
    public String verifyCode(String email, String code) {
        log.info("[验证验证码] 收到请求, email={}, code={}", email, code);

        if (!notificationService.verify(email, code)) {
            log.warn("[验证验证码] 验证码错误或已过期, email={}", email);
            throw BusinessException.badRequest("Invalid or expired verification code");
        }

        String verifyToken = jwtUtil.generateVerifyToken(email);
        log.info("[验证验证码] 验证通过, email={}", email);
        return verifyToken;
    }

    /**
     * 用验证凭证设置密码完成注册
     */
    @Override
    public AuthResponse setPassword(String verifyToken, String username, String password) {
        log.info("[设置密码注册] 收到请求, username={}", username);

        String email = jwtUtil.getEmailFromVerifyToken(verifyToken);
        if (email == null) {
            log.warn("[设置密码注册] 验证凭证无效或已过期, username={}", username);
            throw BusinessException.badRequest("Invalid or expired verification token");
        }

        if (userMapper.exists(new LambdaQueryWrapper<User>().eq(User::getUsername, username))) {
            log.warn("[设置密码注册] 用户名已存在, username={}", username);
            throw BusinessException.conflict("Username already exists");
        }

        if (userMapper.exists(new LambdaQueryWrapper<User>().eq(User::getEmail, email))) {
            log.warn("[设置密码注册] 邮箱已被注册, email={}", email);
            throw BusinessException.conflict("Email already registered");
        }

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setEnabled(true);
        userMapper.insert(user);

        log.info("[设置密码注册] 注册成功, userId={}, username={}, email={}", user.getId(), username, email);
        String token = jwtUtil.generateToken(user.getUsername(), user.getId());
        return new AuthResponse(token, "Bearer", user.getId(), user.getUsername(), user.getEmail());
    }

    /**
     * 获取当前登录用户信息
     */
    @Override
    public UserInfoDTO getCurrentUser() {
        Long userId = UserContext.getUserIdLong();
        if (userId == null) {
            throw BusinessException.forbidden("未登录或登录已过期");
        }

        User user = userMapper.selectById(userId);
        if (user == null) {
            throw BusinessException.notFound("用户不存在");
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

    /**
     * 上传头像到本机磁盘并回写数据库。
     *
     * <p>数据库里只存相对路径（avatar/xxx.png），前端拼 /api/files/avatar/xxx.png 显示。</p>
     */
    @Override
    public CurrentUserResponse uploadAvatar(MultipartFile file) {
        Long userId = UserContext.verifyGetUserId();
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw BusinessException.notFound("用户不存在");
        }

        if (file == null || file.isEmpty()) {
            throw BusinessException.badRequest("头像文件不能为空");
        }

        if (file.getSize() > MAX_AVATAR_SIZE) {
            throw BusinessException.badRequest("头像文件不能超过 100MB");
        }

        String contentType = file.getContentType();
        if (!StringUtils.hasText(contentType) || !contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
            throw BusinessException.badRequest("头像仅支持图片文件");
        }

        String avatarPath = localFileStorage.store(userId, "avatar", file);

        User updateUser = new User();
        updateUser.setId(userId);
        updateUser.setAvatarUrl(avatarPath);
        userMapper.updateById(updateUser);

        user.setAvatarUrl(avatarPath);
        return CurrentUserResponse.builder()
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
