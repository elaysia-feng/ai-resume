-- resume-service -> ai_resume_resume
CREATE DATABASE IF NOT EXISTS `ai_resume_resume` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `ai_resume_resume`;

CREATE TABLE IF NOT EXISTS `resumes` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '简历ID，主键自增',
    `user_id` BIGINT NOT NULL COMMENT '所属用户ID',
    `title` VARCHAR(255) COMMENT '简历标题，默认我的简历',
    `template` VARCHAR(100) COMMENT '模板名：classic / modern / creative',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX `idx_resumes_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='简历主表';

CREATE TABLE IF NOT EXISTS `resume_sections` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '章节ID，主键自增',
    `resume_id` BIGINT NOT NULL COMMENT '所属简历ID',
    `section_code` VARCHAR(50) NOT NULL COMMENT '章节代码',
    `section_title` VARCHAR(255) COMMENT '章节标题',
    `section_type` VARCHAR(20) NOT NULL COMMENT '章节类型：SYSTEM / CUSTOM',
    `schema_type` VARCHAR(20) NOT NULL COMMENT '内容Schema类型：TEXT / LIST / TAGS',
    `content_json` LONGTEXT COMMENT '章节内容JSON',
    `visible` BOOLEAN DEFAULT TRUE COMMENT '是否显示',
    `sort_order` INT DEFAULT 0 COMMENT '排序序号',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX `idx_resume_sections_resume_id` (`resume_id`),
    INDEX `idx_resume_sections_section_code` (`section_code`),
    CONSTRAINT `fk_resume_sections_resume_id` FOREIGN KEY (`resume_id`) REFERENCES `resumes`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='简历章节表';

CREATE TABLE IF NOT EXISTS `resume_versions` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '版本ID，主键自增',
    `resume_id` BIGINT NOT NULL COMMENT '所属简历ID',
    `version_no` INT NOT NULL COMMENT '版本号',
    `version_name` VARCHAR(255) COMMENT '版本名称',
    `resume_title` VARCHAR(255) COMMENT '保存版本时的简历标题',
    `resume_template` VARCHAR(100) COMMENT '保存版本时的简历模板',
    `source` VARCHAR(20) DEFAULT 'MANUAL' COMMENT '版本来源：MANUAL / AUTO / AI',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX `idx_resume_versions_resume_id` (`resume_id`),
    INDEX `idx_resume_versions_resume_id_created_at` (`resume_id`, `created_at`),
    UNIQUE KEY `uk_resume_versions_resume_id_version_no` (`resume_id`, `version_no`),
    CONSTRAINT `fk_resume_versions_resume_id` FOREIGN KEY (`resume_id`) REFERENCES `resumes`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='简历版本表';

CREATE TABLE IF NOT EXISTS `resume_version_sections` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '版本模块ID，主键自增',
    `version_id` BIGINT NOT NULL COMMENT '所属版本ID',
    `section_code` VARCHAR(50) NOT NULL COMMENT '模块代码',
    `section_title` VARCHAR(255) COMMENT '模块标题',
    `section_type` VARCHAR(20) NOT NULL COMMENT '模块类型：SYSTEM / CUSTOM',
    `schema_type` VARCHAR(20) NOT NULL COMMENT '内容Schema类型：TEXT / LIST / TAGS',
    `content_json` LONGTEXT COMMENT '模块内容JSON',
    `visible` BOOLEAN DEFAULT TRUE COMMENT '是否显示',
    `sort_order` INT DEFAULT 0 COMMENT '排序序号',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX `idx_resume_version_sections_version_id` (`version_id`),
    INDEX `idx_resume_version_sections_version_id_sort_order` (`version_id`, `sort_order`),
    INDEX `idx_resume_version_sections_section_code` (`section_code`),
    CONSTRAINT `fk_resume_version_sections_version_id` FOREIGN KEY (`version_id`) REFERENCES `resume_versions`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='简历版本模块表';

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
