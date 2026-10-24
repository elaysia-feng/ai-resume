-- ============================================================
-- AI Resume Forge 单体应用 SQLite 建表脚本
--
-- 原先是 MySQL（mysql_schema/monolith_schema.sql），
-- 现在桌面端不再需要装 MySQL 服务，改用单文件 SQLite。
--
-- 说明：
-- 1. 与 MySQL 版的差异：
--    - 自增主键：INTEGER PRIMARY KEY AUTOINCREMENT（MyBatis-Plus 用 JDBC generated keys 取回）
--    - 时间列：DATETIME 声明类型（SQLite 实际存 ISO 字符串，JDBC 侧由 date_class 转 LocalDateTime）
--    - updated_at 不再用 ON UPDATE CURRENT_TIMESTAMP，改由 MyBatis-Plus 的 MetaObjectHandler 填充
--    - 索引/唯一键拆成独立的 CREATE INDEX / 表内 CONSTRAINT
--    - 原 ai_agent_run 的 information_schema 幂等 ALTER 段全部删除：
--      SQLite 没有 information_schema 也没有 PREPARE，且本脚本建表时已是最新结构
-- 2. 表结构与原 MySQL 版逐字段对齐（字段名、类型语义、中文注释保持原样）。
-- 3. 新增两张表，用来顶替原先的 Redis：
--    - agent_task          ：Agent 任务队列（原来用 Redis Stream）
--    - verification_code   ：邮箱验证码（原来用 Redis 缓存）
-- 4. 外键需要 PRAGMA foreign_keys=ON 才会生效，
--    由 application.yaml 的 datasource.hikari.connection-init-sql 打开。
-- ============================================================

PRAGMA foreign_keys = ON;

-- ============================================================
-- 一、用户与鉴权
-- ============================================================

