package com.elias.order.dto.response;

import lombok.Builder;
import lombok.Data;

/**
 * 套餐响应
 */
@Data
@Builder
public class PlanResponse {
    /**
     * 套餐ID
     */
    private Long id;

    /**
     * 套餐名称
     */
    private String name;

    /**
     * 价格（分）
     */
    private Integer price;

    /**
     * 每日配额
     */
    private Integer dailyQuota;

    /**
     * 有效期（天）
     */
    private Integer durationDays;
}