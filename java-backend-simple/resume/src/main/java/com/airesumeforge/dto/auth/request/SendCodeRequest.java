package com.airesumeforge.dto.auth.request;

import lombok.Data;

/**
 * 发送验证码请求
 */
@Data
public class SendCodeRequest {
    /** 目标邮箱 */
    private String email;

    /** 类型：register(注册) 或 login(登录) */
    private String type;
}
