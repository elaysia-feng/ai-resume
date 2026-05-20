-- order-service -> ai_resume_order
CREATE DATABASE IF NOT EXISTS `ai_resume_order` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `ai_resume_order`;

CREATE TABLE IF NOT EXISTS `plans` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `name` VARCHAR(50) NOT NULL COMMENT '套餐名: start/plus/max',
    `display_name` VARCHAR(100) NOT NULL COMMENT '显示名',
    `price` INT NOT NULL COMMENT '价格（分）',
    `daily_quota` INT NOT NULL COMMENT '每日面试次数',
    `duration_days` INT NOT NULL DEFAULT 30 COMMENT '有效天数',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='套餐定义表';

CREATE TABLE IF NOT EXISTS `orders` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `order_no` VARCHAR(64) NOT NULL UNIQUE COMMENT '订单号',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `plan_id` BIGINT NOT NULL COMMENT '套餐ID',
    `amount` INT NOT NULL COMMENT '支付金额（分）',
    `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/PAID/EXPIRED/CANCELLED',
    `pay_time` DATETIME COMMENT '支付时间',
    `expire_time` DATETIME COMMENT '订单过期时间',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY `idx_orders_user_id` (`user_id`),
    KEY `idx_orders_plan_id` (`plan_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户订单表';

CREATE TABLE IF NOT EXISTS `user_subscriptions` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `plan_id` BIGINT NOT NULL COMMENT '套餐ID',
    `order_id` BIGINT NOT NULL COMMENT '订单ID',
    `start_time` DATETIME NOT NULL,
    `end_time` DATETIME NOT NULL,
    `daily_used` INT NOT NULL DEFAULT 0 COMMENT '今日已用次数',
    `last_reset_date` DATE COMMENT '上次重置日期',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY `idx_user_subscriptions_user_id` (`user_id`),
    KEY `idx_user_subscriptions_order_id` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户订阅记录表';

CREATE TABLE IF NOT EXISTS `undo_log` (
    `branch_id` BIGINT NOT NULL,
    `xid` VARCHAR(128) NOT NULL,
    `context` VARCHAR(128) NOT NULL,
    `rollback_info` LONGBLOB NOT NULL,
    `log_status` INT NOT NULL,
    `log_created` DATETIME(6) NOT NULL,
    `log_modified` DATETIME(6) NOT NULL,
    UNIQUE KEY `ux_undo_log` (`xid`, `branch_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AT transaction mode undo table';
