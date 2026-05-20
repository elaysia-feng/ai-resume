package com.elias.common.mq;

/**
 * MQ 常量定义 - Exchange / RoutingKey
 */
public class MqConstants {

    // ======================== 交换机 ========================

    /**
     * 订单状态变更交换机
     */
    public static final String ORDER_EXCHANGE = "order_exchange";

    /**
     * 死信交换机
     */
    public static final String DEAD_LETTER_EXCHANGE = "order_dlx";

    // ======================== 队列 ========================

    public static final String PAID_QUEUE = "order_paid_queue";
    public static final String EXPIRED_QUEUE = "order_expired_queue";
    public static final String CANCELLED_QUEUE = "order_cancelled_queue";
    public static final String ORDER_DELAY_QUEUE = "order_delay_queue";

    /**
     * 死信队列
     */
    public static final String DEAD_LETTER_QUEUE = "order_dead_letter_queue";

    // ======================== RoutingKey ========================

    /**
     * 订单支付成功路由键
     */
    public static final String ROUTING_KEY_PAID = "order.paid";

    /**
     * 订单过期路由键
     */
    public static final String ROUTING_KEY_EXPIRED = "order.expired";

    /**
     * 订单取消路由键
     */
    public static final String ROUTING_KEY_CANCELLED = "order.cancelled";

    /**
     * 订单延迟路由键
     */
    public static final String ROUTING_KEY_DELAY = "order.delay";

    /**
     * 死信路由键
     */
    public static final String ROUTING_KEY_DEAD = "order.dead";

}