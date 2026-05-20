package com.elias.common.mq;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OrderMessage {
    private String orderNo;
    // PAID / EXPIRED / CANCELLED
    private String eventType;
    private Long userId;
    private Long planId;
}