package com.airesumeforge.payment.service.impl;

import com.airesumeforge.client.OrderClient;
import com.airesumeforge.common.ApiResponse;
import com.airesumeforge.common.OrderResponse;
import com.airesumeforge.common.PayStatus;
import com.airesumeforge.context.UserContext;
import com.airesumeforge.exception.BusinessException;
import com.airesumeforge.mq.MqConstants;
import com.airesumeforge.mq.MqProducer;
import com.airesumeforge.mq.OrderMessage;
import com.airesumeforge.payment.dto.request.AlipayCallbackRequest;
import com.airesumeforge.payment.service.AlipayService;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.alipay.api.response.AlipayTradePagePayResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AliPayServiceImpl implements AlipayService {

    private final AlipayClient alipayClient;
    private final ObjectMapper objectMapper;
    private final OrderClient orderClient;
    private final MqProducer mqProducer;


    /**
     *  创建订单url
     * @param orderNo 订单号
     * @param amount 订单金额
     * @param subject 订阅的商品名称
     * @return 支付宝支付url地址
     */
    @Override
    // TODO 可以考虑多线程同时生成支付路径
    public String createPaymentUrl(String orderNo, Long amount, String subject) throws JsonProcessingException, AlipayApiException {
        // 1. 创建请求
        AlipayTradePagePayRequest request = new AlipayTradePagePayRequest();

        // 2. 设置参数
        // 用map构建BizContent(map -> Json)
        Map<String, Object> bizContent = new HashMap<>();
        // 30分钟后过期
        bizContent.put("timeout_express", "30m");
        bizContent.put("out_trade_no", orderNo);
        bizContent.put("product_code", "FAST_INSTANT_TRADE_PAY");
        bizContent.put("total_amount", new BigDecimal(amount).divide(BigDecimal.valueOf(100)));
        bizContent.put("subject", subject);
        request.setBizContent(objectMapper.writeValueAsString(bizContent));

        // 3. 设置回调地址（可选）, 支付宝支付完成之后就会跳转
        request.setReturnUrl("http://localhost:3000/order/result");

        // 4. 调用API
        AlipayTradePagePayResponse response = alipayClient.pageExecute(request);

        // 5. 返回支付链接
        if (!response.isSuccess()) {
            throw BusinessException.business("生成支付url失败");
        }
        return response.getBody();
    }

    @Override
    public String handleAlipayCallback(AlipayCallbackRequest request) {
        // 1. 验签失败直接返回
        if (!signVerified(request)) {
            log.error("支付宝回调验签失败 - outTradeNo: {}", request.getOutTradeNo());
            return "fail";
        }

        // 2. 只处理支付成功的状态
        String tradeStatus = request.getTradeStatus();
        if (!"TRADE_SUCCESS".equals(tradeStatus) && !"TRADE_FINISHED".equals(tradeStatus)) {
            log.info("支付未成功，不处理 - outTradeNo: {}, tradeStatus: {}",
                    request.getOutTradeNo(), tradeStatus);
            return "success";
        }

        // 3.支付成功
        OrderResponse order = orderClient.queryOrder(request.getOutTradeNo()).getData();
        if (order == null) {
            log.error("订单不存在 - outTradeNo: {}", request.getOutTradeNo());
            return "fail";
        }

        // 4. 通知mq
        OrderMessage message = OrderMessage.builder()
                .orderNo(order.getOrderNo())
                .eventType(PayStatus.PAID.getStatus())
                .planId(order.getPlanId())
                .userId(UserContext.verifyGetUserId())
                .build();
        mqProducer.sendOrderStatusUpdate(MqConstants.ROUTING_KEY_PAID, message);

        log.info("支付宝回调处理成功，已发MQ消息 - outTradeNo: {}, tradeNo: {}",
                request.getOutTradeNo(), request.getTradeNo());
        return "success";
    }

    private boolean signVerified(AlipayCallbackRequest request) {
        if (request == null || request.getOutTradeNo() == null || request.getOutTradeNo().trim().isEmpty()) {
            return false;
        }
        return true;
    }
}
