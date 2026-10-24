import { app, BrowserWindow, dialog, ipcMain, net } from 'electron';
import * as fs from 'node:fs';
import * as path from 'node:path';
import { probeBackend } from './backend';
import type { BackendStatus } from './types';

/** 开发模式的渲染层地址，dev 脚本通过 AI_RESUME_DEV_SERVER_URL 注入 */
const DEV_SERVER_URL = (process.env.AI_RESUME_DEV_SERVER_URL || '').trim();
/** 后端地址，可用 AI_RESUME_BACKEND_URL 覆盖 */
const BACKEND_BASE_URL = (process.env.AI_RESUME_BACKEND_URL || 'http://localhost:8080').replace(/\/+$/, '');
/** 后端探活轮询间隔 */
const BACKEND_PROBE_INTERVAL_MS = 5000;
/** 等待开发服务器就绪的最长时间 */
const DEV_SERVER_WAIT_MS = 30000;

/**
 * 渲染层产物候选相对路径（相对 __dirname 逐级向上查找）：
 * - renderer/dist：开发态与仓库内直接运行
 * - dist-renderer：打包态（electron-builder 只收录 desktop 目录内的文件）
 */
const RENDERER_CANDIDATES = [path.join('renderer', 'dist', 'index.html'), path.join('dist-renderer', 'index.html')];

let mainWindow: BrowserWindow | null = null;
let backendStatus: BackendStatus = {
  online: false,
  baseUrl: BACKEND_BASE_URL,
  checkedAt: 0,
  message: '尚未检测',
};

/** 定位渲染层入口，找不到就抛出可操作的错误信息 */
function resolveRendererEntry(): string {
  const override = (process.env.AI_RESUME_RENDERER_ENTRY || '').trim();
  if (override) {
    if (!fs.existsSync(override)) {
      throw new Error(`AI_RESUME_RENDERER_ENTRY 指向的文件不存在：${override}`);
    }
    return override;
  }

  // 逐级向上查找：同一类候选优先（renderer/dist 优先于打包暂存的 dist-renderer）
  for (const candidate of RENDERER_CANDIDATES) {
    let dir = __dirname;
    for (let depth = 0; depth < 6; depth += 1) {
      const fullPath = path.join(dir, candidate);
      if (fs.existsSync(fullPath)) {
        return fullPath;
      }
      const parent = path.dirname(dir);
      if (parent === dir) {
        break;
      }
      dir = parent;
    }
  }

  throw new Error(
    `找不到渲染层产物（从 ${__dirname} 向上查找 ${RENDERER_CANDIDATES.join('、')}）。` +
      '开发态请先执行 npm run dev:renderer；生产态请先执行 npm run build:renderer。',
  );
}

/** 只允许加载开发服务器或本地文件，避免渲染层被导航到外部站点 */
function isTrustedUrl(url: string): boolean {
  if (DEV_SERVER_URL) {
    return url.startsWith(DEV_SERVER_URL);
  }
  return url.startsWith('file://');
}

/** 探测任意地址是否返回 HTTP 响应，用于等待开发服务器就绪 */
function probeHttp(url: string, timeoutMs: number): Promise<boolean> {
  return new Promise((resolve) => {
    let settled = false;
    const done = (ok: boolean): void => {
      if (!settled) {
        settled = true;
        resolve(ok);
      }
    };

    const request = net.request({ method: 'GET', url });
    const timer = setTimeout(() => {
      request.abort();
      done(false);
    }, timeoutMs);
    request.on('response', (response) => {
      clearTimeout(timer);
      response.on('data', () => {});
      done(true);
    });
    request.on('error', () => {
      clearTimeout(timer);
      done(false);
    });
    request.end();
  });
}

/** 轮询等待开发服务器就绪，避免 Electron 抢先加载导致白屏 */
async function waitForDevServer(url: string, timeoutMs: number): Promise<boolean> {
  const deadline = Date.now() + timeoutMs;
  while (Date.now() < deadline) {
    if (await probeHttp(url, 1500)) {
      return true;
    }
    await new Promise((resolve) => setTimeout(resolve, 500));
  }
  return false;
}

/** 重新探活后端，状态变化时广播给所有窗口 */
async function refreshBackendStatus(): Promise<BackendStatus> {
  const next = await probeBackend(BACKEND_BASE_URL);
  const changed = next.online !== backendStatus.online || next.message !== backendStatus.message;
  backendStatus = next;
  if (changed) {
    for (const win of BrowserWindow.getAllWindows()) {
      if (!win.isDestroyed()) {
        win.webContents.send('desktop:backend-status-changed', next);
      }
    }
  }
  return next;
}

