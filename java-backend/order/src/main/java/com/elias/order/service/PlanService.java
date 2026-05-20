package com.elias.order.service;

import com.elias.order.dto.response.PlanResponse;

import java.util.List;

public interface PlanService {
    // 查询并获取所有的resumePlan
    List<PlanResponse> getAllPlans();
}
