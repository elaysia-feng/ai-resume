package com.elias.auth.dto.request;

import lombok.Data;

/**
 * 登录请求参数
 */
@Data
public class LoginRequest {

    /** 用户名（可选，也可直接用email登录） */
    private String username;

    /** 邮箱（可选，支持直接用邮箱登录） */
    private String email;

    /** 登录模式：password=密码登录，code=验证码登录 */
    private String loginMode = "password";

    /** 验证码（loginMode=code时使用） */
    private String code;

    /** 密码 */
    private String password;
}
