package com.airesumeforge.service;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 验证码服务
 * 负责生成和验证邮箱验证码
 */
@Service
public interface VerificationCodeService {

    /**
     * 发送验证码到邮箱
     * @param to 目标邮箱
     * @param fromUsername 发送者显示名称（你的QQ邮箱）
     * @return 发送是否成功
     */
    public boolean sendCode(String to, String fromUsername);

    /**
     * 验证验证码是否正确且未过期
     * @param email 邮箱
     * @param code 验证码
     * @return 是否验证通过
     */
    public boolean verify(String email, String code);
}