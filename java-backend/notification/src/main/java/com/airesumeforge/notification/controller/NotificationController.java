package com.airesumeforge.notification.controller;

import com.airesumeforge.common.ApiResponse;
import com.airesumeforge.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/notification")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * 发送验证码到邮箱
     */
    @GetMapping("/code/send")
    public ApiResponse<Void> sendCode(
            @RequestParam String to,
            @RequestParam String fromUsername) {
        log.info("[发送验证码] 收到请求, to={}", to);
        notificationService.sendCode(to, fromUsername);
        return ApiResponse.ok(null);
    }

    /**
     * 验证验证码
     */
    @PostMapping("/code/verify")
    public ApiResponse<Boolean> verifyCode(
            @RequestParam String email,
            @RequestParam String code) {
        log.info("[验证验证码] 收到请求, email={}", email);
        boolean result = notificationService.verify(email, code);
        return ApiResponse.ok(result);
    }

    /**
     * 发送支付成功站内通知
     */
    @PostMapping("/paid/notice")
    public ApiResponse<Void> sendPaidNotice(
            @RequestParam Long userId,
            @RequestParam String orderNo) {
        log.info("[发送站内通知] 收到请求, userId={}, orderNo={}", userId, orderNo);
        notificationService.sendPaidNotice(userId, orderNo);
        return ApiResponse.ok(null);
    }

    /**
     * 发送支付成功短信
     */
    @PostMapping("/paid/sms")
    public ApiResponse<Void> sendPaidSms(
            @RequestParam Long userId,
            @RequestParam String orderNo) {
        log.info("[发送短信] 收到请求, userId={}, orderNo={}", userId, orderNo);
        notificationService.sendPaidSms(userId, orderNo);
        return ApiResponse.ok(null);
    }
}