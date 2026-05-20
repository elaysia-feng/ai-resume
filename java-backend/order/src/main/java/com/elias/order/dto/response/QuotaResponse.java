package com.elias.order.dto.response;

import lombok.Builder;
import lombok.Data;

/**
 * 配额响应
 */
@Data
@Builder
public class QuotaResponse {
    /**
     * 是否有配额
     */
    private Boolean hasQuota;

    /**
     * 每日配额
     */
    private Integer dailyQuota;

    /**
     * 今日已用
     */
    private Integer dailyUsed;

    /**
     * 剩余次数
     */
    private Integer remaining;
}