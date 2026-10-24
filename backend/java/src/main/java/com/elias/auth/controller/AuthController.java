package com.elias.auth.controller;

import com.elias.auth.dto.request.LocalProfileRequest;
import com.elias.auth.dto.request.LoginRequest;
import com.elias.auth.dto.request.RegisterRequest;
import com.elias.auth.dto.request.SetPasswordRequest;
import com.elias.auth.dto.request.SendCodeRequest;
import com.elias.auth.dto.request.VerifyCodeRequest;
import com.elias.auth.service.AuthService;
import com.elias.auth.service.RateLimiter;
import com.elias.common.ApiResponse;
import com.elias.common.exception.BusinessException;
import com.elias.auth.dto.response.AuthResponse;
import com.elias.auth.dto.response.CurrentUserResponse;
import com.elias.auth.dto.response.VerifyCodeResponse;
import com.elias.common.UserInfoDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Valid;

/**
 * 认证控制器
 * 处理用户注册和登录请求
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final RateLimiter rateLimiter;

    /**
     * 用户注册
     */
    @PostMapping("/register")
    public ApiResponse<AuthResponse> register(@RequestBody RegisterRequest request) {
        // 匿名接口必须限流：桌面端后端监听 127.0.0.1，本机任何进程都能调到这里。
        // 没有限流的话，本机脚本可以无限刷注册号，把本地库撑爆。
        if (!rateLimiter.tryAcquire("register:" + request.getEmail(), 5, 10 * 60_000L)) {
            throw BusinessException.badRequest("注册过于频繁，请 10 分钟后再试");
        }
        return ApiResponse.ok(authService.register(request));
    }

    /**
     * 创建本地身份（桌面端首次启动用）
     *
     * <p>匿名接口，但不走注册限流：它只对应本机唯一的本地账号，
     * 重复调用不会新建用户，限流按邮箱算在这里也没有意义。</p>
     */
    @PostMapping("/local-profile")
    public ApiResponse<AuthResponse> localProfile(@Valid @RequestBody LocalProfileRequest request) {
        return ApiResponse.ok(authService.createLocalProfile(request));
    }

    /**
     * 用户登录（支持用户名或邮箱）
     */
    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@RequestBody LoginRequest request) {
        // 登录限流：防止本机脚本反复试密码
        String loginKey = request.getUsername() != null && !request.getUsername().isBlank()
                ? request.getUsername()
                : String.valueOf(request.getEmail());
        if (!rateLimiter.tryAcquire("login:" + loginKey, 10, 60_000L)) {
            throw BusinessException.badRequest("登录尝试过于频繁，请 1 分钟后再试");
        }
        return ApiResponse.ok(authService.login(request));
    }

    /**
     * 发送验证码到邮箱
     *
     * type=register: 注册时发送（不检查邮箱是否存在）
     * type=login: 登录时发送（检查邮箱必须已注册）
     */
    @PostMapping("/send-code")
    public ApiResponse<Void> sendCode(@RequestBody SendCodeRequest request) {
        // 每个邮箱 60 秒一封：防止拿别人的邮箱反复触发验证码（邮件炸弹）。
        // 发信失败不占用配额 —— 否则网络抖一下用户就得干等一分钟。
        String key = "send-code:" + request.getEmail();
        if (rateLimiter.isBlocked(key, 1, 60_000L)) {
            throw BusinessException.badRequest("验证码发送过于频繁，请 60 秒后再试");
        }
        String type = request.getType() != null ? request.getType() : "register";
        authService.sendVerificationCode(request.getEmail(), type);
        rateLimiter.record(key, 60_000L);
        return ApiResponse.ok();
    }

    /**
     * 验证验证码
     * 注册场景：验证通过后返回临时凭证（用于set-password）
     * 登录场景：验证通过后返回JWT Token
     */
    @PostMapping("/verify-code")
    public ApiResponse<VerifyCodeResponse> verifyCode(@RequestBody VerifyCodeRequest request) {
        // 6 位验证码只有 100 万种，必须限流，否则本机脚本可以直接爆破
        if (!rateLimiter.tryAcquire("verify-code:" + request.getEmail(), 10, 60_000L)) {
            throw BusinessException.badRequest("验证码尝试过于频繁，请 1 分钟后再试");
        }
        String verifyToken = authService.verifyCode(request.getEmail(), request.getCode());
        return ApiResponse.ok(new VerifyCodeResponse(verifyToken));
    }

    /**
     * 设置密码完成注册（验证码注册的第二步）
     */
    @PostMapping("/set-password")
    public ApiResponse<AuthResponse> setPassword(@RequestBody SetPasswordRequest request) {
        return ApiResponse.ok(authService.setPassword(
            request.getVerifyToken(),
            request.getUsername(),
            request.getPassword()
        ));
    }

    /**
     * 邮箱+验证码登录
     */
    @PostMapping("/login-by-code")
    public ApiResponse<AuthResponse> loginByCode(@RequestBody VerifyCodeRequest request) {
        return ApiResponse.ok(authService.loginByCode(request.getEmail(), request.getCode()));
    }

    /**
     * 获取当前登录用户资料
     */
    @GetMapping("/me")
    public ApiResponse<UserInfoDTO> getCurrentUser() {
        return ApiResponse.ok(authService.getCurrentUser());
    }

    /**
     * 上传头像
     */
    @PostMapping("/avatar")
    public ApiResponse<CurrentUserResponse> uploadAvatar(@RequestParam("file") MultipartFile file) {
        return ApiResponse.ok(authService.uploadAvatar(file));
    }
}
