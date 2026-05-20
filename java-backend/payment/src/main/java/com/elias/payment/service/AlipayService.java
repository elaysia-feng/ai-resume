package com.elias.payment.service;

import com.elias.payment.dto.request.AlipayCallbackRequest;
import com.alipay.api.AlipayApiException;
import com.fasterxml.jackson.core.JsonProcessingException;

public interface AlipayService {
    // 创建订单支付的 url
    public String createPaymentUrl(String orderNo, Long amount, String subject) throws JsonProcessingException, AlipayApiException;

    // 处理支付宝账单回调
    String handleAlipayCallback(AlipayCallbackRequest request);
}
