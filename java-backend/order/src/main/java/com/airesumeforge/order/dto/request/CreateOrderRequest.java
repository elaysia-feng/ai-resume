package com.airesumeforge.order.dto.request;

import lombok.Data;

/**
 * 创建订单请求
 */
@Data
public class CreateOrderRequest {
    /**
     * 套餐ID
     */
    private Long planId;
}