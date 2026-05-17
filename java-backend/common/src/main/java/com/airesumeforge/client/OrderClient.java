package com.airesumeforge.client;

import com.airesumeforge.common.ApiResponse;
import com.airesumeforge.common.OrderResponse;
import jakarta.validation.Valid;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "order-service")
public interface OrderClient {
    @GetMapping("/api/orders/order")
    ApiResponse<OrderResponse> queryOrder(@Valid @RequestParam String orderNo);


    @PutMapping("/api/orders/status")
    ApiResponse<Void> updateOrderStatus(@RequestParam String orderNo,
                                        @RequestParam String status);

    /**
     * 创建配额
     * @return Void
     */
    @PostMapping("/api/subscriptions/create")
    ApiResponse<Void> createQuota(@Valid @RequestParam Long planId,  @RequestParam(required = false) String orderId);
}
