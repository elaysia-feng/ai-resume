package com.airesumeforge.auth.controller;

import com.airesumeforge.auth.dto.request.LoginRequest;
import com.airesumeforge.auth.dto.request.RegisterRequest;
import com.airesumeforge.auth.dto.request.SetPasswordRequest;
import com.airesumeforge.auth.dto.request.SendCodeRequest;
import com.airesumeforge.auth.dto.request.VerifyCodeRequest;
import com.airesumeforge.auth.service.AuthService;
import com.airesumeforge.common.ApiResponse;
import com.airesumeforge.auth.dto.response.AuthResponse;
import com.airesumeforge.auth.dto.response.CurrentUserResponse;
import com.airesumeforge.auth.dto.response.VerifyCodeResponse;
import com.airesumeforge.common.UserInfoDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 认证控制器
 * 处理用户注册和登录请求
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * 用户注册
     */
    @PostMapping("/register")
    public ApiResponse<AuthResponse> register(@RequestBody RegisterRequest request) {
        return ApiResponse.ok(authService.register(request));
    }

    /**
     * 用户登录（支持用户名或邮箱）
     */
    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@RequestBody LoginRequest request) {
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
        String type = request.getType() != null ? request.getType() : "register";
        authService.sendVerificationCode(request.getEmail(), type);
        return ApiResponse.ok();
    }

    /**
     * 验证验证码
     * 注册场景：验证通过后返回临时凭证（用于set-password）
     * 登录场景：验证通过后返回JWT Token
     */
    @PostMapping("/verify-code")
    public ApiResponse<VerifyCodeResponse> verifyCode(@RequestBody VerifyCodeRequest request) {
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
