package com.elias.order.service;

import com.elias.common.OrderResponse;
import com.elias.order.dto.response.OrderCreateResponse;
import com.elias.order.dto.response.PageResponse;
import jakarta.validation.Valid;


public interface OrderService {
    // 创建订单
    OrderCreateResponse createOrder(Long planId);
    // 列出所有订单
    PageResponse<OrderResponse> listOrder(Long userId, Integer pageNum, Integer pageSize);
    // 按订单号查询订单
    OrderResponse queryOrder(@Valid String orderNo);

    // 更新订单状态
    void updateOrderStatus(String orderNo, String status);
}
