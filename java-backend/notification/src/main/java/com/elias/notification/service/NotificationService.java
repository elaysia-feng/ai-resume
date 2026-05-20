package com.elias.notification.service;

public interface NotificationService {

    /**
     * 在网站内发送支付成功站内通知
     * @param userId 用户ID
     * @param orderNo 订单号
     */
    void sendPaidNotice(Long userId, String orderNo);

    /**
     * 发送支付成功短信/邮件通知
     * @param userId 用户ID
     * @param orderNo 订单号
     */
    void sendPaidSms(Long userId, String orderNo);

    /**
     * 发送验证码到邮箱
     * @param to 目标邮箱
     * @param fromUsername 发送者显示名称（你的QQ邮箱）
     * @return 发送是否成功
     */
    boolean sendCode(String to, String fromUsername);

    /**
     * 验证验证码是否正确且未过期
     * @param email 邮箱
     * @param code 验证码
     * @return 是否验证通过
     */
    boolean verify(String email, String code);
}