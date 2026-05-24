ALTER TABLE `ai_agent_run`
    ADD COLUMN `target_section_id` BIGINT NULL COMMENT '当前单模块 run 的目标 sectionId' AFTER `job_description`,
    ADD COLUMN `active_flag` TINYINT NULL DEFAULT 1 COMMENT '是否活跃：1=活跃，NULL=已结束' AFTER `error_message`;

UPDATE `ai_agent_run`
SET `active_flag` = NULL
WHERE `status` IN ('SUCCESS', 'FAILED', 'CANCELLED');

UPDATE `ai_agent_run`
SET `target_section_id` = 0
WHERE `scene_code` = 'INTERVIEW'
  AND `target_section_id` IS NULL;

ALTER TABLE `ai_agent_run`
    DROP INDEX `uk_ai_agent_run_user_client_request`,
    ADD UNIQUE KEY `uk_ai_agent_run_user_session_client_request` (`user_id`, `session_id`, `client_request_id`),
    ADD UNIQUE KEY `uk_ai_agent_run_active_target` (`user_id`, `session_id`, `scene_code`, `target_section_id`, `active_flag`);
