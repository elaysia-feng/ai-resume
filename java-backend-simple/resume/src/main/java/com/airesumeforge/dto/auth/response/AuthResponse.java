package com.airesumeforge.dto.auth.response;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 认证成功响应
 */
@Data
@AllArgsConstructor
public class AuthResponse {

    /** JWT Token */
    private String token;

    /** Token类型，固定为Bearer */
    private String type = "Bearer";

    /** 用户ID */
    private Long userId;

    /** 用户名 */
    private String username;

    /** 邮箱 */
    private String email;
}

