package com.airesumeforge.service;

import com.airesumeforge.dto.auth.request.LoginRequest;
import com.airesumeforge.dto.auth.request.RegisterRequest;
import com.airesumeforge.dto.auth.response.AuthResponse;
import com.airesumeforge.dto.auth.response.CurrentUserResponse;
import org.springframework.web.multipart.MultipartFile;

/**
 * 认证服务接口
 * 定义用户注册和登录的业务抽象
 */
public interface AuthService {

    /**
     * 用户注册
     *
     * @param request 注册请求参数
     * @return 认证响应（包含JWT Token）
     * @throws RuntimeException 用户名或邮箱已存在
     */
    AuthResponse register(RegisterRequest request);

    /**
     * 用户登录
     *
     * @param request 登录请求参数
     * @return 认证响应（包含JWT Token）
     * @throws RuntimeException 用户名或密码错误
     */
    AuthResponse login(LoginRequest request);

    /**
     * 发送验证码到邮箱
     *
     * @param email 目标邮箱
     * @param type 类型：register(注册) 或 login(登录)
     * @throws RuntimeException 发送失败
     */
    void sendVerificationCode(String email, String type);

    /**
     * 验证验证码并返回验证凭证
     *
     * @param email 邮箱
     * @param code 验证码
     * @return 验证凭证（临时JWT，包含email和verified状态）
     * @throws RuntimeException 验证码错误或已过期
     */
    String verifyCode(String email, String code);

    /**
     * 用验证凭证注册（验证码验证通过后，调用此接口设置密码完成注册）
     *
     * @param verifyToken 验证凭证（verify-code返回的JWT）
     * @param username 用户名
     * @param password 密码
     * @return 认证响应（包含JWT Token）
     * @throws RuntimeException 凭证无效、用户名已存在等
     */
    AuthResponse setPassword(String verifyToken, String username, String password);

    /**
     * 邮箱+验证码登录
     *
     * @param email 邮箱
     * @param code 验证码
     * @return 认证响应（包含JWT Token）
     * @throws RuntimeException 验证码错误或邮箱未注册
     */
    AuthResponse loginByCode(String email, String code);

    /**
     * 获取当前登录用户信息
     *
     * @return 当前用户资料
     */
    CurrentUserResponse getCurrentUser();

    /**
     * 上传头像到OSS并回写用户资料
     *
     * @param file 头像文件
     * @return 最新用户资料
     */
    CurrentUserResponse uploadAvatar(MultipartFile file);
}

