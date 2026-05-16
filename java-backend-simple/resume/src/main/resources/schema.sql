-- AI Resume Forge 数据库初始化脚本
-- MySQL 8.0+

-- ============================================================
-- 用户表
-- ============================================================
CREATE TABLE IF NOT EXISTS `users` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '用户ID，主键自增',
    `username` VARCHAR(100) NOT NULL UNIQUE COMMENT '用户名，唯一',
    `email` VARCHAR(255) NOT NULL UNIQUE COMMENT '邮箱，唯一',
    `password` VARCHAR(255) NOT NULL COMMENT '密码，BCrypt加密存储',
    `avatar_url` VARCHAR(500) COMMENT '头像URL',
    `enabled` BOOLEAN DEFAULT TRUE COMMENT '账户是否启用',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX `idx_users_email` (`email`),
    INDEX `idx_users_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- ============================================================
-- 简历主表
-- ============================================================
CREATE TABLE IF NOT EXISTS `resumes` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '简历ID，主键自增',
    `user_id` BIGINT NOT NULL COMMENT '所属用户ID，关联users.id',
    `title` VARCHAR(255) COMMENT '简历标题，默认"我的简历"',
    `template` VARCHAR(100) COMMENT '模板名：classic / modern / creative',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX `idx_resumes_user_id` (`user_id`),
    CONSTRAINT `fk_resumes_user_id` FOREIGN KEY (`user_id`) REFERENCES `users`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='简历主表';

-- ============================================================
-- 简历章节表
-- ============================================================
CREATE TABLE IF NOT EXISTS `resume_sections` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '章节ID，主键自增',
    `resume_id` BIGINT NOT NULL COMMENT '所属简历ID，关联resumes.id',
    `section_code` VARCHAR(50) NOT NULL COMMENT '章节代码：BASIC / JOB_INTENT / SUMMARY / EXPERIENCE 等',
    `section_title` VARCHAR(255) COMMENT '章节标题，如"工作经验"',
    `section_type` VARCHAR(20) NOT NULL COMMENT '章节类型：SYSTEM（系统内置不可删）/ CUSTOM（用户自定义可删）',
    `schema_type` VARCHAR(20) NOT NULL COMMENT '内容Schema类型：TEXT（单文本）/ LIST（列表）/ TAGS（标签组）',
    `content_json` LONGTEXT COMMENT '章节内容的JSON字符串',
    `visible` BOOLEAN DEFAULT TRUE COMMENT '是否显示',
    `sort_order` INT DEFAULT 0 COMMENT '排序序号，数字越小越靠前',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX `idx_resume_sections_resume_id` (`resume_id`),
    INDEX `idx_resume_sections_section_code` (`section_code`),
    CONSTRAINT `fk_resume_sections_resume_id` FOREIGN KEY (`resume_id`) REFERENCES `resumes`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='简历章节表';

-- ============================================================
-- 简历版本表
-- ============================================================
CREATE TABLE IF NOT EXISTS `resume_versions` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '版本ID，主键自增',
    `resume_id` BIGINT NOT NULL COMMENT '所属简历ID，关联resumes.id',
    `version_no` INT NOT NULL COMMENT '版本号，从1开始递增',
    `version_name` VARCHAR(255) COMMENT '版本名称，如v1、投递后端版',
    `resume_title` VARCHAR(255) COMMENT '保存版本时的简历标题',
    `resume_template` VARCHAR(100) COMMENT '保存版本时的简历模板',
    `source` VARCHAR(20) DEFAULT 'MANUAL' COMMENT '版本来源：MANUAL / AUTO / AI',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX `idx_resume_versions_resume_id` (`resume_id`),
    INDEX `idx_resume_versions_resume_id_created_at` (`resume_id`, `created_at`),
    UNIQUE KEY `uk_resume_versions_resume_id_version_no` (`resume_id`, `version_no`),
    CONSTRAINT `fk_resume_versions_resume_id` FOREIGN KEY (`resume_id`) REFERENCES `resumes`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='简历版本表';

-- ============================================================
-- 简历版本模块表
-- ============================================================
CREATE TABLE IF NOT EXISTS `resume_version_sections` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '版本模块ID，主键自增',
    `version_id` BIGINT NOT NULL COMMENT '所属版本ID，关联resume_versions.id',
    `section_code` VARCHAR(50) NOT NULL COMMENT '模块代码：BASIC / JOB_INTENT / SUMMARY / EXPERIENCE 等',
    `section_title` VARCHAR(255) COMMENT '模块标题，如工作经验',
    `section_type` VARCHAR(20) NOT NULL COMMENT '模块类型：SYSTEM / CUSTOM',
    `schema_type` VARCHAR(20) NOT NULL COMMENT '内容Schema类型：TEXT / LIST / TAGS',
    `content_json` LONGTEXT COMMENT '模块内容JSON',
    `visible` BOOLEAN DEFAULT TRUE COMMENT '是否显示',
    `sort_order` INT DEFAULT 0 COMMENT '排序序号，数字越小越靠前',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX `idx_resume_version_sections_version_id` (`version_id`),
    INDEX `idx_resume_version_sections_version_id_sort_order` (`version_id`, `sort_order`),
    INDEX `idx_resume_version_sections_section_code` (`section_code`),
    CONSTRAINT `fk_resume_version_sections_version_id` FOREIGN KEY (`version_id`) REFERENCES `resume_versions`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='简历版本模块表';

-- ============================================================
-- AI会话表
-- ============================================================
CREATE TABLE IF NOT EXISTS `ai_session` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '会话ID，主键自增',
    `user_id` BIGINT NOT NULL COMMENT '所属用户ID，关联users.id',
    `resume_id` BIGINT NULL COMMENT '关联简历ID，关联resumes.id，可为空',
    `scene_code` VARCHAR(50) NOT NULL COMMENT '会话场景：OPTIMIZE / MATCH / SUMMARY / CHAT',
    `session_title` VARCHAR(255) COMMENT '会话标题',
    `job_description` LONGTEXT NULL COMMENT '当前会话复用的目标岗位JD',
    `summary` LONGTEXT NULL COMMENT '当前会话长期记忆摘要',
    `parent_session_id` BIGINT NULL COMMENT '派生来源会话ID，用于New Session复制JD但清空历史',
    `status` VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '会话状态：ACTIVE / ARCHIVED / DELETED',
    `last_message_at` DATETIME NULL COMMENT '最后一条消息时间',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX `idx_ai_session_user_id` (`user_id`),
    INDEX `idx_ai_session_resume_id` (`resume_id`),
    INDEX `idx_ai_session_parent_session_id` (`parent_session_id`),
    INDEX `idx_ai_session_user_id_updated_at` (`user_id`, `updated_at`),
    CONSTRAINT `fk_ai_session_user_id` FOREIGN KEY (`user_id`) REFERENCES `users`(`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_ai_session_resume_id` FOREIGN KEY (`resume_id`) REFERENCES `resumes`(`id`) ON DELETE SET NULL,
    CONSTRAINT `fk_ai_session_parent_session_id` FOREIGN KEY (`parent_session_id`) REFERENCES `ai_session`(`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI会话表';

