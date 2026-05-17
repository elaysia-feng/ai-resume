package com.airesumeforge.notification.mq;

import com.airesumeforge.client.OrderClient;
import com.airesumeforge.client.PayClient;
import com.airesumeforge.common.ApiResponse;
import com.airesumeforge.common.OrderResponse;
import com.airesumeforge.common.PayStatus;
import com.airesumeforge.exception.BusinessException;
import com.airesumeforge.mq.MqConstants;
import com.airesumeforge.mq.MqProducer;
import com.airesumeforge.mq.MultiDelayMessage;
import com.airesumeforge.mq.OrderMessage;
import com.airesumeforge.notification.service.NotificationService;
import com.rabbitmq.client.Channel;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;


@Slf4j
@Component
@RequiredArgsConstructor
public class MqConsumer {
    private final OrderClient orderClient;
    private final MqProducer mqProducer;
    private final NotificationService notificationService;

    // 更新支付成功的queue
    @RabbitListener(queues = MqConstants.PAID_QUEUE)
    public void updateOrderToPaid(OrderMessage message, Channel channel, @Header Long channelId) throws IOException {
        log.info("收到消息 - orderNo: {}, eventType: {}", message.getOrderNo(), message.getEventType());

        try{
            // 幂等检查
            if (!canUpdateOrder(message.getOrderNo())) {
                channel.basicAck(channelId, false);
                return;
            }

            // 下游业务：发短信、开通会员、记录日志等
            // 1. 发邮件通知用户
            notificationService.sendPaidSms(message.getUserId(), message.getOrderNo());

            // 2. 开通会员/增加额度
            orderClient.createQuota(message.getPlanId(), message.getOrderNo());
            // 3. 发站内信/推送
             notificationService.sendPaidNotice(message.getUserId(), message.getOrderNo());

            channel.basicAck(channelId, false);
        } catch (IOException e) {
            // 4. 失败 → NACK，重新入队（requeue=true）
            //  拒绝，让消息重新排队
            channel.basicNack(channelId, false, true);
        }
    }
    // TODO 现在是用的 direct 交换机, 后续如果需要的话可以换成topic
    @RabbitListener(queues = MqConstants.ROUTING_KEY_DELAY)
    public void handlerDelayMessage(MultiDelayMessage<String> message, Channel channel, @Header Long channelId) throws IOException {
        log.info("收到消息 - 还可以重试size{}", message.getDelays().size());
        try{
            // 1. 查询订单状态
            ApiResponse<OrderResponse> order = orderClient.queryOrder(message.getData());

            // 2. 校验订单, 如果订单存在话, 就校验状态 -> 如果状态为 Paid 返回ack 并且删除队列里面的消息
            if (order == null) {
                // 第一个是 bool 类型: 是否批量拒绝, 第二个是 是否将消息重新放回队列
                channel.basicNack(channelId, false, false);
            }
            // 2.1 状态为已支付, 并删除订单 (只要返回ack 给mq,订单就会自动删除)
            assert order != null;
            if (PayStatus.PAID.getStatus().equals(order.getData().getStatus())) {
                channel.basicAck(channelId, false);
            }

            // 3. 未支付，如果没有下次检查
            if (!PayStatus.PAID.getStatus().equals(order.getData().getStatus())) {
                if (! message.hasNextDelay()){
                    // 3.1 取消订单
                    orderClient.updateOrderStatus(message.getData(), PayStatus.CANCELLED.getStatus());
                    channel.basicAck(channelId, false);
                }
            }

            // 4. 还有下一次检查 → 继续发延迟消息
            long nextDelayTime = message.removeAndGetCurrent();

            if (nextDelayTime > 0) {
                log.info("订单仍未支付，继续检查 - orderNo: {}, 下次延迟: {}ms",
                        message.getData(), nextDelayTime);
                // 从新发消息
                mqProducer.sendOrderStatusUpdateWithDelay(MqConstants.ROUTING_KEY_DELAY,
                        new MultiDelayMessage<String>(message.getData(), new ArrayList<>(message.getDelays())));
            }
            // 5. 防止消息永远卡在队列里
            channel.basicAck(channelId, false);
        }catch (Exception e){
            log.error("处理过期消息失败 - orderNo: {}", message.getData(), e);
            channel.basicNack(channelId, false, true);
        }

    }


    // TODO 死信队列消费
    @RabbitListener(queues = MqConstants.DEAD_LETTER_QUEUE)
    public void handleDeadLetter(Message message, Channel channel,
                                 @Header Long channelId) throws IOException {
        Map<String, Object> headers = message.getMessageProperties().getHeaders();

        log.error("========== 死信消息 ==========");
        log.error("原始交换机: {}", headers.get("x-first-death-exchange"));
        log.error("原始队列: {}", headers.get("x-first-death-queue"));
        log.error("死亡原因: {}", headers.get("x-first-death-reason"));
        log.error("消息内容: {}", new String(message.getBody()));

        // 死信消息处理后直接 ACK，防止堆积
        channel.basicAck(channelId, false);
    }


    // 做的幂等判断, TODO 做商品库存
    private boolean canUpdateOrder(String orderNo) {
        try {
            ApiResponse<OrderResponse> response = orderClient.queryOrder(orderNo);
            if (response == null || response.getData() == null) {
                return true;
            }
            return !PayStatus.PAID.getStatus().equals(response.getData().getStatus());
        } catch (Exception e) {
            log.warn("查询订单状态失败，允许更新 - orderNo: {}", orderNo, e);
            return true;
        }
    }
}
