package com.airesumeforge.order.controller;

import com.airesumeforge.common.ApiResponse;
import com.airesumeforge.context.UserContext;
import com.airesumeforge.order.dto.request.CreateOrderRequest;
import com.airesumeforge.common.OrderResponse;
import com.airesumeforge.order.dto.response.OrderCreateResponse;
import com.airesumeforge.order.dto.response.PageResponse;
import com.airesumeforge.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 订单控制器
 * 提供订单创建和查询相关接口
 */
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    /**
     * 创建订单
     * @param request 创建订单请求
     * @return 订单信息，包含支付链接
     */
    @PostMapping
    public ApiResponse<OrderCreateResponse> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        return ApiResponse.ok(orderService.createOrder(request.getPlanId()));
    }

    /**
     * 查询用户的所有订单
     *
     * @return 订单信息
     */
    @GetMapping("/listOrder")
    public ApiResponse<PageResponse<OrderResponse>> listOrder(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {

        Long userId = UserContext.getUserIdLong();
        return ApiResponse.ok(orderService.listOrder(userId, pageNum, pageSize));
    }

    /**
     * 查询指定的订单信息
     * @param orderNo 订单号
     * @return 返回改订单号的详情
     */
    @GetMapping("/order")
    public ApiResponse<OrderResponse> queryOrder(@Valid @RequestParam String orderNo) {
        return ApiResponse.ok(orderService.queryOrder(orderNo));
    }


    /**
     * 更新订单状态, 一般内部调用
     * @param orderNo 订单编号
     * @param status 状态
     * @return void
     */
    @PutMapping("/status")
    public ApiResponse<Void> updateOrderStatus(@RequestParam String orderNo,
                                          @RequestParam String status) {
        orderService.updateOrderStatus(orderNo, status);
        return ApiResponse.ok();
    }


}