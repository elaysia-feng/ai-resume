package com.airesumeforge.order.service;

import com.airesumeforge.order.dto.response.PlanResponse;

import java.util.List;

public interface PlanService {
    // 查询并获取所有的resumePlan
    List<PlanResponse> getAllPlans();
}
