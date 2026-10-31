# 桌面端改造接口契约（AI Resume Forge）

> 本文件是**唯一契约来源**。Java 后端、Electron 主进程、Vue 渲染层都按这份实现，不要各自发明字段名。

## 0. 总体架构

```
┌──────────────────────────── Electron 桌面端（TS）────────────────────────────┐
│  Main Process (TS)                                                             │
│   · 窗口管理                                                                    │
│   · 原生目录选择框 (dialog.showOpenDialog)  ← 浏览器和后端都给不了这个能力      │
│   · 启动/探活后端进程                                                            │
│  Renderer (Vue3)  ← 多入口工作台：简历 / AI 对话 / 模拟面试 / 历史       │
└──────────────┬─────────────────────────────────────────────┬───────────────────┘
               │ HTTP + SSE                                  │ IPC
               ▼                                             ▼
┌────────────────────────── Java 后端 (8080) ───────────────────────────┐
│  /api/chat/**    会话鉴权、消息落库、Python SSE 代理                      │
│  /api/files/**   本地磁盘文件（带鉴权下载）                                │
│  业务域          简历 / Agent / 面试 / 个人资料 / 通知                       │
└──────────────┬──────────────────────────────────────────────────────────┘
               │ HTTP（内部）
               ▼
┌────────────────── Python AI 运行时 (8000) ──────────────────┐
│  模型调用、提示词、工作区聊天工具、LangGraph 简历与面试工作流 │
└─────────────────────────────────────────────────────────────┘
```

**分工原则**
- 用户鉴权、会话权限和消息落库 → **Java 后端**；聊天模型调用与工作区只读工具 → **Python AI 运行时**
- 「用户选哪个目录」这种原生 UI 交互 → **Electron 主进程**（`dialog`），选完把路径 POST 给 Java
- 深度 AI 能力（LangGraph 工作流、RAG） → **Python**，由 Java 通过内部接口调用

---

## 1. 本地存储布局

```
{storage.root}/
└── {userId}/
    ├── avatar/           用户头像        {uuid}.png
    └── resume-avatar/    简历头像        {uuid}.png
```

- `storage.root` 配置项（`application.yaml` → `storage.root`，默认 `${user.home}/Documents/AI-Resume-Forge`）
- 数据库里**只存相对路径**，不存绝对路径、不存完整 URL：
  - `users.avatar_url` = `avatar/abc-123.png`
  - 简历头像（在 section JSON 里）= `resume-avatar/def-456.png`
- 存量数据兼容：原值是 `http://` / `https://` 开头的一律当**外链**原样返回，不做迁移

## 2. 文件下载接口（Java）

### `GET /api/files/{kind}/{name}`

- `kind` ∈ `avatar` | `resume-avatar`
- `name` = 文件名（不含路径分隔符）
- 归属校验：目标用户**从 `UserContext` 取**，不接受前端传 userId
- 找不到 → 404；路径越界 → 403
- **鉴权方式**：优先用 `Authorization: Bearer <jwt>`；因为 `<img src>` 带不了请求头，
  文件接口额外允许用 query 参数 `?token=<jwt>`（**只有 `/api/files/**` 放行 query 传 token，其它接口仍然只能用请求头**）
- 前端拼接规则（`desktop/renderer/src/api/request.js` → `resolveApiAssetUrl`）：
  - `avatar/xxx.png`、`resume-avatar/xxx.png` → `{API_BASE_URL}/api/files/{value}?token={jwt}`
  - `http://` / `https://` 开头 → 当外链原样返回（兼容存量 OSS 地址）
  - 其它相对路径 → 仍按原逻辑拼到 API 根下

### `POST /api/auth/avatar`（已存在，响应体不变）
响应 `avatarUrl` 从「完整 URL」改为**相对路径** `avatar/xxx.png`。
前端负责拼成 `/api/files/avatar/xxx.png` 显示。

## 3. Chat API（Java，全部在 `/api/chat` 下）

统一响应包装 `ApiResponse<T>`（`{code,message,data}`）。鉴权走现有 JWT。

