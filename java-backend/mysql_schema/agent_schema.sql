-- agent-service -> ai_resume_agent
CREATE DATABASE IF NOT EXISTS `ai_resume_agent` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `ai_resume_agent`;

CREATE TABLE IF NOT EXISTS `ai_session` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '会话ID，主键自增',
    `user_id` BIGINT NOT NULL COMMENT '所属用户ID',
    `resume_id` BIGINT NULL COMMENT '关联简历ID',
    `scene_code` VARCHAR(50) NOT NULL COMMENT '会话场景',
    `session_title` VARCHAR(255) COMMENT '会话标题',
    `job_description` LONGTEXT NULL COMMENT '当前会话复用的目标岗位JD',
    `summary` LONGTEXT NULL COMMENT '当前会话长期记忆摘要',
    `parent_session_id` BIGINT NULL COMMENT '派生来源会话ID',
    `status` VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '会话状态：ACTIVE / ARCHIVED / DELETED',
    `last_message_at` DATETIME NULL COMMENT '最后一条消息时间',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX `idx_ai_session_user_id` (`user_id`),
    INDEX `idx_ai_session_resume_id` (`resume_id`),
    INDEX `idx_ai_session_parent_session_id` (`parent_session_id`),
    INDEX `idx_ai_session_user_id_updated_at` (`user_id`, `updated_at`),
    CONSTRAINT `fk_ai_session_parent_session_id` FOREIGN KEY (`parent_session_id`) REFERENCES `ai_session`(`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI会话表';

CREATE TABLE IF NOT EXISTS `ai_message` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '消息ID，主键自增',
    `session_id` BIGINT NOT NULL COMMENT '所属会话ID',
    `role` VARCHAR(20) NOT NULL COMMENT '消息角色：USER / ASSISTANT / SYSTEM / TOOL',
    `content` LONGTEXT NOT NULL COMMENT '消息正文',
    `content_type` VARCHAR(20) NOT NULL DEFAULT 'TEXT' COMMENT '内容类型：TEXT / JSON',
    `seq_no` INT NOT NULL COMMENT '会话内顺序号',
    `status` VARCHAR(20) NOT NULL DEFAULT 'SUCCESS' COMMENT '消息状态：SUCCESS / FAILED',
    `tool_name` VARCHAR(100) NULL COMMENT '工具名称',
    `extra_json` LONGTEXT NULL COMMENT '扩展信息JSON',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY `uk_ai_message_session_id_seq_no` (`session_id`, `seq_no`),
    INDEX `idx_ai_message_session_id` (`session_id`),
    CONSTRAINT `fk_ai_message_session_id` FOREIGN KEY (`session_id`) REFERENCES `ai_session`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI消息表';

CREATE TABLE IF NOT EXISTS `ai_agent_run` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT 'run ID，主键自增',
    `user_id` BIGINT NOT NULL COMMENT '所属用户ID',
    `session_id` BIGINT NOT NULL COMMENT '所属AI会话ID',
    `resume_id` BIGINT NOT NULL COMMENT '关联简历ID',
    `scene_code` VARCHAR(50) NOT NULL COMMENT '场景编码',
    `status` VARCHAR(30) NOT NULL DEFAULT 'PENDING' COMMENT 'run状态',
    `current_stage` VARCHAR(50) NULL COMMENT '当前阶段编码',
    `user_input` LONGTEXT NULL COMMENT '用户原始输入',
    `job_description` LONGTEXT NULL COMMENT '目标岗位JD',
    `selected_section_ids_json` LONGTEXT NULL COMMENT '前端指定的sectionId列表JSON',
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
    CONSTRAINT `fk_ai_agent_run_session_id` FOREIGN KEY (`session_id`) REFERENCES `ai_session`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Agent Run表';

CREATE TABLE IF NOT EXISTS `ai_run_event` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '事件ID，主键自增',
    `run_id` BIGINT NOT NULL COMMENT '所属run ID',
    `event_seq` INT NOT NULL COMMENT 'run内递增事件序号',
    `event_type` VARCHAR(80) NOT NULL COMMENT '事件类型',
    `stage_code` VARCHAR(50) NULL COMMENT '阶段编码',
    `message` VARCHAR(1000) NULL COMMENT '展示消息',
    `payload_json` LONGTEXT NULL COMMENT '事件附加数据JSON',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY `uk_ai_run_event_run_seq` (`run_id`, `event_seq`),
    INDEX `idx_ai_run_event_run_id` (`run_id`),
    INDEX `idx_ai_run_event_run_id_created_at` (`run_id`, `created_at`),
    CONSTRAINT `fk_ai_run_event_run_id` FOREIGN KEY (`run_id`) REFERENCES `ai_agent_run`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Agent Run事件表';

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
