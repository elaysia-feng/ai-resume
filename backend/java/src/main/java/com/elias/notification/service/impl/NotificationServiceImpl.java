package com.elias.notification.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.elias.common.exception.BusinessException;
import com.elias.notification.config.NotificationMailProperties;
import com.elias.notification.entity.VerificationCode;
import com.elias.notification.mapper.VerificationCodeMapper;
import com.elias.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Random;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final JavaMailSender mailSender;
    private final VerificationCodeMapper verificationCodeMapper;
    private final NotificationMailProperties notificationMailProperties;

    /**
     * 验证码有效期（分钟）
     */
    private static final int EXPIRATION_MINUTES = 5;

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
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plusMinutes(EXPIRATION_MINUTES);
        log.info("发送的验证码是：{}", code);

        // 同一邮箱只保留一条有效验证码：旧的没过期就拒绝，过期了直接覆盖
        VerificationCode existing = verificationCodeMapper.selectOne(
                new LambdaQueryWrapper<VerificationCode>().eq(VerificationCode::getEmail, to));
        if (existing != null && existing.getExpiresAt() != null && existing.getExpiresAt().isAfter(now)) {
            throw BusinessException.business("验证码发送过于频繁，请稍后再试");
        }

        VerificationCode record = existing != null ? existing : new VerificationCode();
        record.setEmail(to);
        record.setCode(code);
        record.setExpiresAt(expiresAt);
        if (existing != null) {
            verificationCodeMapper.updateById(record);
        } else {
            verificationCodeMapper.insert(record);
        }

        // 发送邮件
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromUsername);
            message.setTo(to);
            message.setSubject("【AI Resume Forge】验证码");
            message.setText("您的验证码是：" + code + "\n有效期5分钟，请勿泄露。");
            mailSender.send(message);
            log.info("[发送验证码] 发送成功, to={}", to);
            return true;
        } catch (Exception e) {
            // 发送失败，删除存储的验证码
            verificationCodeMapper.deleteById(record.getId());
            log.error("[发送验证码] 发送失败, to={}, error={}", to, e.getMessage());
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
        VerificationCode record = verificationCodeMapper.selectOne(
                new LambdaQueryWrapper<VerificationCode>().eq(VerificationCode::getEmail, email));
        if (record == null) {
            log.warn("[验证验证码] 验证码不存在或已过期, email={}", email);
            return false;
        }
        if (record.getExpiresAt() != null && record.getExpiresAt().isBefore(LocalDateTime.now())) {
            log.warn("[验证验证码] 验证码已过期, email={}", email);
            verificationCodeMapper.deleteById(record.getId());
            return false;
        }
        if (record.getCode() != null && record.getCode().equals(code)) {
            // 验证成功后删除验证码（防重复使用）
            verificationCodeMapper.deleteById(record.getId());
            log.info("[验证验证码] 验证通过, email={}", email);
            return true;
        }
        log.warn("[验证验证码] 验证码错误, email={}", email);
        return false;
    }
}
