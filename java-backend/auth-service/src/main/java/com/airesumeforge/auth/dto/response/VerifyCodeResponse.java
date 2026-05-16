package com.airesumeforge.auth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 验证码验证成功响应
 */
@Data
@AllArgsConstructor
public class VerifyCodeResponse {

    /** 验证凭证，用于set-password */
    private String verifyToken;
}
