# AI Resume Forge - Architecture

## 系统总览

```
+------------------+        +----------------------------+        +------------------+
|                  |  HTTP |                            |  HTTP  |                  |
|   Frontend       | -----> |   Java Backend (单体)      | -----> |  Python Backend  |
|   (Vue3)         | <----- |   (Spring Boot, 单进程)    | <----- |  (FastAPI+LangGraph)|
|   :5173          |        |   :8080                    |        |  :8000           |
+------------------+        +----------------------------+        +------------------+
                                     |      |      |                        |
                          SQLite   |      |      |  SQLite 任务队列         | SQLite
                        (ai_resume.db)     |      +----------------------+ (checkpoint)
                                     |      |
                              本地磁盘 (头像/附件)
```

## 形态说明

Java 后端是供桌面端使用的**单体本机服务**：简历、Agent、面试和本地 API 会话在一个 Spring Boot 进程中运行
（`com.elias.AiResumeForgeApplication`，端口 8080），业务数据保存在本机 SQLite。

Python 后端仍是独立进程：LangGraph 编排、LLM 调用天然属于 Python 运行时，
与 Java 之间通过 **HTTP 内部接口** + **共享 SQLite 任务队列** 协作，不做语言层面的合并。

拆分演进过程保留在 [微服务拆解方案](docs/MICROSERVICE_ARCHITECTURE.md)，作为架构演进的记录。

## Java 单体内部结构

```
backend/java/src/main/java/com/elias/
├── AiResumeForgeApplication.java   # 唯一启动类，扫描 com.elias
├── common/                         # 跨模块公共层：DTO、异常、MQ、JWT、本地文件存储、UserContext
├── auth/                           # 本地数据记录与 API JWT 令牌
├── resume/                         # 简历、段落、版本、patch 应用
├── agent/                          # Agent 会话与 run 生命周期、SSE、任务派发
├── chat/                           # 通用对话：MiniMax 对话循环、工具调用、工作区
├── interview/                      # 模拟面试会话、轮次、答题
└── security/                       # 本地 API JWT 与 Python 内部服务令牌
```

模块边界靠**包 + 接口**表达，而不是靠进程边界。

## 跨模块调用规则

单体最容易出的问题是循环依赖，所以跨模块调用方向是**固定且单向**的：

| 调用方 | 被调方 | 方式 |
| --- | --- | --- |
| interview | agent | Service（`InternalAgentSupportService` / `AgentSessionService`） |
| agent | resume | Service（`ResumeService` / `ResumePatchService`） |
| agent | interview | Mapper（`AiInterviewRoundMapper`） |

跨模块依赖保持单向；需要回读或更新其他领域数据时，使用明确的 Service 契约或 Mapper 查询，避免绕回调用方形成循环。

## 鉴权

单体只有一个 `SecurityFilterChain`（`com.elias.auth.config.SecurityConfig`）：

1. `InternalAgentTokenFilter` 先校验 `/internal/**` 的 `X-Internal-Service-Token`，拦掉非内部调用；
2. `JwtAuthFilter` 解析 `Authorization: Bearer <token>`，把 userId 写进 `UserContext`（TransmittableThreadLocal），
   并把用户写入 `SecurityContext`；
3. `/api/auth/local-profile` 用于首次创建或复用本机数据记录；其余业务接口使用应用自动管理的 JWT，不提供账号注册、登录或邮箱验证码流程。

原先网关负责的 JWT 全局校验与 CORS，现在由应用内的过滤器与 `CorsConfig` 承担。

## 内部接口契约（Python → Java）

Python 侧通过 `JAVA_INTERNAL_BASE_URL`（默认 `http://localhost:8080`）访问，路径保持不变。
下表是单体里全部 `/internal/**` 路由（15 个方法，其中 2 个带兼容别名）：

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/internal/agent/bootstrap` | 构造简历优化启动上下文 |
| POST | `/internal/agent/interviewBootstrap` | 构造面试启动上下文 |
| POST | `/internal/agent/runs/interview` | 创建面试 run |
| GET | `/internal/agent/runs/{runId}` | 读取 run 详情 |
| POST | `/internal/agent/runs/{runId}/events/batch` | 批量写入 run 事件（别名 `/events/saveBatch`） |
| POST | `/internal/agent/runs/{runId}/status` | 回写 run 状态 |
| GET | `/internal/agent/runs/{runId}/status` | 读取 run 状态 |
| POST | `/internal/agent/runs/{runId}/claim` | 抢占待执行 run（QUEUED → RUNNING） |
| GET | `/internal/resumes/{resumeId}/snapshot` | 读取简历快照 |
| POST | `/internal/resumes/{resumeId}/patch-preview` | patch 预览 |
| POST | `/internal/resumes/{resumeId}/patch-apply` | 应用 patch |
| POST | `/internal/resumes/{resumeId}/versions/ai` | 保存 AI 版本 |
| POST | `/internal/agent/interview/runs/{runId}/rounds` | 写入面试轮次 |
| POST | `/internal/agent/interview/rounds/{roundId}/analysis` | 回写题目分析结果 |
| GET | `/internal/agent/interview/rounds/{roundId}/answers` | 读取题目与用户答案（别名 `/rounds/{roundId}`） |

所有内部接口都要带 `X-Internal-Service-Token` 请求头，值取 `INTERNAL_AGENT_SERVICE_TOKEN`，
必须与 Python 侧一致；缺失或不匹配直接返回 401 `内部服务令牌无效`。

用户隔离由 Java 负责（校验 run/round 归属），run 级并发与恢复保护由 Python 负责。

## 数据模型

单体共用一个 SQLite 文件（默认 `backend/java/data/ai_resume.db`，可用 `DB_FILE` 覆盖），
建表脚本 `backend/java/db_schema/schema.sql`。
表前缀沿用各域语义：`users`、`resumes` / `resume_sections` / `resume_versions`、
`ai_agent_run` / `ai_run_event` / `ai_session` / `ai_message`、`ai_interview_round`、
`user_setting`；`agent_task` / `agent_stream_group` 替代 Redis Stream 作为本机 Agent 任务队列。

## 中间件

桌面端形态：**不需要装任何外部中间件**，全部落在本地文件上。

| 组件 | 用途 |
| --- | --- |
| SQLite（业务库） | 本机业务数据 + Agent 任务队列（`backend/java/data/ai_resume.db`） |
| SQLite（checkpoint） | Python 侧 LangGraph checkpoint（`backend/python/data/checkpoints.db`） |
| 本地磁盘 | 头像与附件（根目录 `storage.root`，用户可自选） |

**唯一的网络依赖是 LLM API**（DeepSeek / MiMo / MiniMax 等），其余全部本地完成。

## 技术栈

| 层 | 技术 |
| --- | --- |
| 前端 | Vite、Vue 3、Pinia |
| Java API | Spring Boot 3.2.5、Spring Security、MyBatis-Plus、Java 17、SQLite |
| Python API | FastAPI、LangGraph、SQLite checkpoint |
| 通信 | 前端 ↔ Java：REST；Java ↔ Python：REST + 共享 SQLite 任务队列 |
