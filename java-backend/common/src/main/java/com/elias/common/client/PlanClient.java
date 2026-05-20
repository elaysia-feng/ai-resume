package com.elias.common.client;

import com.elias.common.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
import java.util.Map;

/**
 * 套餐服务Feign客户端
 * 用于其他服务调用套餐相关接口
 */
@FeignClient(name = "order-service", contextId = "planClient")
public interface PlanClient {

    /**
     * 查询所有套餐列表
     *
     * @return 套餐列表（调用方自行解析）
     */
    @GetMapping("/api/plans")
    ApiResponse<List<Map<String, Object>>> getAllPlans();
}