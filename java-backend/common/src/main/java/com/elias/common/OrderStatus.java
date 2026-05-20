package com.elias.common;

/**
 * 订单状态枚举
 */
public class OrderStatus {
    // 待支付
    public static final String PENDING = "PENDING";
    // 已支付
    public static final String PAID = "PAID";
    // 已过期（超时未支付）
    public static final String EXPIRED = "EXPIRED";
    // 已取消
    public static final String CANCELLED = "CANCELLED";
}