### 3.1 会话

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/api/chat/sessions` | body `{title?, resumeId?}` → `{sessionId}`；复用 `ai_session` 表，`sceneCode=CHAT` |
| GET | `/api/chat/sessions` | → `[{id,title,lastMessageAt,messageCount}]`，按最后消息时间倒序 |
| GET | `/api/chat/sessions/{id}/messages` | → `[{id,role,content,contentType,toolName,seqNo,status,createdAt}]`，按 seqNo 升序 |
| DELETE | `/api/chat/sessions/{id}` | 删除会话（连带消息） |

复用说明：
- `ai_session`：`sceneCode='CHAT'`，`sessionTitle`=标题，`status='ACTIVE'`
- `ai_message`：`role` ∈ `USER`/`ASSISTANT`/`SYSTEM`/`TOOL`；`toolName` 记工具名；`contentType` ∈ `TEXT`/`JSON`；
  `extraJson` 记模型名/token/原始返回等

### 3.2 发消息（SSE 流式）

`POST /api/chat/sessions/{id}/messages`
- 请求：`{ "content": "用户输入", "workspacePath": "D:\\项目\\xxx" }`（workspacePath 可选，缺省用当前工作区）
- 响应：`text/event-stream`，事件名见下表

| event | data 结构 | 说明 |
|---|---|---|
| `text_delta` | `{"text":"增量文本"}` | 模型输出增量 |
| `tool_call` | `{"id":"...","name":"read_local_file","input":{...}}` | 模型要调工具 |
| `tool_result` | `{"id":"...","name":"...","ok":true,"summary":"读了多少字节"}` | 工具执行结果（**只回摘要，完整内容不推前端**） |
| `done` | `{"messageId":123,"finishReason":"end_turn"}` | 本轮结束 |
| `error` | `{"message":"..."}` | 出错 |

### 3.3 工作区（用户选的根目录）

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/chat/workspace` | → `{path}` 当前工作区根目录（没设过返回 `{path:null}`） |
| POST | `/api/chat/workspace` | body `{path}` → `{path}`；**必须是绝对路径、必须存在、必须是目录**；存 `ai_session`-外的用户级设置（见 3.4） |

### 3.4 工作区存哪

新增一张表（只有这一张新表）：

```sql
CREATE TABLE IF NOT EXISTS `user_setting` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `setting_key` VARCHAR(64) NOT NULL COMMENT '配置项，如 WORKSPACE_ROOT',
  `setting_value` VARCHAR(1024) NULL COMMENT '配置值',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY `uk_user_key` (`user_id`, `setting_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户级设置';
```

## 4. 工具（Python 侧执行，模型通过 tool calling 触发）

| 工具名 | 入参 | 行为 | 限制 |
|---|---|---|---|
| `read_local_file` | `{path: string, maxBytes?: number}` | 读文本文件内容返回 | 必须在工作区根目录内；默认最多 200KB；只读文本，二进制返回提示 |
| `list_dir` | `{path: string, recursive?: boolean, maxEntries?: number}` | 列目录 | 必须在工作区内；默认非递归、最多 500 条；自动排除 `node_modules`/`target`/`.git`/`.idea` |
| `search_in_files` | `{query: string, path?: string, maxResults?: number}` | 在工作区内按内容/文件名搜索 | 默认最多 100 条命中；跳过二进制和排除目录 |
| `read_workspace_tree` | `{path?: string, depth?: number}` | 列工作区目录树 | 默认深度 2 |

**路径安全（必须全部满足）**：
1. 解析成绝对路径后必须 `startsWith(工作区根目录)`，否则返回错误，**不泄露路径是否存在**
2. 拒绝含 `..` 的路径
3. 拒绝符号链接逃逸（`toRealPath()` 后再校验一次）

## 5. Python ChatModel 与供应商兼容协议

- 所有模型调用、提示词、Agent 编排和聊天工具执行都在 Python；Java 保留用户鉴权、会话和消息落库，并代理 SSE。
- Python 通过 LangChain `init_chat_model` 创建统一 `BaseChatModel`。设置页可选择 OpenAI、Anthropic、DeepSeek、Google GenAI、OpenRouter、Ollama、xAI、Mistral、Groq、Together、Hugging Face、Bedrock 或 LiteLLM 集成。
- 服务预设只填入常见地址和模型 ID，所有字段仍可修改；自建中转站填写它实际实现的协议、Base URL、模型 ID、额外请求头和集成原生参数。额外请求头是否可用取决于所选集成。
- 中转站名称不等于它支持的协议。使用中转站时按其实际请求/响应格式选择适配器；它还需要支持工具调用、流式输出和简历 Agent 使用的结构化输出。只支持普通文本聊天的中转站不能完成这些工作流。
- Python 按集成映射通用 API Key、地址、超时、最大 token 和请求头；`providerOptions` 用于传递该集成自己的参数。Bedrock 可使用 AWS 凭据链或初始化参数，Ollama 可不填 API Key。
- MiniMax 的 Anthropic 兼容 Base URL 为 `https://api.minimaxi.com/anthropic`，常用模型 ID 可按账户实际可用值填写。

## 6. Electron 桌面端

### 6.1 目录结构

```
desktop/
├── package.json          # electron + electron-builder + vite + vue
├── electron/
│   ├── main.ts           # 主进程
│   ├── preload.ts        # 暴露 IPC API 给渲染层
│   └── tsconfig.json
├── src/                  # 渲染层（Vue3）
│   ├── main.ts
│   ├── App.vue
│   ├── views/ChatView.vue
│   ├── components/chat/  # 会话列表 / 消息流 / 输入框
│   └── api/chat.js
└── vite.config.ts
```

### 6.2 preload 暴露的 API（`window.desktop`）

