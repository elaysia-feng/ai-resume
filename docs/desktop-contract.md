# 桌面端改造接口契约（AI Resume Forge）

> 本文件是**唯一契约来源**。Java 后端、Electron 主进程、Vue 渲染层都按这份实现，不要各自发明字段名。

## 0. 总体架构

```
┌──────────────────────────── Electron 桌面端（TS）────────────────────────────┐
│  Main Process (TS)                                                             │
│   · 窗口管理                                                                    │
│   · 原生目录选择框 (dialog.showOpenDialog)  ← 浏览器和后端都给不了这个能力      │
│   · 启动/探活后端进程                                                            │
│  Renderer (Vue3)  ← 现有渲染层搬进来 + 新的 chat-first 界面               │
└──────────────┬─────────────────────────────────────────────┬───────────────────┘
               │ HTTP + SSE                                  │ IPC
               ▼                                             ▼
┌────────────────────────── Java 后端 (8080) ───────────────────────────┐
│  /api/chat/**    对话循环 + 工具调用 + MiniMax M3.1 Flash                │
│  /api/files/**   本地磁盘文件（带鉴权下载）                                │
│  业务域          简历 / 订单 / 支付 / 通知（沿用现有）                       │
│  工具            read_local_file / list_dir / search_in_files /           │
│                  choose_workspace_root                                    │
└──────────────┬──────────────────────────────────────────────────────────┘
               │ HTTP（内部）
               ▼
┌────────────────── Python AI 运行时 (8000) ──────────────────┐
│  保留：LangGraph 简历优化 / 面试模拟工作流（RAG、模型 provider） │
└─────────────────────────────────────────────────────────────┘
```

**分工原则**
- 文件读写、LLM 对话循环、工具执行 → **Java 后端**（它跑在用户本机，权限完整，且无跨进程中转）
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

## 4. 工具（Java 侧执行，模型通过 tool calling 触发）

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

## 5. MiniMax 接入（Java）

- 端点：`https://api.minimaxi.com/anthropic/v1/messages`（Anthropic 兼容）
- 模型名：`MiniMax-M3.1-Flash`（**已实测有效**，不是 `MiniMax-M3`）
- 鉴权头：`x-api-key: <token>` + `authorization: Bearer <token>`，`anthropic-version: 2023-06-01`
- **原生支持 tool calling**（已实测：会返回 `tool_use` block，`stop_reason=tool_use`）
- 配置项（`application.yaml`）：
  ```yaml
  minimax:
    base-url: ${MINIMAX_BASE_URL:https://api.minimaxi.com/anthropic}
    # 密钥属性名必须是 anthropic-api-key，不能叫 api-key：
    # Spring 的 relaxed binding 会把 minimax.api-key 映射到操作系统环境变量 MINIMAX_API_KEY，
    # 而操作系统环境变量优先级高于 .env，会把 .env 里能用的 key 盖掉（本机那个旧 key 只对
    # 原生 chatcompletion 端点有效，对本兼容端点会 401）。
    anthropic-api-key: ${MINIMAX_ANTHROPIC_API_KEY:${MINIMAX_API_KEY:}}
    model: ${MINIMAX_MODEL:MiniMax-M3.1-Flash}
    max-tokens: ${MINIMAX_MAX_TOKENS:4096}
  ```
- **缺 api-key 时不要启动失败**（跟 OSS 一样，延迟到真正调用时报错），但调用时必须给清晰报错

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
}
```

### 6.3 选完目录之后

渲染层拿到路径 → `POST /api/chat/workspace { path }` → Java 记为该用户的工作区根目录。
之后模型调文件工具时，只能在这个根目录内活动。

## 7. 前端 chat-first 界面（Vue3，DeepSeek 风格）

布局：
```
┌────────────┬──────────────────────────────────────┬──────────────┐
│ 会话列表    │  消息流（用户气泡 / 模型气泡 /       │  侧边面板    │
│ + 新建对话  │  工具调用卡片 / 文件引用卡片）        │  （可折叠）  │
│            │                                      │  简历预览    │
│            ├──────────────────────────────────────┤  工作区信息  │
│            │  输入框（多行 + 发送 + 附件按钮）      │  文件树      │
└────────────┴──────────────────────────────────────┴──────────────┘
```

- 消息流用 SSE 增量渲染，`text_delta` 追加文本
- 工具调用渲染成卡片：工具名 + 入参摘要 + 成功/失败
- 附件按钮 → `window.desktop.pickFiles()` → 把路径写进输入框上下文
- 现有编辑器（`ResumeEditorView`）保留，改为从侧边面板或独立路由进入
