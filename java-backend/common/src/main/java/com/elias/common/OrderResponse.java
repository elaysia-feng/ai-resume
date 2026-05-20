package com.elias.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
// 这个注解是让空字段不返回
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrderResponse {
    /**
     * 用户号
     */
    private Long userId;

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
     * 支付时间
     */
    private LocalDateTime payTime;

    /**
     * 订单过期时间
     */
    private LocalDateTime expireTime;

    /**
     * 支付链接
     */
    private String payUrl;


}
