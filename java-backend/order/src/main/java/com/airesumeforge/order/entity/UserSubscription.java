package com.airesumeforge.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 用户订阅实体类
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("user_subscriptions")
public class UserSubscription {

    /**
     * 订阅ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 套餐ID 为 -1 表示免费套餐
     */
    private Long planId;

    /**
     * 订单ID
     */
    private Long orderId;

    /**
     * 订阅开始时间
     */
    private LocalDateTime startTime;

    /**
     * 订阅结束时间
     */
    private LocalDateTime endTime;

    /**
     * 今日已使用次数
     */
    private Integer dailyUsed;

    /**
     * 一天内剩余额度
     */
    private Integer quotaLefToday;

    /**
     * 本周剩余额度
     */
    private Integer quotaLeftThisWeek;

    /**
     * 上次重置日期
     */
    private LocalDate lastResetDate;

    /**
     * 创建时间
     */
    @TableField(fill = com.baomidou.mybatisplus.annotation.FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @TableField(fill = com.baomidou.mybatisplus.annotation.FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}