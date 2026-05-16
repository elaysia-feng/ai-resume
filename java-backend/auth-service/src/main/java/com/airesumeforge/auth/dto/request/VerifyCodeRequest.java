package com.airesumeforge.auth.dto.request;

import lombok.Data;

/**
 * 验证验证码并登录请求
 */
@Data
public class VerifyCodeRequest {
    /** 邮箱 */
    private String email;

    /** 验证码 */
    private String code;
}
