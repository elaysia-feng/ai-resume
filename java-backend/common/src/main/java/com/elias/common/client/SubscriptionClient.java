package com.elias.common.client;

import com.elias.common.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.Map;

/**
 * 订阅服务Feign客户端
 * 用于其他服务调用订阅配额相关接口
 */
@FeignClient(name = "order-service", contextId = "subscriptionClient")
public interface SubscriptionClient {

    /**
     * 检查用户今日配额
     *
     * @param userId 用户ID
     * @return 配额信息（调用方自行解析）
     */
    @GetMapping("/api/subscriptions/check")
    ApiResponse<Map<String, Object>> checkQuota(@RequestHeader("X-User-Id") Long userId);

    /**
     * 使用一次配额（面试成功后调用）
     *
     * @param userId 用户ID
     * @return 扣减后的配额信息（调用方自行解析）
     */
    @PostMapping("/api/subscriptions/use")
    ApiResponse<Map<String, Object>> useQuota(@RequestHeader("X-User-Id") Long userId);
}