-- interview-service -> ai_resume_interview
CREATE DATABASE IF NOT EXISTS `ai_resume_interview` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `ai_resume_interview`;

CREATE TABLE IF NOT EXISTS `ai_interview_round` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `user_id` BIGINT NOT NULL COMMENT '所属用户ID',
    `run_id` BIGINT NOT NULL COMMENT '本场面试模拟任务的 run ID',
    `session_id` BIGINT NOT NULL COMMENT '所属 Agent 会话ID',
    `resume_id` BIGINT NOT NULL COMMENT '关联简历ID',
    `round_no` INT NOT NULL COMMENT '第几轮问题，从1开始',
    `question_text` TEXT NOT NULL COMMENT '题干',
    `options_json` JSON NOT NULL COMMENT '选项JSON，例如 A/B/C/D',
    `user_answer` TEXT NULL COMMENT '用户本轮完整回答JSON',
    `analysis_json` JSON NULL COMMENT 'Python 对本轮回答的分析结果',
    `status` VARCHAR(32) NOT NULL COMMENT '状态：WAITING_ANSWER / ANSWERED / FINISHED',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY `uk_run_round` (`run_id`, `round_no`),
    KEY `idx_session_id` (`session_id`),
    KEY `idx_resume_id` (`resume_id`),
    KEY `idx_user_session` (`user_id`, `session_id`),
    KEY `idx_user_resume` (`user_id`, `resume_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI 面试模拟轮次表';

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