CREATE TABLE IF NOT EXISTS users (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    username VARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL,
    avatar_url VARCHAR(500),
    enabled INTEGER DEFAULT TRUE,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_users_username ON users (username);
CREATE UNIQUE INDEX IF NOT EXISTS uk_users_email ON users (email);

-- ============================================================
-- 二、简历
-- ============================================================

CREATE TABLE IF NOT EXISTS resumes (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id BIGINT NOT NULL,
    title VARCHAR(255),
    template VARCHAR(100),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_resumes_user_id ON resumes (user_id);

CREATE TABLE IF NOT EXISTS resume_sections (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    resume_id BIGINT NOT NULL,
    section_code VARCHAR(50) NOT NULL,
    section_title VARCHAR(255),
    section_type VARCHAR(20) NOT NULL,
    schema_type VARCHAR(20) NOT NULL,
    content_json TEXT,
    visible INTEGER DEFAULT TRUE,
    sort_order INT DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_resume_sections_resume_id FOREIGN KEY (resume_id) REFERENCES resumes (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_resume_sections_resume_id ON resume_sections (resume_id);
CREATE INDEX IF NOT EXISTS idx_resume_sections_section_code ON resume_sections (section_code);

CREATE TABLE IF NOT EXISTS resume_versions (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    resume_id BIGINT NOT NULL,
    version_no INT NOT NULL,
    version_name VARCHAR(255),
    resume_title VARCHAR(255),
    resume_template VARCHAR(100),
    source VARCHAR(20) DEFAULT 'MANUAL',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_resume_versions_resume_id FOREIGN KEY (resume_id) REFERENCES resumes (id) ON DELETE CASCADE,
    CONSTRAINT uk_resume_versions_resume_id_version_no UNIQUE (resume_id, version_no)
);

CREATE INDEX IF NOT EXISTS idx_resume_versions_resume_id ON resume_versions (resume_id);
CREATE INDEX IF NOT EXISTS idx_resume_versions_resume_id_created_at ON resume_versions (resume_id, created_at);

CREATE TABLE IF NOT EXISTS resume_version_sections (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    version_id BIGINT NOT NULL,
    section_code VARCHAR(50) NOT NULL,
    section_title VARCHAR(255),
    section_type VARCHAR(20) NOT NULL,
    schema_type VARCHAR(20) NOT NULL,
    content_json TEXT,
    visible INTEGER DEFAULT TRUE,
    sort_order INT DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_resume_version_sections_version_id FOREIGN KEY (version_id) REFERENCES resume_versions (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_resume_version_sections_version_id ON resume_version_sections (version_id);
CREATE INDEX IF NOT EXISTS idx_resume_version_sections_version_id_sort_order ON resume_version_sections (version_id, sort_order);
CREATE INDEX IF NOT EXISTS idx_resume_version_sections_section_code ON resume_version_sections (section_code);

-- ============================================================
-- 三、AI 会话与 Agent
-- ============================================================

CREATE TABLE IF NOT EXISTS ai_session (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id BIGINT NOT NULL,
    resume_id BIGINT NULL,
    scene_code VARCHAR(50) NOT NULL,
    session_title VARCHAR(255),
    job_description TEXT NULL,
    summary TEXT NULL,
    parent_session_id BIGINT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    last_message_at DATETIME NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_ai_session_parent_session_id FOREIGN KEY (parent_session_id) REFERENCES ai_session (id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_ai_session_user_id ON ai_session (user_id);
CREATE INDEX IF NOT EXISTS idx_ai_session_resume_id ON ai_session (resume_id);
CREATE INDEX IF NOT EXISTS idx_ai_session_parent_session_id ON ai_session (parent_session_id);
CREATE INDEX IF NOT EXISTS idx_ai_session_user_id_updated_at ON ai_session (user_id, updated_at);

CREATE TABLE IF NOT EXISTS ai_message (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    session_id BIGINT NOT NULL,
    role VARCHAR(20) NOT NULL,
    content TEXT NOT NULL,
    content_type VARCHAR(20) NOT NULL DEFAULT 'TEXT',
    seq_no INT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'SUCCESS',
    tool_name VARCHAR(100) NULL,
    extra_json TEXT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_ai_message_session_id FOREIGN KEY (session_id) REFERENCES ai_session (id) ON DELETE CASCADE,
    CONSTRAINT uk_ai_message_session_id_seq_no UNIQUE (session_id, seq_no)
);

CREATE INDEX IF NOT EXISTS idx_ai_message_session_id ON ai_message (session_id);

CREATE TABLE IF NOT EXISTS ai_agent_run (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id BIGINT NOT NULL,
    session_id BIGINT NOT NULL,
    resume_id BIGINT NOT NULL,
    scene_code VARCHAR(50) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    current_stage VARCHAR(50) NULL,
    user_input TEXT NULL,
    job_description TEXT NULL,
    target_section_id BIGINT NULL,
    selected_section_ids_json TEXT NULL,
    clarification_payload TEXT NULL,
    approval_payload TEXT NULL,
    result_summary TEXT NULL,
    error_message TEXT NULL,
    active_flag TINYINT NULL DEFAULT 1,
    client_request_id VARCHAR(100) NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    completed_at DATETIME NULL,
    CONSTRAINT fk_ai_agent_run_session_id FOREIGN KEY (session_id) REFERENCES ai_session (id) ON DELETE CASCADE,
    CONSTRAINT uk_ai_agent_run_user_session_client_request UNIQUE (user_id, session_id, client_request_id),
    CONSTRAINT uk_ai_agent_run_active_target UNIQUE (user_id, session_id, scene_code, target_section_id, active_flag)
);

CREATE INDEX IF NOT EXISTS idx_ai_agent_run_user_id ON ai_agent_run (user_id);
CREATE INDEX IF NOT EXISTS idx_ai_agent_run_session_id ON ai_agent_run (session_id);
CREATE INDEX IF NOT EXISTS idx_ai_agent_run_resume_id ON ai_agent_run (resume_id);
CREATE INDEX IF NOT EXISTS idx_ai_agent_run_status ON ai_agent_run (status);
CREATE INDEX IF NOT EXISTS idx_ai_agent_run_user_id_created_at ON ai_agent_run (user_id, created_at);

-- 已结束的 run 标记为非活跃
UPDATE ai_agent_run
SET active_flag = NULL
WHERE status IN ('SUCCESS', 'FAILED', 'CANCELLED');

-- 面试场景历史 run 补齐目标 sectionId
UPDATE ai_agent_run
SET target_section_id = 0
WHERE scene_code = 'INTERVIEW'
  AND target_section_id IS NULL;

CREATE TABLE IF NOT EXISTS ai_run_event (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    run_id BIGINT NOT NULL,
    event_seq INT NOT NULL,
    event_type VARCHAR(80) NOT NULL,
    stage_code VARCHAR(50) NULL,
    message VARCHAR(1000) NULL,
    payload_json TEXT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_ai_run_event_run_id FOREIGN KEY (run_id) REFERENCES ai_agent_run (id) ON DELETE CASCADE,
    CONSTRAINT uk_ai_run_event_run_seq UNIQUE (run_id, event_seq)
);

CREATE INDEX IF NOT EXISTS idx_ai_run_event_run_id ON ai_run_event (run_id);
CREATE INDEX IF NOT EXISTS idx_ai_run_event_run_id_created_at ON ai_run_event (run_id, created_at);

-- ============================================================
-- 四、面试模拟
-- ============================================================

CREATE TABLE IF NOT EXISTS ai_interview_round (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id BIGINT NOT NULL,
    run_id BIGINT NOT NULL,
    session_id BIGINT NOT NULL,
    resume_id BIGINT NOT NULL,
    round_no INT NOT NULL,
    question_text TEXT NOT NULL,
    options_json TEXT NOT NULL,
    user_answer TEXT NULL,
    analysis_json TEXT NULL,
    status VARCHAR(32) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_run_round UNIQUE (run_id, round_no)
);

CREATE INDEX IF NOT EXISTS idx_session_id ON ai_interview_round (session_id);
CREATE INDEX IF NOT EXISTS idx_resume_id ON ai_interview_round (resume_id);
CREATE INDEX IF NOT EXISTS idx_user_session ON ai_interview_round (user_id, session_id);
CREATE INDEX IF NOT EXISTS idx_user_resume ON ai_interview_round (user_id, resume_id);

-- ============================================================
-- 五、用户级设置（桌面端对话工作区）
-- ============================================================

CREATE TABLE IF NOT EXISTS user_setting (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id BIGINT NOT NULL,
    setting_key VARCHAR(64) NOT NULL,
    setting_value VARCHAR(1024),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_user_setting_key UNIQUE (user_id, setting_key)
);

-- ============================================================
-- 六、邮箱验证码（顶替原来的 Redis 缓存）
-- ============================================================

CREATE TABLE IF NOT EXISTS verification_code (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    email VARCHAR(255) NOT NULL,
    code VARCHAR(16) NOT NULL,
    expires_at DATETIME NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_verification_code_email ON verification_code (email);
CREATE INDEX IF NOT EXISTS idx_verification_code_expires_at ON verification_code (expires_at);

-- ============================================================
-- 七、Agent 任务队列（顶替原来的 Redis Stream）
--
-- 一条任务 = 一个消费组视角下的一行。
-- - publish : 插入一行 PENDING
-- - read    : 用一条 UPDATE ... WHERE id IN (SELECT ...) 原子认领，
--              认领成功的行打上 claim_token，再按 claim_token 读回来
-- - ack     : 认领成功后置 DONE
-- - 死信    : 单独写到 agent.run.dlq 流并把原任务置 DONE
-- - 崩溃回收：CLAIMED 且超过一段时间没完成的，重新置回 PENDING
--
-- 为什么用 claim_token 而不是事务：
-- SQLite 单条 UPDATE 语句本身就是原子的（隐式写事务），
-- 认领和回读分成两条语句也不会被别人抢走 —— 认领后状态已是 CLAIMED，
-- 别的消费者只认领 PENDING；而 claim_token 唯一，回读时只有自己认领的行匹配。
-- ============================================================

CREATE TABLE IF NOT EXISTS agent_stream_group (
    stream VARCHAR(64) NOT NULL,
    group_name VARCHAR(64) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (stream, group_name)
);

CREATE TABLE IF NOT EXISTS agent_task (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    stream VARCHAR(64) NOT NULL,
    group_name VARCHAR(64) NOT NULL,
    payload TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    consumer VARCHAR(64) NULL,
    claim_token VARCHAR(64) NULL,
    dlq_reason TEXT NULL,
    retry_count INT NOT NULL DEFAULT 0,
    claimed_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_agent_task_pending ON agent_task (stream, group_name, status, id);
CREATE INDEX IF NOT EXISTS idx_agent_task_claimed ON agent_task (status, claimed_at);
CREATE INDEX IF NOT EXISTS idx_agent_task_token ON agent_task (claim_token);
