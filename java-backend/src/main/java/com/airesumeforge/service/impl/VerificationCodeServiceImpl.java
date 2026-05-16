package com.airesumeforge.service.impl;

import com.airesumeforge.exception.BusinessException;
import com.airesumeforge.service.VerificationCodeService;
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
public class VerificationCodeServiceImpl implements VerificationCodeService {

    private final JavaMailSender mailSender;

    /**
     * 存储验证码：email -> {code, expirationTime}
     */
    private final Map<String, CodeInfo> codeStore = new ConcurrentHashMap<>();

    /**
     * 验证码有效期（毫秒）：5分钟
     */
    private static final long EXPIRATION = 5 * 60 * 1000;

    public VerificationCodeServiceImpl(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * 发送验证码到邮箱
     * @param to 目标邮箱
     * @param fromUsername 发送者显示名称（你的QQ邮箱）
     * @return 发送是否成功
     */
    @Override
    public boolean sendCode(String to, String fromUsername) {
        // 生成6位验证码
        String code = String.format("%06d", new Random().nextInt(1000000));

        // 存储验证码和过期时间
        codeStore.put(to, new CodeInfo(code, System.currentTimeMillis() + EXPIRATION));

        // 发送邮件
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromUsername);
            message.setTo(to);
            message.setSubject("【AI Resume Forge】验证码");
            message.setText("您的验证码是：" + code + "\n有效期5分钟，请勿泄露。");
            mailSender.send(message);
            return true;
        } catch (Exception e) {
            // 发送失败，删除存储的验证码
            codeStore.remove(to);
            throw BusinessException.business("Failed to send email: " + e.getMessage());
        }
    }

    /**
     * 验证验证码是否正确且未过期
     * @param email 邮箱
     * @param code 验证码
     * @return 是否验证通过
     */
    @Override
    public boolean verify(String email, String code) {
        CodeInfo info = codeStore.get(email);
        if (info == null) {
            return false;
        }
        if (System.currentTimeMillis() > info.expirationTime) {
            codeStore.remove(email);
            return false;
        }
        boolean valid = info.code.equals(code);
        if (valid) {
            // 验证成功后删除验证码
            codeStore.remove(email);
        }
        return valid;
    }

    /**
     * 根据邮箱查找用户（内部用）
     */
    private static class CodeInfo {
        String code;
        long expirationTime;

        CodeInfo(String code, long expirationTime) {
            this.code = code;
            this.expirationTime = expirationTime;
        }
    }
}
