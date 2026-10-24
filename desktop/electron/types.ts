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
}
