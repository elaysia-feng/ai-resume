package com.elias.controller.auth;

import com.elias.common.ApiResponse;
import com.elias.common.UserInfoDTO;
import com.elias.dto.auth.request.LocalProfileRequest;
import com.elias.dto.auth.response.AuthResponse;
import com.elias.service.auth.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 本机数据会话控制器。 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * 创建或复用本机数据归属记录，并返回本地 API 请求令牌。
     *
     * @param request 本机记录展示名称
     * @return 本地数据编号与请求令牌
     */
    @PostMapping("/local-profile")
    public ApiResponse<AuthResponse> localProfile(@Valid @RequestBody LocalProfileRequest request) {
        return ApiResponse.ok(authService.createLocalProfile(request));
    }

    /**
     * 读取当前令牌对应的本地数据归属记录。
     *
     * @return 当前本地记录
     */
    @GetMapping("/me")
    public ApiResponse<UserInfoDTO> getCurrentUser() {
        return ApiResponse.ok(authService.getCurrentUser());
    }
}
