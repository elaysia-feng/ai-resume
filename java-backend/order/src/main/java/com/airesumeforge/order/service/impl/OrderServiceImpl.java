package com.airesumeforge.order.service.impl;

import com.airesumeforge.client.PayClient;
import com.airesumeforge.client.UserClient;
import com.airesumeforge.common.ApiResponse;
import com.airesumeforge.common.PayStatus;
import com.airesumeforge.common.UserInfoDTO;
import com.airesumeforge.context.UserContext;
import com.airesumeforge.exception.BusinessException;
import com.airesumeforge.common.OrderResponse;
import com.airesumeforge.order.dto.response.OrderCreateResponse;
import com.airesumeforge.order.dto.response.PageResponse;
import com.airesumeforge.order.entity.Order;
import com.airesumeforge.order.entity.Plan;
import com.airesumeforge.order.mapper.OrderMapper;
import com.airesumeforge.order.mapper.PlanMapper;
import com.airesumeforge.order.service.OrderService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;

// TODO 加入事务

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {
    
    private final UserClient userClient;
    private final PayClient payClient;
    private final PlanMapper planMapper;
    private final OrderMapper orderMapper;

    /**
     * 创建订单
     * @param planId 用户订阅的Plan
     * @return 返回订阅的订单详情
     */
    // TODO 如果某一时刻很多用户同时创建很多订单, 还有就是注意 一个用户一个月内(还没过期内只能买同种类型的Plan一次)
    @Override
    public OrderCreateResponse createOrder(Long planId) {
        ApiResponse<UserInfoDTO> userInfo = userClient.getCurrentUser();

        // 看是否有该Plan
        Plan orderPlan = planMapper.selectById(planId);
        if (planId == null || orderPlan == null) {
            throw BusinessException.notFound("没找到订阅计划");
        }

        long needPay = orderPlan.getPrice().longValue();

        Order order = orderMapper.selectOne(new LambdaQueryWrapper<Order>()
                .eq(Order::getUserId, userInfo.getData().getUserId())
                .orderByDesc(Order::getExpireTime).last("limit 1"));

        // 判断是否买过同种订单
        if (order != null && PayStatus.PAID.getStatus().equals(order.getStatus())) {
            if (order.getPlanId().equals(planId) && order.getExpireTime().isAfter(LocalDateTime.now())) {
                throw BusinessException.business("当月不能购买同类型Plan");
            }
            // 订阅的Plan用户需要升级的:
            else if (order.getExpireTime().isAfter(LocalDateTime.now()) && order.getPlanId().compareTo(planId) > 0) {
                // 选择的订阅等级更高
                // 相差天数
                long duration = Duration.between(LocalDateTime.now(), order.getExpireTime()).toDays();

                // 剩余的订阅价值
                long surplus= order.getAmount() / (30 - duration);

                // 还需要支付的
                needPay = orderPlan.getPrice().longValue() - surplus;
            }
        }

        // 没有买过
        String orderNo = generateOrderNo();

        // 创建订单
        order = Order.builder()
                .orderNo(orderNo)
                .status(PayStatus.PENDING.getStatus())
                .amount((int) needPay)
                .expireTime(LocalDateTime.now().plusDays(orderPlan.getDurationDays().longValue()))
                .userId(userInfo.getData().getUserId())
                .planId(planId)
                .expireTime(LocalDateTime.now().plusDays(orderPlan.getDurationDays().longValue()))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // 生成支付链接
        String payUrl = payClient.createPayUrl(orderNo, orderPlan.getPrice().longValue(), orderPlan.getName()).getData();
        order.setPayUrl(payUrl);
        // 插入订单
        orderMapper.insert(order);

        return  OrderCreateResponse.builder()
                .orderNo(order.getOrderNo())
                .planId(order.getPlanId())
                .amount(order.getAmount())
                .status(PayStatus.PENDING.getStatus())
                .payUrl(payUrl)
                .build();
    }

    /**
     *  列出该用户的所有订单,返回给前端
     * @return 列出订单
     */
    @Override
    public PageResponse<OrderResponse> listOrder(Long userId, Integer pageNum, Integer pageSize) {
        ApiResponse<UserInfoDTO> currentUser = userClient.getCurrentUser();
        if (currentUser == null) {
            throw BusinessException.business("不存在用户请先注册");
        }
        Page<Order> page = new Page<>(pageNum, pageSize);

        Page<Order> orderPage = orderMapper.selectPage(page, new LambdaQueryWrapper<Order>().eq(Order::getUserId, userId).orderByDesc(Order::getCreatedAt));
        List<Order> listOrder = orderPage.getRecords();

        List<OrderResponse> listOrderResponse = listOrder.stream().map(order ->
                OrderResponse.builder()
                        .orderNo(order.getOrderNo())
                        .planId(order.getPlanId())
                        .amount(order.getAmount())
                        .status(order.getStatus())
                        .payTime(order.getPayTime() != null ? order.getPayTime() : null)
                        .expireTime(order.getExpireTime())
                        .build()
        ).toList();


       return PageResponse.<OrderResponse>builder()
                .list(listOrderResponse)
                .total(orderPage.getTotal())
                .pageNum(pageNum)
                .pageSize(pageSize)
                .build();
    }

    // 按订单号查询订单
    @Override
    public OrderResponse queryOrder(String orderNo) {
        Long userId = UserContext.verifyGetUserId();

        Order order = orderMapper.selectOne(new LambdaQueryWrapper<Order>().eq(Order::getOrderNo, orderNo)
                .orderByDesc(Order::getCreatedAt).last("limit 1"));

        if (order == null || !order.getUserId().equals(userId)) {
            throw BusinessException.business("该订单不存在");
        }


        switch (PayStatus.valueOf(order.getStatus())) {
            case PAID -> {
                /* 已支付 */
                return OrderResponse.builder()
                        .orderNo(orderNo)
                        .status(order.getStatus())
                        .payTime(order.getPayTime())
                        .planId(order.getPlanId())
                        .amount(order.getAmount())
                        .build();
                }
            case PENDING -> {
                /* 待支付 */
                return OrderResponse.builder()
                        .orderNo(orderNo)
                        .status(order.getStatus())
                        .payTime(order.getPayTime())
                        .planId(order.getPlanId())
                        .amount(order.getAmount())
                        .expireTime(order.getExpireTime())
                        .payUrl(order.getPayUrl())
                        .build();
            }
            case EXPIRED -> {
                /* 已过期 */
                return OrderResponse.builder()
                        .orderNo(orderNo)
                        .status(order.getStatus())
                        .payTime(order.getPayTime())
                        .planId(order.getPlanId())
                        .amount(order.getAmount())
                        .expireTime(order.getExpireTime())
                        .build();
            }
            case CANCELLED -> {
                /* 已取消 */
                return OrderResponse.builder()
                        .orderNo(orderNo)
                        .status(order.getStatus())
                        .payTime(order.getPayTime())
                        .planId(order.getPlanId())
                        .amount(order.getAmount())
                        .expireTime(order.getExpireTime())
                        .build();
            }
            default -> {
                throw BusinessException.business("订单状态错误");
            }
        }
    }

    /**
     * 更新订单状态
     * @param orderNo 订单号
     * @param status 订单状态
     */

    // TODO 加入分布式锁
    @Override
    public void updateOrderStatus(String orderNo, String status) {
        Long userId = UserContext.verifyGetUserId();
        Order order = orderMapper.selectOne(new LambdaQueryWrapper<Order>()
                .eq(Order::getOrderNo, orderNo)
                .orderByDesc(Order::getCreatedAt).last("limit 1"));

        if (order == null || !order.getUserId().equals(userId)) {
            throw BusinessException.business("该用户无该订单");
        }

        // 更新订单状态
        order.setStatus(status);
        order.setPayTime(LocalDateTime.now());
        orderMapper.updateById(order);
    }

    private String generateOrderNo() {
        // 格式：PL + 年月日时分秒 + 6位随机数
        // 例如：PL20260513143045000001
        return "PL" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + String.format("%05d", new Random().nextInt(100000));
    }
}