function registerIpcHandlers(): void {
  ipcMain.handle('desktop:choose-directory', async () => {
    const options: Electron.OpenDialogOptions = {
      title: '选择工作区目录',
      buttonLabel: '选择此目录',
      properties: ['openDirectory'],
    };
    const owner = BrowserWindow.getFocusedWindow() ?? mainWindow;
    const result =
      owner && !owner.isDestroyed()
        ? await dialog.showOpenDialog(owner, options)
        : await dialog.showOpenDialog(options);
    const picked = result.canceled ? null : (result.filePaths[0] ?? null);
    return { path: picked };
  });

  ipcMain.handle('desktop:pick-files', async () => {
    const options: Electron.OpenDialogOptions = {
      title: '选择文件',
      buttonLabel: '打开',
      properties: ['openFile', 'multiSelections'],
    };
    const owner = BrowserWindow.getFocusedWindow() ?? mainWindow;
    const result =
      owner && !owner.isDestroyed()
        ? await dialog.showOpenDialog(owner, options)
        : await dialog.showOpenDialog(options);
    return { paths: result.canceled ? [] : result.filePaths };
  });

  ipcMain.handle('desktop:backend-status', () => backendStatus);
  ipcMain.handle('desktop:backend-refresh', () => refreshBackendStatus());

  // preload 需要同步拿到基础信息，这里用 sendSync 返回
  ipcMain.on('desktop:backend-base-url', (event) => {
    event.returnValue = BACKEND_BASE_URL;
  });
  ipcMain.on('desktop:platform', (event) => {
    event.returnValue = process.platform;
  });
  ipcMain.on('desktop:version', (event) => {
    event.returnValue = app.getVersion();
  });
}

function createWindow(): BrowserWindow {
  const win = new BrowserWindow({
    width: 1440,
    height: 900,
    minWidth: 1024,
    minHeight: 640,
    show: false,
    title: 'AI Resume Forge',
    backgroundColor: '#0f172a',
    webPreferences: {
      preload: path.join(__dirname, 'preload.js'),
      contextIsolation: true,
      nodeIntegration: false,
      sandbox: true,
      webSecurity: true,
    },
  });
  mainWindow = win;

  // ready-to-show 可能被外部字体等慢资源拖住，兜底定时显示，避免用户看不到窗口
  const showFallback = setTimeout(() => {
    if (!win.isDestroyed() && !win.isVisible()) {
      win.show();
    }
  }, 2000);
  win.once('ready-to-show', () => {
    clearTimeout(showFallback);
    win.show();
  });
  win.on('closed', () => {
    clearTimeout(showFallback);
    if (mainWindow === win) {
      mainWindow = null;
    }
  });

  // 渲染层不允许开新窗口，也不允许跳转到外部地址
  win.webContents.setWindowOpenHandler(({ url }) => {
    console.warn(`[desktop] 已拦截新窗口请求：${url}`);
    return { action: 'deny' };
  });
  win.webContents.on('will-navigate', (event, url) => {
    if (!isTrustedUrl(url)) {
      event.preventDefault();
      console.warn(`[desktop] 已拦截跳转：${url}`);
    }
  });
  win.webContents.on('will-attach-webview', (event) => {
    event.preventDefault();
  });

  return win;
}

async function loadRenderer(win: BrowserWindow): Promise<void> {
  try {
    if (DEV_SERVER_URL) {
      const ready = await waitForDevServer(DEV_SERVER_URL, DEV_SERVER_WAIT_MS);
      if (!ready) {
        console.warn(`[desktop] 开发服务器 ${DEV_SERVER_URL} 未在超时内就绪，仍尝试加载`);
      }
      await win.loadURL(DEV_SERVER_URL);
      return;
    }
    await win.loadFile(resolveRendererEntry());
  } catch (error) {
    const message = error instanceof Error ? error.message : String(error);
    console.error(`[desktop] 渲染层加载失败：${message}`);
    await win.loadURL(
      `data:text/html;charset=utf-8,${encodeURIComponent(
        `<!doctype html><meta charset="utf-8"><title>AI Resume Forge</title>` +
          `<body style="font:14px/1.8 'Microsoft YaHei',sans-serif;padding:40px;color:#e2e8f0;background:#0f172a">` +
          `<h2>渲染层加载失败</h2><p>${message}</p>` +
          `<p>开发态请确认 Vite 已启动（npm run dev:renderer）；生产态请先执行 npm run build。</p>`,
      )}`,
    );
  }
}

if (!app.requestSingleInstanceLock()) {
  app.quit();
} else {
  app.on('second-instance', () => {
    if (mainWindow) {
      if (mainWindow.isMinimized()) {
        mainWindow.restore();
      }
      mainWindow.focus();
    }
  });

  app.on('window-all-closed', () => {
    if (process.platform !== 'darwin') {
      app.quit();
    }
  });

  app.on('activate', () => {
    if (BrowserWindow.getAllWindows().length === 0) {
      const win = createWindow();
      void loadRenderer(win);
    }
  });

  app.whenReady().then(async () => {
    app.setAppUserModelId('com.elias.resumeforge.desktop');
    registerIpcHandlers();

    const win = createWindow();
    await loadRenderer(win);

    // 探活失败不阻塞界面：渲染层外壳会显示「后端未启动」和重试按钮
    await refreshBackendStatus();
    setInterval(() => {
      void refreshBackendStatus();
    }, BACKEND_PROBE_INTERVAL_MS);
  });
}
