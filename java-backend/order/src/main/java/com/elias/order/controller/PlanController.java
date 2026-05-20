package com.elias.order.controller;

import com.elias.common.ApiResponse;
import com.elias.order.dto.response.PlanResponse;
import com.elias.order.service.PlanService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 套餐控制器
 * 提供套餐查询相关接口
 */
@RestController
@RequestMapping("/api/plans")
@RequiredArgsConstructor
public class PlanController {

    private final PlanService planService;

    /**
     * 查询所有套餐列表
     *
     * @return 套餐列表
     */
    @GetMapping
    public ApiResponse<List<PlanResponse>> getAllPlans() {
        return ApiResponse.ok(planService.getAllPlans());
    }
}