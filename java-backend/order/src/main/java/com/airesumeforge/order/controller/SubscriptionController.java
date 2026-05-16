package com.airesumeforge.order.controller;

import com.airesumeforge.common.ApiResponse;
import com.airesumeforge.order.dto.response.QuotaResponse;
import com.airesumeforge.order.service.SubscriptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 订阅配额控制器
 * 提供订阅配额查询和扣减相关接口
 */
@RestController
@RequestMapping("/api/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    /**
     * 创建配额
     * @return Void
     */
    @PostMapping("/create")
    public ApiResponse<Void> createQuota(@Valid @RequestParam Long planId,  @RequestParam(required = false) String orderId) {


        return ApiResponse.ok(subscriptionService.createQuota(planId, orderId));
    }

    /**
     * 检查用户今日配额
     *
     * @return 配额信息
     */
    @GetMapping("/check")
    public ApiResponse<QuotaResponse> checkQuota() {
        return ApiResponse.ok(subscriptionService.checkQuota());
    }

    /**
     * 使用一次配额（面试成功后调用）
     *
     * @return 扣减后的配额信息
     */
    @PostMapping("/use")
    public ApiResponse<QuotaResponse> useQuota() {
        return ApiResponse.ok(subscriptionService.useQuota());
    }
}