-- ============================================================
-- AI消息表
-- ============================================================
CREATE TABLE IF NOT EXISTS `ai_message` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '消息ID，主键自增',
    `session_id` BIGINT NOT NULL COMMENT '所属会话ID，关联ai_session.id',
    `role` VARCHAR(20) NOT NULL COMMENT '消息角色：USER / ASSISTANT / SYSTEM / TOOL',
    `content` LONGTEXT NOT NULL COMMENT '消息正文',
    `content_type` VARCHAR(20) NOT NULL DEFAULT 'TEXT' COMMENT '内容类型：TEXT / JSON',
    `seq_no` INT NOT NULL COMMENT '会话内顺序号，从1开始递增',
    `status` VARCHAR(20) NOT NULL DEFAULT 'SUCCESS' COMMENT '消息状态：SUCCESS / FAILED',
    `tool_name` VARCHAR(100) NULL COMMENT '工具名称，可为空',
    `extra_json` LONGTEXT NULL COMMENT '扩展信息JSON，可记录模型名、token、原始返回等',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY `uk_ai_message_session_id_seq_no` (`session_id`, `seq_no`),
    INDEX `idx_ai_message_session_id` (`session_id`),
    CONSTRAINT `fk_ai_message_session_id` FOREIGN KEY (`session_id`) REFERENCES `ai_session`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI消息表';

