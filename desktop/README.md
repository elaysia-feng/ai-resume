# AI Resume Forge 桌面端（Electron）

TypeScript 主进程 + 复用 `./renderer` 的 Vue3 渲染层。本目录**不复制**任何 Vue 代码，
只负责主进程、preload、打包配置。

契约来源：仓库根目录 `docs/desktop-contract.md`（第 0 节架构、第 6 节 Electron 桌面端）。

## 形态：自包含桌面应用

装完 exe 就能用，**用户机器上不需要装 Java / Python / Node，也不需要任何外部中间件**：

- 主进程拉起并守护 Java 后端和 Python AI 运行时；
- 两个后端的运行时由 CI 打进安装包（jpackage 自带 JRE / PyInstaller 独立 exe）；
- 模型配置与后端地址在「设置」界面里改，保存到安装目录的 `config/application.yaml`。

```
{安装目录}/
├── AI Resume Forge.exe
├── resources/
│   ├── app.asar
│   ├── backend/ai-resume-backend/   # Java 后端（自带 JRE）
│   └── python/ai-resume-api/        # Python AI 运行时
├── config/application.yaml          # 设置界面读写
├── data/                            # SQLite 业务库 / checkpoint / 头像
└── logs/                            # 后端进程日志
```

## 目录结构

```
desktop/
├── package.json            # 依赖、脚本、electron-builder 配置
├── tsconfig.json           # 主进程 TS 配置（tsc --noEmit 类型检查）
├── vite.config.mts          # 渲染层构建配置，root = ./renderer
├── electron/
│   ├── main.ts             # 主进程：窗口、IPC、探活、导航拦截、拉起后端
│   ├── preload.ts          # contextBridge 暴露 window.desktop
│   ├── shell.ts            # 注入渲染层的桌面端外壳（状态 + 原生选择入口）
│   ├── backend.ts          # 后端探活（Electron net.request）
│   ├── backend_process.ts  # 后端进程管理：启动 / 守护 / 重启 / 收尾
│   ├── config.ts           # 读写安装目录的 config/application.yaml
│   └── types.ts            # DesktopApi / BackendStatus / AppConfig 类型
├── scripts/
│   ├── build-electron.mjs  # esbuild 打包主进程与 preload
│   ├── stage-renderer.mjs  # renderer/dist -> desktop/dist-renderer（打包用）
│   ├── ensure-backends.mjs # 打包前检查后端运行时产物是否就位
│   └── smoke.mjs           # CDP 冒烟验证（开发用）
├── dist/                   # 主进程编译产物（已 gitignore）
├── dist-renderer/          # 打包进 asar 的渲染层副本（已 gitignore）
└── release/                # electron-builder 输出（已 gitignore）
```

## 命令

```bash
cd desktop
npm install          # 会下载 Electron 二进制（约 100MB+）

npm run typecheck    # tsc --noEmit
npm run dev          # 并行启动 vite(dev, 5173) + electron
npm run build        # 构建渲染层 + 主进程 + 暂存渲染层
npm start            # 以生产模式启动（加载 renderer/dist/index.html）
npm run package      # electron-builder 打 Windows 安装包
npm run smoke        # CDP 冒烟验证（需先起 dev server）
```

> Electron 二进制下载失败时（国内网络常见）：
> `$env:ELECTRON_MIRROR='https://npmmirror.com/mirrors/electron/'; npm install`
>
> 渲染层的运行期依赖（vue / pinia / axios 等）来自 `./renderer/node_modules`，
> 所以 `desktop/renderer/` 需要先执行过一次 `npm install`。
>
> `npm run package` 之前必须先把两个后端运行时构建出来（脚本里的
> `ensure-backends.mjs` 会检查并提示具体命令）。

环境变量：

| 变量 | 作用 | 默认值 |
| --- | --- | --- |
| `AI_RESUME_DEV_SERVER_URL` | 开发模式渲染层地址；设置后主进程加载该地址 | 空（走 file:// 生产模式） |
| `AI_RESUME_BACKEND_URL` | Java 后端地址 | `http://localhost:8080` |
| `AI_RESUME_RENDERER_ENTRY` | 覆盖渲染层入口文件绝对路径 | 空（自动查找） |

## preload API（window.desktop）

| 成员 | 类型 | 说明 |
| --- | --- | --- |
| `chooseDirectory()` | `Promise<{path: string \| null}>` | 原生目录选择框，取消时 `path` 为 `null` |
| `pickFiles()` | `Promise<{paths: string[]}>` | 原生文件选择框（多选） |
| `backendBaseUrl()` | `string` | 后端地址，受 `AI_RESUME_BACKEND_URL` 控制 |
| `platform` | `string` | 运行时平台，如 `win32` |
| `version` | `string` | 应用版本 |
| `getBackendStatus()` | `Promise<BackendStatus>` | 读取缓存的后端探活结果（补充项） |
| `refreshBackendStatus()` | `Promise<BackendStatus>` | 立即重新探活（补充项） |
| `onBackendStatusChanged(cb)` | `() => void` | 订阅后端状态变化，返回取消订阅函数（补充项） |

后端探活：主进程用 `net.request` 请求 `{backendBaseUrl}/api/auth/me`，收到任意 HTTP 响应即视为后端在线
（401/403 说明服务在跑只是没带 token）；探不通时外壳显示「后端未启动」和「重试连接」按钮，不会白屏。

## 安全基线

- `contextIsolation: true`、`nodeIntegration: false`、`sandbox: true`、`webSecurity: true`
- 渲染层新窗口请求一律 `deny`；`will-navigate` 只放行 dev server 或 `file://`
- 不使用 `shell.openExternal` 打开任意 URL
- preload 只暴露白名单 API，不透传 `ipcRenderer`

## 与前端 / 后端的衔接（主会话已处理）

1. `desktop/renderer/vite.config.js` **不用改**：本目录的 `vite.config.mts` 自己按 `base: './'` 构建渲染层，
   输出到 `desktop/renderer/dist`，再由 `build:stage` 拷进 `desktop/dist-renderer`。
   注意 `npm run build` 会把 `desktop/renderer/dist` 覆盖成桌面端版本。
2. `desktop/renderer/src/router/index.js` 已按 `import.meta.env.BASE_URL` 分支：
   桌面端（`base: './'`）走 `createWebHashHistory()`，网页端仍走 `createWebHistory()`。
   不这样切的话，`file://` 下 `location.pathname` 会变成 `/F:/.../index.html`，
   history 模式匹配不到路由，router-view 空白。
3. Java 侧 `CorsConfig` 已放行 `Origin: null`（`file://` 渲染层发的就是字面量 `null`），
   否则桌面端生产模式下的跨域请求会被浏览器拦掉。

## 生产模式验证

```bash
cd desktop
npm run build                       # 先出产物
AI_RESUME_JWT=<登录后的 JWT> node scripts/verify-production.mjs
```

脚本会起 Electron 生产模式（`file://`），通过 CDP 检查：渲染层加载、router-view 非空白、
hash 路由生效、`window.desktop` 齐全、后端探活在线、注入登录态后 `#/dashboard` 能渲染多入口工作台。