```ts
interface DesktopApi {
  chooseDirectory(): Promise<{ path: string | null }>;   // 原生目录选择框
  pickFiles(): Promise<{ paths: string[] }>;             // 原生选文件
  backendBaseUrl(): string;                              // 默认 http://localhost:8080
  platform: string;                                      // 'win32' | ...
  version: string;
  getBackendStatus(): Promise<BackendStatus>;
  refreshBackendStatus(): Promise<BackendStatus>;
  onBackendStatusChanged(cb: (s: BackendStatus) => void): () => void;

  // 配置与后端进程（设置界面用）
  getAppConfig(): Promise<AppConfig>;
  saveAppConfig(config: AppConfig): Promise<AppConfig>;
  restartBackends(): Promise<void>;
  getBackendProcesses(): Promise<BackendProcessState[]>;
  onBackendProcessChanged(cb: (s: BackendProcessState) => void): () => void;
}
```

### 6.2.1 应用配置（安装目录下的 config/application.yaml）

配置是**应用级**的（一个安装目录一份），**不入库、不进 SQLite**：
- 位置 = **安装目录**（NSIS 装到哪就放哪），不是 `%APPDATA%`
- 存 yml：人可读、可手改、可 diff
- 设置界面读写它，改完点「重启后端」生效

```yaml
# {安装目录}/config/application.yaml
agent:                     # 所有 AI 功能共用，由 Python ChatModel 使用
  provider: deepseek       # 服务预设标识，只用于设置页默认值
  modelProvider: deepseek  # LangChain ChatModel 集成标识
  baseUrl: https://api.deepseek.com
  apiKey: ''
  model: deepseek-flash
  maxTokens: 4096
  extraHeaders: {}         # 中转站特殊请求头，可选
  providerOptions: {}      # 所选集成的原生初始化参数，可选
server:                    # Java 后端监听地址
  host: 127.0.0.1
  port: 8080
python:                    # Python AI 运行时监听地址
  host: 127.0.0.1
  port: 8000
internalToken: change-me   # Java 与 Python 必须一致
```

### 6.2.2 后端进程由主进程拉起

桌面端是**自包含应用**：用户装完就能用，不需要自己开命令行起后端。

| 进程 | 打包形态 | 说明 |
| --- | --- | --- |
| Java 后端 | `resources/backend/ai-resume-backend/ai-resume-backend.exe` | jpackage app-image，**自带 JRE**，用户机器不用装 Java |
| Python 运行时 | `resources/python/ai-resume-api/ai-resume-api.exe` | PyInstaller onedir，**用户机器不用装 Python** |

主进程职责：
1. **启动**：按配置注入环境变量后 spawn；Java 额外传
   `--spring.config.additional-location=file:{安装目录}/config/`，让外部 yml 覆盖 jar 内默认值。
2. **守护**：进程意外退出时按退避间隔（1s→2s→5s→10s→30s）自动重启。
3. **收尾**：桌面端退出时 `kill` 子进程，不留孤儿占端口。
4. **日志**：子进程输出写到 `{安装目录}/logs/{java,python}.log`。

数据目录：`{安装目录}/data/`（SQLite 业务库 + checkpoint + 头像）。
Java 启动时会**自动建目录并执行建表脚本**（`db_schema/schema.sql` 已打进 jar），
所以全新安装第一次启动即可用，不需要用户手动导入 SQL。

### 6.3 选完目录之后

渲染层拿到路径 → `POST /api/chat/workspace { path }` → Java 记为该用户的工作区根目录。
之后模型调文件工具时，只能在这个根目录内活动。

## 7. 当前渲染层多入口工作台（Vue3）

- `POST /api/resumes` 创建简历时，`ApiResponse.data` 返回 `{ resumeId, id }`；`resumeId` 是规范字段，`id` 是兼容旧调用方的别名。前端创建后使用 `resumeId` 跳转到 `/editor/:id`。
- `/dashboard` 是总览页，分别汇总简历、AI 对话、模拟面试和近期任务；每张记录打开对应的原功能，并使用精确的会话或简历 ID。
- `/editor/:id` 与 `/resume/create` 是独立简历编辑入口，支持模块编辑、自动保存、版本快照、AI 建议审阅和导出。
- `/chat` 是独立对话工作区，保留自己的会话列表、流式消息和本地文件工作区；从总览或历史打开记录时必须选择指定的对话 ID。
- `/dashboard/interview` 是独立的面试入口。启动面试必须关联一份简历，岗位 JD 可选；已完成的 run 也要能从会话历史重新打开。
- `/dashboard/history` 汇总 Chat 会话和 Agent/面试任务。Agent 会话管理能力仍通过高级管理区进入。
- `GET /api/agent/sessions` 的会话项提供 `latestRunId` / `latestRunStatus`，使历史列表能显示已结束任务的最新状态。
- `GET /api/agent/sessions/{sessionId}/runs` 返回该会话按更新时间倒序排列的 run 摘要（`runId`、`status`、`currentStage`、`resultSummary`、创建/更新时间和完成时间）；接口会校验会话归属。
- 工作台不强制所有用户按单一路径完成所有功能；共享上下文通过现有简历、会话和任务 API 传递。
- 简历首次创建为空白模块，不预填虚构姓名、学校、经历或业绩数据。
- 应用简历 AI patch 前，必须先保存当前编辑内容，并校验建议中的 `beforeJson`；缺少原始内容的旧任务会被拒绝，用户需要重新生成建议。
