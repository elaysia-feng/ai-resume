package com.airesumeforge.order.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 订单详情响应
 */
@Data
@Builder
public class OrderCreateResponse {
    /**
     * 订单号
     */
    private String orderNo;

    /**
     * 套餐ID
     */
    private Long planId;

    /**
     * 支付金额（分）
     */
    private Integer amount;

    /**
     * 订单状态
     */
    private String status;

    /**
     * 订单过期时间
     */
    private LocalDateTime expireTime;

    /**
     * 支付链接
     */
    private String payUrl;
}