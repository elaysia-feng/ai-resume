# AI Resume Forge - Java Backend（单体）

Spring Boot 单体后端：认证、简历、Agent、面试、订单支付、通知等模块合并在一个进程内，
按包隔离领域边界，一次构建、一次启动。

## 环境要求

- Java 17+（运行可用 JDK 21）
- Maven 3.8+
- **不需要外部数据库**：业务数据、Agent 任务队列、邮箱验证码都落在 SQLite 单文件上
  （默认 `backend/java/data/ai_resume.db`，可用 `DB_FILE` 覆盖）

## 构建与运行

```bash
cd backend/java
mvn package                 # 会跑单元测试
java -jar target/ai-resume-forge-backend.jar
```

服务启动在 `http://localhost:8080`。开发期也可以：

```bash
mvn spring-boot:run
```

> `JWT_SECRET`、`DB_PASSWORD`、`INTERNAL_AGENT_SERVICE_TOKEN` 没有默认值，
> 缺任何一个都会**启动直接失败**并给出提示，不会带着空密钥跑起来。
> 这三个值放在仓库根目录 `.env`（已被 git 忽略）或环境变量里。

## 数据库

单体应用共用一个 SQLite 文件，建表脚本：

```bash
sqlite3 data/ai_resume.db < db_schema/schema.sql
```

文件路径由 `DB_FILE` 指定（默认 `backend/java/data/ai_resume.db`），后端启动时会自动建目录。

当前数据库脚本是本地测试版本的目标结构，`CREATE TABLE IF NOT EXISTS` 不会改造已有表。
调整表结构后需使用空的 SQLite 文件重建本地数据；正式发行前再增加版本化迁移。

## 结构

```
src/main/java/com/elias/
├── AiResumeForgeApplication.java   # 唯一启动类
├── common/                         # 公共层：DTO、异常、MQ、JWT、本地文件存储、UserContext
├── auth/                           # 注册登录、验证码、JWT 签发
├── resume/                         # 简历、段落、版本、patch 应用
├── agent/                          # Agent 会话与 run 生命周期、SSE、任务派发
├── chat/                           # 通用对话：MiniMax 对话循环、工具调用、工作区
├── interview/                      # 模拟面试会话、轮次、答题
└── notification/                   # 邮件验证码（邮箱验证码已改存 SQLite）

src/test/java/com/elias/            # 单元测试：JWT、鉴权过滤器派发、MQ 消费、通知归属
```

跨模块调用规则、鉴权链路和内部接口契约见仓库根目录的 [ARCHITECTURE.md](../ARCHITECTURE.md)。

## 配置

配置集中在 `src/main/resources/application.yaml`，敏感项走环境变量（见仓库根目录 `.env.example`）：

- `DB_FILE`：SQLite 数据文件路径（业务数据 + Agent 任务队列 + 验证码）
- `JWT_SECRET`：JWT 签名密钥
- `MAIL_USERNAME` / `MAIL_PASSWORD`：发信邮箱
- `AGENT_RESUME_RUN_STREAM` / `AGENT_INTERVIEW_RUN_STREAM` / `AGENT_RUN_RESULT_STREAM` / `AGENT_RUN_DLQ_STREAM`：Agent 任务流名（Java 和 Python 两端必须一致）
- `STORAGE_ROOT`：本地磁盘存储根目录（头像等文件，留空用「用户主目录/Documents/AI-Resume-Forge」）
- `MINIMAX_BASE_URL` / `MINIMAX_ANTHROPIC_API_KEY` / `MINIMAX_MODEL`：通用对话用的 MiniMax 模型
  （密钥单独起名：Spring 的 relaxed binding 会把 `minimax.api-key` 映射到操作系统环境变量
  `MINIMAX_API_KEY`，而系统环境变量优先级高于 `.env`，同名会被旧值盖掉）
- `ALIPAY_*`：支付宝
- `PYTHON_AGENT_BASE_URL`：Python AI 运行时地址
- `INTERNAL_AGENT_SERVICE_TOKEN`：Python 调用内部接口时携带的令牌
