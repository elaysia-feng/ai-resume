package com.elias.order.service.impl;

import com.elias.common.context.UserContext;
import com.elias.common.exception.BusinessException;
import com.elias.order.dto.response.QuotaResponse;
import com.elias.order.entity.Plan;
import com.elias.order.entity.UserSubscription;
import com.elias.order.mapper.PlanMapper;
import com.elias.order.mapper.UserSubscriptionMapper;
import com.elias.order.service.SubscriptionService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class SubscriptionServiceImpl implements SubscriptionService {

    private final UserSubscriptionMapper userSubscriptionMapper;
    private final PlanMapper planMapper;
    private final RedisTemplate redisTemplate;

    // 配额信息
    @Override
    public QuotaResponse checkQuota() {
        Long userId = UserContext.verifyGetUserId();
        UserSubscription userSubscription = userSubscriptionMapper.selectOne(new LambdaQueryWrapper<UserSubscription>()
                .eq(UserSubscription::getUserId, userId)
                .orderByDesc(UserSubscription::getCreatedAt)
                .last("limit 1"));
        return QuotaResponse.builder()
                .dailyQuota(userSubscription.getQuotaLefToday())
                .dailyUsed(userSubscription.getDailyUsed())
                .remaining(Math.max(0, userSubscription.getQuotaLefToday() - userSubscription.getDailyUsed()))
                .hasQuota((userSubscription.getQuotaLefToday() - userSubscription.getDailyUsed()) > 0)
                .build();
    }
    // 使用一次配额（面试成功后调用）
    @Override
    @Transactional
    public QuotaResponse useQuota() {
        Long userId = UserContext.verifyGetUserId();

        // 幂等校验：同一用户3秒内只能提交一次
        String key = "quota:use:" + userId;
        Boolean success = redisTemplate.opsForValue().setIfAbsent(key, "1", 3, TimeUnit.SECONDS);


        if (!success) {
            throw BusinessException.business("请求过于频繁，请稍后重试");
        }

        try {
            // 查询用户订阅
            UserSubscription sub = userSubscriptionMapper.selectOne(
                    new LambdaQueryWrapper<UserSubscription>()
                            .eq(UserSubscription::getUserId, userId)
                            .orderByDesc(UserSubscription::getCreatedAt)
                            .last("limit 1")
            );

            // 校验
            if (sub == null) {
                throw BusinessException.business("该用户无订阅");
            }

            int quotaLeftToday = sub.getQuotaLefToday() - sub.getDailyUsed();
            if (quotaLeftToday <= 0) {
                throw BusinessException.business("今日配额已用完");
            }

            if (sub.getQuotaLeftThisWeek() <= 0) {
                throw BusinessException.business("本周配额已用完");
            }

            // 乐观锁更新（扣减今日配额和本周配额）
            int updated = userSubscriptionMapper.update(
                    UserSubscription.builder()
                            .dailyUsed(sub.getDailyUsed() + 1)
                            .quotaLeftThisWeek(sub.getQuotaLeftThisWeek() - 1)
                            .build(),
                    new LambdaUpdateWrapper<UserSubscription>()
                            .eq(UserSubscription::getUserId, userId)
                            .eq(UserSubscription::getQuotaLefToday, sub.getQuotaLefToday())
            );

            if (updated == 0) {
                throw BusinessException.business("配额更新失败，请重试");
            }

            // 重新查询获取最新数据
            UserSubscription updatedSub = userSubscriptionMapper.selectOne(
                    new LambdaQueryWrapper<UserSubscription>()
                            .eq(UserSubscription::getUserId, userId)
                            .orderByDesc(UserSubscription::getCreatedAt)
                            .last("limit 1")
            );

            int remainingToday = updatedSub.getQuotaLefToday() - updatedSub.getDailyUsed();

            return QuotaResponse.builder()
                    .dailyUsed(updatedSub.getDailyUsed())
                    .dailyQuota(updatedSub.getQuotaLefToday())
                    .remaining(remainingToday)
                    .hasQuota(remainingToday > 0)
                    .build();

        } finally {
            // 锁自动过期，不需要手动删除
        }
    }
    @Override
    public Void createQuota(Long planId, String orderId) {

        Plan plan = planMapper.selectOne(new LambdaQueryWrapper<Plan>().eq(Plan::getId, planId).orderByDesc(Plan::getCreatedAt).last("limit 1"));
        // 简单处理plan
        if (plan == null) {
            return null;
        }

        UserSubscription subscription = UserSubscription.builder()
                .userId(UserContext.verifyGetUserId())
                .planId(-1L)
                .startTime(LocalDateTime.now())
                .endTime(LocalDateTime.now().plusDays(planMapper.selectById(planId).getDurationDays()))
                .lastResetDate(LocalDate.now())
                .build();

        if (planId == 1 || planId == 2 || planId == 3) {
            subscription.setPlanId(planId);
            subscription.setDailyUsed(0);
            subscription.setQuotaLefToday(plan.getDailyQuota());
            subscription.setQuotaLeftThisWeek(plan.getDailyQuota() * 7);
        } else {
            throw new IllegalArgumentException("未知 planId: " + planId);
        }



        userSubscriptionMapper.insert(subscription);
        return null;
    }

}
