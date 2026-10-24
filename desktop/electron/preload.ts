import { contextBridge, ipcRenderer } from 'electron';
import { installDesktopShell } from './shell';
import type { BackendStatus, DesktopApi } from './types';

/**
 * 渲染层可用的桌面端能力。
 * 形状与 docs/desktop-contract.md 第 6.2 节保持一致，新增的状态查询属于向后兼容的补充。
 */
const api: DesktopApi = {
  chooseDirectory: () => ipcRenderer.invoke('desktop:choose-directory') as Promise<{ path: string | null }>,
  pickFiles: () => ipcRenderer.invoke('desktop:pick-files') as Promise<{ paths: string[] }>,
  backendBaseUrl: () => ipcRenderer.sendSync('desktop:backend-base-url') as string,
  platform: ipcRenderer.sendSync('desktop:platform') as string,
  version: ipcRenderer.sendSync('desktop:version') as string,
  getBackendStatus: () => ipcRenderer.invoke('desktop:backend-status') as Promise<BackendStatus>,
  refreshBackendStatus: () => ipcRenderer.invoke('desktop:backend-refresh') as Promise<BackendStatus>,
  onBackendStatusChanged: (callback) => {
    const listener = (_event: unknown, status: BackendStatus): void => callback(status);
    ipcRenderer.on('desktop:backend-status-changed', listener);
    return () => {
      ipcRenderer.removeListener('desktop:backend-status-changed', listener);
    };
  },
};

contextBridge.exposeInMainWorld('desktop', api);

// 桌面端外壳：后端连接状态 + 原生选择目录/文件入口 + 后端未启动时的重试按钮
installDesktopShell({
  chooseDirectory: api.chooseDirectory,
  pickFiles: api.pickFiles,
  backendBaseUrl: api.backendBaseUrl,
  refreshBackendStatus: api.refreshBackendStatus,
  onBackendStatusChanged: api.onBackendStatusChanged,
});
