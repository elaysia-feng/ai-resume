package com.elias.notification.service.impl;

import com.elias.common.client.OrderClient;
import com.elias.common.client.UserClient;
import com.elias.common.ApiResponse;
import com.elias.common.OrderResponse;
import com.elias.common.UserInfoDTO;
import com.elias.common.exception.BusinessException;
import com.elias.notification.config.NotificationMailProperties;
import com.elias.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.Random;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final JavaMailSender mailSender;
    private final StringRedisTemplate redisTemplate;
    private final UserClient userClient;
    private final OrderClient orderClient;
    private final NotificationMailProperties notificationMailProperties;

    /**
     * 验证码有效期（毫秒）：5分钟
     */
    private static final long EXPIRATION = 5 * 60 * 1000;

    /**
     * 在网站内发送支付成功站内通知
     */
    @Override
    public void sendPaidNotice(Long userId, String orderNo) {
        log.info("[站内通知] 发送支付成功通知, userId={}, orderNo={}", userId, orderNo);
        // TODO: 后续可扩展为存储到站内消息表，供用户查看
        log.info("[站内通知] 支付成功通知已发送, userId={}, orderNo={}", userId, orderNo);
    }

    /**
     * 发送支付成功通知邮件
     */
    @Override
    public void sendPaidSms(Long userId, String orderNo) {
        log.info("[邮件通知] 发送支付成功通知, userId={}, orderNo={}", userId, orderNo);

        // 查询用户信息
        ApiResponse<UserInfoDTO> userResponse = userClient.getUserById(userId);
        if (userResponse == null || userResponse.getData() == null) {
            log.warn("[邮件通知] 用户不存在, userId={}", userId);
            throw BusinessException.notFound("用户不存在");
        }
        UserInfoDTO user = userResponse.getData();

        // 查询订单信息
        ApiResponse<OrderResponse> orderResponse = orderClient.queryOrder(orderNo);
        if (orderResponse == null || orderResponse.getData() == null) {
            log.warn("[邮件通知] 订单不存在, orderNo={}", orderNo);
            throw BusinessException.notFound("订单不存在");
        }
        OrderResponse order = orderResponse.getData();

        // 检查用户邮箱
        if (user.getEmail() == null || user.getEmail().isEmpty()) {
            log.warn("[邮件通知] 用户邮箱为空, userId={}", userId);
            throw BusinessException.badRequest("用户邮箱不存在");
        }

        // 发送邮件
        try {
            String emailContent = String.format(
                    "尊敬的 %s您好！\n\n您的订单已支付成功。\n\n订单号：%s\n套餐ID：%d\n支付金额：%d\n\n感谢您的购买，如有问题请联系客服。",
                    user.getUsername() != null ? user.getUsername() : user.getEmail(),
                    order.getOrderNo(),
                    order.getPlanId(),
                    order.getAmount()
            );

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(notificationMailProperties.getUsername());
            message.setTo(user.getEmail());
            message.setSubject("【AI Resume Forge】支付成功");
            message.setText(emailContent);
            mailSender.send(message);
            log.info("[邮件通知] 支付成功通知已发送, userId={}, email={}, orderNo={}", userId, user.getEmail(), orderNo);
        } catch (Exception e) {
            log.error("[邮件通知] 发送失败, userId={}, error={}", userId, e.getMessage());
            throw BusinessException.business("发送通知邮件失败");
        }
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
        String key = "verification:" + to;
        log.info("发送的验证码是：{}", code);
        // 存储验证码和过期时间
        Boolean success = redisTemplate.opsForValue()
                .setIfAbsent(key, code, EXPIRATION, TimeUnit.MILLISECONDS);
        if (Boolean.FALSE.equals(success)) {
            throw BusinessException.business("验证码发送过于频繁，请稍后再试");
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
            redisTemplate.delete(key);
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
        String key = "verification:" + email;
        String storedCode = redisTemplate.opsForValue().get(key);

        if (storedCode == null) {
            log.warn("[验证验证码] 验证码不存在或已过期, email={}", email);
            return false;
        }

        if (storedCode.equals(code)) {
            // 验证成功后删除验证码（防重复使用）
            redisTemplate.delete(key);
            log.info("[验证验证码] 验证通过, email={}", email);
            return true;
        }
        log.warn("[验证验证码] 验证码错误, email={}", email);
        return false;
    }
}
