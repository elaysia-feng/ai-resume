package com.airesumeforge.auth.dto.request;

import lombok.Data;

/**
 * 注册请求参数
 */
@Data
public class RegisterRequest {

    /** 用户名 */
    private String username;

    /** 邮箱 */
    private String email;

    /** 密码（直接注册时使用） */
    private String password;

    /** 真实姓名，可选 */
    private String fullName;
}
