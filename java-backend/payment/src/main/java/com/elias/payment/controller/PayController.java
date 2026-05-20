package com.elias.payment.controller;

import com.elias.common.ApiResponse;
import com.elias.payment.service.AlipayService;
import com.alipay.api.AlipayApiException;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import com.elias.payment.dto.request.AlipayCallbackRequest;

@Slf4j
@RestController
@RequestMapping("/api/pay")
@RequiredArgsConstructor
public class PayController {
    private AlipayService alipayService;

    @GetMapping("/create")
    // TODO 封装这个传入参数为一个类
    public ApiResponse<String> createPayUrl(
            @RequestParam String orderNo,
            @RequestParam Long amount,
            @RequestParam String subject) throws AlipayApiException, JsonProcessingException {
        return ApiResponse.ok(alipayService.createPaymentUrl(orderNo, amount, subject));
    }


    @PostMapping("/callback")
    public String alipayCallback(AlipayCallbackRequest request) {
        log.info("收到支付宝回调 - outTradeNo: {}, tradeStatus: {}",
                request.getOutTradeNo(), request.getTradeStatus());
        return alipayService.handleAlipayCallback(request);
    }


}
