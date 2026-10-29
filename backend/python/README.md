# AI Resume Forge - Python Backend

这是一个“标准 LangGraph 项目 + FastAPI 兼容入口”的 Python 后端。
![img.png](img.png)

目标不是单纯把目录拆多，而是让调试入口、业务入口、领域逻辑和基础设施边界清楚，方便长期维护。

## 双入口

- `langgraph dev`：加载 `langgraph.json`，用于 LangGraph Studio 图形化调试。
- `uv run uvicorn app.main:app --reload`：保留 FastAPI，给 Java 后端调用。

## 目录分层

```text
backend/python/
├── langgraph.json              # LangGraph Studio 官方入口配置
├── pyproject.toml              # Python 项目元数据、依赖、测试和 lint 配置
├── src/
│   ├── agent/                  # 标准 LangGraph 包入口，保持薄封装
│   │   └── graph.py            # 导出 graph，给 langgraph.json 使用
│   └── app/
│       ├── api/                # HTTP 路由与 Java 内部接口
│       ├── core/               # 配置与共享 LLM 构造
│       ├── graphs/             # resume_agent 与 interview 两个图及其节点
│       ├── schemas/            # 公共与 Java/Python 内部 Pydantic 契约
│       ├── services/           # Java 网关、事件、校验和 patch 能力
│       ├── stream/             # SQLite 任务队列
│       ├── workers/            # 简历与面试任务 worker
│       ├── evaluation/         # 质量评分与评测规则
│       └── main.py             # FastAPI 应用入口
└── tests/
    └── unit_tests/             # 单元测试
```

`uv_build` 从 `src/` 安装 `app` 与 LangGraph 入口 `agent`；应用代码统一使用 `app.*` 导入，不把 `src` 当作 Python 包名。`uv sync` 后可从不同工作目录启动应用。

## LangGraph Studio

```powershell
cd backend/python
$projectEnv = (Join-Path (Get-Location).Path '.venv')
$env:UV_PROJECT_ENVIRONMENT = $projectEnv
$env:VIRTUAL_ENV = $projectEnv
$env:UV_PYTHON = (Join-Path $projectEnv 'Scripts/python.exe')
uv sync --extra dev
uv run langgraph dev --no-browser --port 2024
```

Studio 加载的图：

```text
src/agent/graph.py:graph
```

`langgraph dev` 使用 `.env.langgraph`，避免读取本地 `.env` 时受到中文注释或系统编码影响。

Studio 的 `Input` 面板只保留最小运行输入，完整中间状态仍在 `ResumeAgentState` 内部流转。

## FastAPI

```powershell
cd backend/python
$projectEnv = (Join-Path (Get-Location).Path '.venv')
$env:UV_PROJECT_ENVIRONMENT = $projectEnv
$env:VIRTUAL_ENV = $projectEnv
$env:UV_PYTHON = (Join-Path $projectEnv 'Scripts/python.exe')
uv run uvicorn app.main:app --reload --port 8000
```

内部 Agent 接口：

- `POST /internal/graph/runs/stream`
- `POST /internal/graph/runs/{run_id}/continue/stream`
- `POST /internal/graph/runs/{run_id}/cancel`

## Checkpoint

Agent 等待态 checkpoint 存在本地 SQLite 文件里（不再用 Redis）：

- 默认路径：`./data/checkpoints.db`，可用 `CHECKPOINT_DB_PATH` 覆盖。
- 用途：保存 `WAITING_USER` / `WAITING_CONFIRM` 状态，支持 continue 恢复。
- 实现：`langgraph-checkpoint-sqlite` + `src/app/graphs/resume_agent/sqlite_checkpoint.py`。

## 记忆摘要

`ResumeAgentState` 继承 `MessagesState`，并额外维护 `summary`：

- `messages`：保留最近几轮原始消息，方便 LangGraph Studio Chat 和节点 Agent 读取上下文。
- `summary`：当消息数超过阈值时，由 `summarize_conversation` 节点把旧消息压缩成长期摘要。
- 阈值配置：`AGENT_SUMMARY_TRIGGER_MESSAGE_COUNT`，默认 `12`。
- 保留最近消息数：`AGENT_SUMMARY_KEEP_RECENT_MESSAGES`，默认 `6`。

## MCP Server

简历业务域 MCP Server 入口：

```powershell
cd backend/python
.\.venv\Scripts\python.exe -m app.graphs.resume_agent.mcp.resume_domain_mcp
```

当前暴露的工具：

- `load_resume_context`
- `validate_patch`
- `preview_patch`
- `persist_run_events`
- `update_run_status`

这些工具内部仍然复用 `JavaGatewayService`，不会绕过 Java 业务网关。

## 当前图结构

```text
bootstrap
  -> summarize_conversation
  -> supervisor
    -> clarifier -> END
    -> jd_analyst -> gap_analyzer -> rewriter -> reviewer
      -> rewriter
      -> approval_packager -> END
      -> END
```

## 测试

```bash
cd backend/python
pytest
```

单元测试会指向临时目录里的 checkpoint 库，不会污染本地的 `data/checkpoints.db`。
