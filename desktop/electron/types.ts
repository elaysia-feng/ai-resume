/**
 * 主进程与 preload 共享的类型定义。
 * preload 通过 contextBridge 暴露的 API 形状见 docs/desktop-contract.md 第 6.2 节。
 */

/** 后端探活结果 */
export interface BackendStatus {
  /** 后端是否可达（收到任意 HTTP 响应即视为可达，401 也说明服务在跑） */
  online: boolean;
  /** 实际探测的后端地址 */
  baseUrl: string;
  /** 最近一次探测时间戳（毫秒） */
  checkedAt: number;
  /** 探测结果说明，用于界面展示 */
  message: string;
}

/** Java 对话模型配置（Anthropic 兼容端点） */
export interface ChatModelConfig {
  baseUrl: string;
  apiKey: string;
  model: string;
  maxTokens: number;
}

/** Python Agent 模型配置（OpenAI 兼容端点） */
export interface AgentModelConfig {
  provider: string;
  baseUrl: string;
  apiKey: string;
  model: string;
}

/** 整体应用配置（对应安装目录下的 config/application.yaml） */
export interface AppConfig {
  chat: ChatModelConfig;
  agent: AgentModelConfig;
  server: { host: string; port: number };
  python: { host: string; port: number };
  internalToken: string;
}

/** 单个后端进程的运行状态 */
export interface BackendProcessState {
  /** 进程标识：java / python */
  id: 'java' | 'python';
  /** 是否正在运行 */
  running: boolean;
  /** 已重启次数 */
  restarts: number;
  /** 最近一次退出码，null 表示还没退出过 */
  lastExitCode: number | null;
  /** 最近一次退出原因 */
  lastExitReason: string;
}

/** 渲染层可访问的桌面端 API */
export interface DesktopApi {
  /** 弹出原生目录选择框，取消时 path 为 null */
  chooseDirectory(): Promise<{ path: string | null }>;
  /** 弹出原生文件选择框（可多选） */
  pickFiles(): Promise<{ paths: string[] }>;
  /** 后端地址，默认 http://localhost:8080，可用 AI_RESUME_BACKEND_URL 覆盖 */
  backendBaseUrl(): string;
  /** 运行平台，如 win32 */
  platform: string;
  /** 应用版本 */
  version: string;
  /** 读取缓存的后端探活结果 */
  getBackendStatus(): Promise<BackendStatus>;
  /** 立即重新探活并返回最新结果 */
  refreshBackendStatus(): Promise<BackendStatus>;
  /** 订阅后端状态变化，返回取消订阅函数 */
  onBackendStatusChanged(callback: (status: BackendStatus) => void): () => void;

  /* ---- 配置与后端进程（桌面端设置界面用）---- */

  /** 读取安装目录下的 config/application.yaml */
  getAppConfig(): Promise<AppConfig>;
  /** 保存配置到 config/application.yaml */
  saveAppConfig(config: AppConfig): Promise<AppConfig>;
  /** 应用配置后重启两个后端进程 */
  restartBackends(): Promise<void>;
  /** 两个后端进程的运行状态 */
  getBackendProcesses(): Promise<BackendProcessState[]>;
  /** 订阅后端进程状态变化，返回取消订阅函数 */
  onBackendProcessChanged(callback: (state: BackendProcessState) => void): () => void;
}
