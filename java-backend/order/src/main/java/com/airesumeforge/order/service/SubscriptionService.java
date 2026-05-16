package com.airesumeforge.order.service;

import com.airesumeforge.order.dto.response.QuotaResponse;
import jakarta.validation.Valid;

public interface SubscriptionService {
    // 配额信息
    QuotaResponse checkQuota();
    // 使用一次配额（面试成功后调用）
    QuotaResponse useQuota();
    // 创建个人额度
    Void createQuota(@Valid Long planId, @Valid String orderId);
}
