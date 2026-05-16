package com.airesumeforge.order.service.impl;

import com.airesumeforge.order.dto.response.PlanResponse;
import com.airesumeforge.order.entity.Plan;
import com.airesumeforge.order.mapper.PlanMapper;
import com.airesumeforge.order.service.PlanService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
@RequiredArgsConstructor
public class PlanServiceImpl implements PlanService {
    private final PlanMapper planMapper;

    @Override
    public List<PlanResponse> getAllPlans() {
        List<Plan> plans = planMapper.selectList(new LambdaQueryWrapper<Plan>());

        return plans.stream()
                .map(plan -> PlanResponse.builder()
                        .id(plan.getId())
                        .name(plan.getName())
                        .price(plan.getPrice())
                        .dailyQuota(plan.getDailyQuota())
                        .durationDays(plan.getDurationDays())
                        .build()
                )
                .toList();
    }
}
