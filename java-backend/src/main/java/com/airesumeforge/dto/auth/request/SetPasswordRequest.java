package com.airesumeforge.dto.auth.request;

import lombok.Data;

/**
 * 设置密码请求（用于验证码注册后设置密码）
 */
@Data
public class SetPasswordRequest {
    /** 验证凭证（verify-code返回的临时token） */
    private String verifyToken;

    /** 用户名 */
    private String username;

    /** 密码 */
    private String password;
}
