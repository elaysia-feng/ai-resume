package com.elias.common.mq;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import static com.elias.common.mq.MqConstants.ORDER_EXCHANGE;

/**
 * MQ 生产者 - 各服务注入使用
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MqProducer {


    // ========================= 过期时间数组 ==========================
    // ← long[] = long类型的数组
    public static final long[] ORDER_EXPIRE_SHARDS = {
            // 1分钟
            60_000L,
            // 2分钟
            120_000L,
            // 5分钟
            300_000L,
            // 10分钟
            600_000L,
            // 12分钟 (总计30分钟)
            720_000L
    };
    private final RabbitTemplate rabbitTemplate;

    /**
     * 发送订单状态变更消息
     *
     * @param routingKey 路由键：order.paid / order.expired/ order.cancelled/
     * @param message    消息体
     */
    public void sendOrderStatusUpdate(String routingKey, OrderMessage message) {
        rabbitTemplate.convertAndSend(ORDER_EXCHANGE, routingKey, message);
        log.info("发送MQ消息成功 - routingKey: {}, orderNo: {}, eventType: {}",
                routingKey, message.getOrderNo(), message.getEventType());
    }


    /**
     * 发送订单过期检查消息（延迟）
     *
     * @param routingKey  order.expired / order.paid 等
     * @param message     消息体
     */
    public void sendOrderStatusUpdateWithDelay(String routingKey, MultiDelayMessage<String> message) {

        long delay = message.removeAndGetCurrent();

        rabbitTemplate.convertAndSend(
                ORDER_EXCHANGE,
                routingKey,
                message,
                new DelayMessagePostProcessor(delay)

        );
        log.info("发送延迟MQ消息 - routingKey: {}, orderNo: {}, delay: {}ms",
                routingKey, message.getData(), delay);
    }

}