-- ============================================================
-- Agent Run 表
-- ============================================================
CREATE TABLE IF NOT EXISTS `ai_agent_run` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT 'run ID，主键自增',
    `user_id` BIGINT NOT NULL COMMENT '所属用户ID，关联users.id',
    `session_id` BIGINT NOT NULL COMMENT '所属Agent会话ID，关联ai_session.id',
    `resume_id` BIGINT NOT NULL COMMENT '关联简历ID，关联resumes.id',
    `scene_code` VARCHAR(50) NOT NULL COMMENT '场景编码，例如JD_CUSTOMIZE',
    `status` VARCHAR(30) NOT NULL DEFAULT 'PENDING' COMMENT 'run状态：PENDING/QUEUED/RUNNING/WAITING_USER/WAITING_CONFIRM/SUCCESS/FAILED/CANCELLED',
    `current_stage` VARCHAR(50) NULL COMMENT '当前阶段编码，例如BOOTSTRAP/REWRITER',
    `user_input` LONGTEXT NULL COMMENT '用户原始输入',
    `job_description` LONGTEXT NULL COMMENT '目标岗位JD',
    `target_section_id` BIGINT NOT NULL COMMENT '本次run只允许修改的目标模块ID',
    `clarification_payload` LONGTEXT NULL COMMENT '追问payload JSON',
    `approval_payload` LONGTEXT NULL COMMENT '审批payload JSON',
    `result_summary` LONGTEXT NULL COMMENT '本次run结果摘要',
    `error_message` LONGTEXT NULL COMMENT '失败错误信息',
    `client_request_id` VARCHAR(100) NULL COMMENT '客户端幂等请求ID',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `completed_at` DATETIME NULL COMMENT '完成时间',
    INDEX `idx_ai_agent_run_user_id` (`user_id`),
    INDEX `idx_ai_agent_run_session_id` (`session_id`),
    INDEX `idx_ai_agent_run_resume_id` (`resume_id`),
    INDEX `idx_ai_agent_run_status` (`status`),
    INDEX `idx_ai_agent_run_user_id_created_at` (`user_id`, `created_at`),
    UNIQUE KEY `uk_ai_agent_run_user_client_request` (`user_id`, `client_request_id`),
    CONSTRAINT `fk_ai_agent_run_user_id` FOREIGN KEY (`user_id`) REFERENCES `users`(`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_ai_agent_run_session_id` FOREIGN KEY (`session_id`) REFERENCES `ai_session`(`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_ai_agent_run_resume_id` FOREIGN KEY (`resume_id`) REFERENCES `resumes`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Agent Run表';

-- ============================================================
-- Agent Run 事件表
-- ============================================================
CREATE TABLE IF NOT EXISTS `ai_run_event` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '事件ID，主键自增',
    `run_id` BIGINT NOT NULL COMMENT '所属run ID，关联ai_agent_run.id',
    `event_seq` INT NOT NULL COMMENT 'run内递增事件序号',
    `event_type` VARCHAR(80) NOT NULL COMMENT '事件类型，例如stage.changed',
    `stage_code` VARCHAR(50) NULL COMMENT '阶段编码，例如REWRITER',
    `message` VARCHAR(1000) NULL COMMENT '展示消息',
    `payload_json` LONGTEXT NULL COMMENT '事件附加数据JSON',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY `uk_ai_run_event_run_seq` (`run_id`, `event_seq`),
    INDEX `idx_ai_run_event_run_id` (`run_id`),
    INDEX `idx_ai_run_event_run_id_created_at` (`run_id`, `created_at`),
    CONSTRAINT `fk_ai_run_event_run_id` FOREIGN KEY (`run_id`) REFERENCES `ai_agent_run`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Agent Run事件表';

CREATE TABLE ai_interview_round (
                                    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',

                                    user_id BIGINT NOT NULL COMMENT '所属用户ID',
                                    run_id BIGINT NOT NULL COMMENT '本场面试模拟任务的 run ID',
                                    session_id BIGINT NOT NULL COMMENT '所属 Agent 会话ID',
                                    resume_id BIGINT NOT NULL COMMENT '关联简历ID',

                                    round_no INT NOT NULL COMMENT '第几轮问题，从1开始',

                                    question_text TEXT NOT NULL COMMENT '题干',
                                    options_json JSON NOT NULL COMMENT '选项JSON，例如 A/B/C/D',

                                    selected_option VARCHAR(20) NULL COMMENT '用户选择的选项，例如 A',
                                    supplement_text TEXT NULL COMMENT '用户补充说明',

                                    analysis_json JSON NULL COMMENT 'Python 对本轮回答的分析结果',
                                    status VARCHAR(32) NOT NULL COMMENT '状态：WAITING_ANSWER / ANSWERED / FINISHED',

                                    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

                                    UNIQUE KEY uk_run_round (run_id, round_no),
                                    KEY idx_user_session (user_id, session_id),
                                    KEY idx_user_resume (user_id, resume_id),
                                    KEY idx_run_id (run_id)
) COMMENT='AI 面试模拟轮次表';

-- 套餐定义（固定3条数据）
CREATE TABLE plans (
                       id BIGINT AUTO_INCREMENT PRIMARY KEY,
                       name VARCHAR(50) NOT NULL COMMENT '套餐名: start/plus/max',
                       display_name VARCHAR(100) NOT NULL COMMENT '显示名',
                       price INT NOT NULL COMMENT '价格（分）',
                       daily_quota INT NOT NULL COMMENT '每日面试次数',
                       duration_days INT NOT NULL DEFAULT 30 COMMENT '有效天数',
                       created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                       updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 用户订单
CREATE TABLE orders (
                        id BIGINT AUTO_INCREMENT PRIMARY KEY,
                        order_no VARCHAR(64) NOT NULL UNIQUE COMMENT '订单号',
                        user_id BIGINT NOT NULL,
                        plan_id BIGINT NOT NULL,
                        amount INT NOT NULL COMMENT '支付金额（分）',
                        status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/PAID/EXPIRED/CANCELLED',
                        pay_time DATETIME COMMENT '支付时间',
                        expire_time DATETIME COMMENT '订单过期时间（未支付自动关闭）',
                        created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                        updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 用户订阅记录
CREATE TABLE user_subscriptions (
                                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                    user_id BIGINT NOT NULL,
                                    plan_id BIGINT NOT NULL,
                                    order_id BIGINT NOT NULL,
                                    start_time DATETIME NOT NULL,
                                    end_time DATETIME NOT NULL,
                                    daily_used INT NOT NULL DEFAULT 0 COMMENT '今日已用次数',
                                    last_reset_date DATE COMMENT '上次重置日期',
                                    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                                    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);