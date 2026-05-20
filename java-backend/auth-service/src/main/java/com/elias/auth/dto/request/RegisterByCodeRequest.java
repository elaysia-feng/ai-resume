package com.elias.auth.dto.request;

import lombok.Data;

/**
 * 通过验证码注册请求
 */
@Data
public class RegisterByCodeRequest {
    /** 邮箱 */
    private String email;

    /** 验证码 */
    private String code;

    /** 用户名 */
    private String username;

    /** 密码 */
    private String password;
}
