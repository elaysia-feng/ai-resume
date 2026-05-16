package com.airesumeforge.client;

import com.airesumeforge.common.ApiResponse;
import com.airesumeforge.common.OrderResponse;
import jakarta.validation.Valid;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "payment-service")
public interface PayClient {
    @GetMapping("/api/pay/create")
    ApiResponse<String> createPayUrl(
            @RequestParam String orderNo,      // → ?orderNo=xxx
            @RequestParam Long amount,         // → &amount=200
            @RequestParam String subject        // → &subject=xxx
    );
}
