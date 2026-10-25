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
-- SQLite 不提供表/列 COMMENT 元数据；表、索引用途使用本脚本的 SQL 注释说明。
-- 本脚本对应本地测试库的目标结构；表结构调整后需重置本地库，正式发行前再编写版本迁移。
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

-- 按简历读取模块并按 sort_order、id 排序，避免详情查询额外排序。
CREATE INDEX IF NOT EXISTS idx_resume_sections_resume_sort
    ON resume_sections (resume_id, sort_order, id);
-- 按简历校验 section_code 是否已存在，避免跨简历扫描同名模块。
CREATE INDEX IF NOT EXISTS idx_resume_sections_resume_section_code
    ON resume_sections (resume_id, section_code);

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

-- 唯一约束 uk_resume_versions_resume_id_version_no 已覆盖 resume_id 前缀查询。
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

-- 复合索引 idx_resume_version_sections_version_id_sort_order 已覆盖 version_id 前缀查询。
CREATE INDEX IF NOT EXISTS idx_resume_version_sections_version_id_sort_order ON resume_version_sections (version_id, sort_order);
-- 当前版本模块通过 version_id 读取；复合索引已覆盖该等值前缀。

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

CREATE INDEX IF NOT EXISTS idx_ai_session_resume_id ON ai_session (resume_id);
CREATE INDEX IF NOT EXISTS idx_ai_session_parent_session_id ON ai_session (parent_session_id);
-- 按用户读取近期会话，并匹配 last_message_at、updated_at、id 的倒序。
CREATE INDEX IF NOT EXISTS idx_ai_session_user_activity
    ON ai_session (user_id, last_message_at DESC, updated_at DESC, id DESC);
-- 为 Copilot 按用户、简历和场景筛选的列表匹配最近活动排序。
CREATE INDEX IF NOT EXISTS idx_ai_session_user_resume_scene_activity
    ON ai_session (user_id, resume_id, scene_code, last_message_at DESC, updated_at DESC, id DESC);

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

-- 唯一约束 uk_ai_message_session_id_seq_no 已覆盖 session_id 前缀和消息顺序查询。
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
    -- 0 表示本次 run 未绑定简历模块，例如面试 run。
    target_section_id BIGINT NOT NULL DEFAULT 0 CHECK (target_section_id >= 0),
    selected_section_ids_json TEXT NULL,
    clarification_payload TEXT NULL,
    approval_payload TEXT NULL,
    result_summary TEXT NULL,
    error_message TEXT NULL,
    active_flag TINYINT NOT NULL DEFAULT 1 CHECK (active_flag IN (0, 1)),
    client_request_id VARCHAR(100) NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    completed_at DATETIME NULL,
    CONSTRAINT fk_ai_agent_run_session_id FOREIGN KEY (session_id) REFERENCES ai_session (id) ON DELETE CASCADE
);

-- 仅限制同一目标同时存在一个活跃 run；终态记录不参与唯一性检查。
CREATE UNIQUE INDEX IF NOT EXISTS uk_ai_agent_run_active_target
    ON ai_agent_run (user_id, session_id, scene_code, target_section_id)
    WHERE active_flag = 1;
-- 幂等键为空时不进入唯一索引；非空键仍保持原有唯一性。
CREATE UNIQUE INDEX IF NOT EXISTS uk_ai_agent_run_user_session_client_request
    ON ai_agent_run (user_id, session_id, client_request_id)
    WHERE client_request_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_ai_agent_run_session_id ON ai_agent_run (session_id);
-- 当前 run 读取限定 user_id/session_id；未用于筛选的单列索引会增加状态更新写入成本。
-- 按用户、会话、场景和优先级取 run 摘要；历史列表另按创建时间排序。
CREATE INDEX IF NOT EXISTS idx_ai_agent_run_session_summary
    ON ai_agent_run (user_id, session_id, scene_code, active_flag DESC, updated_at DESC, id DESC, status);
CREATE INDEX IF NOT EXISTS idx_ai_agent_run_user_session_created
    ON ai_agent_run (user_id, session_id, created_at DESC, id DESC);

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

-- 唯一约束 uk_ai_run_event_run_seq 已覆盖 run_id、event_seq 查询和顺序读取。

-- ============================================================
-- 四、面试模拟
-- ============================================================

CREATE TABLE IF NOT EXISTS ai_interview_round (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    run_id BIGINT NOT NULL,
    round_no INT NOT NULL,
    question_text TEXT NOT NULL,
    options_json TEXT NOT NULL,
    user_answer TEXT NULL,
    analysis_json TEXT NULL,
    status VARCHAR(32) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_ai_interview_round_run_id FOREIGN KEY (run_id) REFERENCES ai_agent_run (id) ON DELETE CASCADE,
    CONSTRAINT uk_run_round UNIQUE (run_id, round_no),
    CONSTRAINT ck_ai_interview_round_round_no CHECK (round_no > 0)
);

-- 通过 run_id 校验归属；分页、看板过滤 status 并按 round_no 排序，计数可由窄索引覆盖。
CREATE INDEX IF NOT EXISTS idx_ai_interview_round_run_status_round
    ON ai_interview_round (run_id, status, round_no);
-- user/session/resume 可从 run 派生，避免重复存储导致归属不一致。

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
-- 按流、消费组和状态定位过期认领；claimed_at 用于范围过滤。
CREATE INDEX IF NOT EXISTS idx_agent_task_expired_claim
    ON agent_task (stream, group_name, status, claimed_at);
-- 只有认领中的任务有 token；排除大量 NULL 记录，缩小回读索引。
CREATE INDEX IF NOT EXISTS idx_agent_task_token
    ON agent_task (claim_token)
    WHERE claim_token IS NOT NULL;
