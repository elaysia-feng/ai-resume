import type { BackendStatus } from './types';

/** preload 注入桌面端外壳时需要的主进程能力 */
export interface DesktopShellHost {
  chooseDirectory(): Promise<{ path: string | null }>;
  pickFiles(): Promise<{ paths: string[] }>;
  backendBaseUrl(): string;
  refreshBackendStatus(): Promise<BackendStatus>;
  onBackendStatusChanged(callback: (status: BackendStatus) => void): () => void;
}

const SHELL_ID = 'desktop-shell';

const SHELL_CSS = `
#desktop-shell { all: initial; position: fixed; right: 18px; bottom: 18px; z-index: 2147483000;
  display: flex; flex-direction: column-reverse; align-items: flex-end; gap: 10px;
  font: 13px/1.6 "Microsoft YaHei", "Segoe UI", sans-serif; color: #e2e8f0; }
#desktop-shell * { box-sizing: border-box; }

/* ---- 状态圆点（默认唯一可见的东西）---- */
#desktop-shell .ds-fab { appearance: none; width: 16px; height: 16px; margin: 0; padding: 0;
  border: none; border-radius: 50%; background: #64748b; cursor: pointer;
  box-shadow: 0 0 0 3px rgba(100, 116, 139, 0.18);
  transition: transform 140ms ease, box-shadow 140ms ease, background-color 140ms ease; }
#desktop-shell .ds-fab:hover { transform: scale(1.18); }
#desktop-shell .ds-fab:focus-visible { outline: 2px solid #86efac; outline-offset: 3px; }
#desktop-shell .ds-fab.is-online { background: #22c55e; box-shadow: 0 0 0 3px rgba(34, 197, 94, 0.18), 0 0 10px rgba(34, 197, 94, 0.65); }
#desktop-shell .ds-fab.is-offline { background: #ef4444; box-shadow: 0 0 0 3px rgba(239, 68, 68, 0.2); }
#desktop-shell .ds-fab.is-unknown { background: #64748b; }

/* ---- 展开面板（点圆点才出现）---- */
#desktop-shell .ds-panel { display: none; width: 336px; overflow: hidden;
  background: rgba(15, 23, 42, 0.96); border: 1px solid #334155; border-radius: 12px;
  box-shadow: 0 16px 40px rgba(0, 0, 0, 0.45); }
#desktop-shell .ds-panel.is-open { display: block; }

#desktop-shell .ds-head { display: flex; align-items: center; justify-content: space-between;
  padding: 8px 12px; background: rgba(34, 197, 94, 0.14); border-bottom: 1px solid #334155; }
#desktop-shell .ds-title { font-size: 13px; font-weight: 700; color: #86efac; }
#desktop-shell .ds-body { padding: 10px 12px 12px; }
#desktop-shell .ds-row { display: flex; align-items: center; gap: 6px; margin-bottom: 6px; }
#desktop-shell .ds-dot { width: 8px; height: 8px; border-radius: 50%; background: #64748b; flex: none; }
#desktop-shell .ds-dot.is-online { background: #22c55e; box-shadow: 0 0 6px rgba(34, 197, 94, 0.8); }
#desktop-shell .ds-dot.is-offline { background: #ef4444; }
#desktop-shell .ds-status { font-weight: 600; }
#desktop-shell .ds-url { color: #94a3b8; font-size: 12px; word-break: break-all; }
#desktop-shell .ds-actions { display: flex; flex-wrap: wrap; gap: 6px; margin: 8px 0; }
#desktop-shell .ds-btn { appearance: none; border: 1px solid #475569; background: #1e293b; color: #e2e8f0;
  border-radius: 8px; padding: 5px 10px; font: inherit; font-size: 12px; cursor: pointer; }
#desktop-shell .ds-btn:hover { border-color: #22c55e; color: #bbf7d0; }
#desktop-shell .ds-btn.is-primary { background: #16a34a; border-color: #22c55e; color: #fff; }
#desktop-shell .ds-btn.is-hidden { display: none; }
#desktop-shell .ds-path { color: #cbd5e1; font-size: 12px; word-break: break-all;
  border-top: 1px dashed #334155; padding-top: 8px; }
#desktop-shell .ds-hint { color: #94a3b8; font-size: 12px; margin-top: 4px; word-break: break-all; }
#desktop-shell .ds-hint.is-error { color: #fca5a5; }
#desktop-shell .ds-hint.is-ok { color: #86efac; }
`;

function createStyle(): HTMLStyleElement {
  const style = document.createElement('style');
  style.id = `${SHELL_ID}-style`;
  style.textContent = SHELL_CSS;
  return style;
}

function createElement<K extends keyof HTMLElementTagNameMap>(
  tag: K,
  className?: string,
  text?: string,
): HTMLElementTagNameMap[K] {
  const element = document.createElement(tag);
  if (className) {
    element.className = className;
  }
  if (text !== undefined) {
    element.textContent = text;
  }
  return element;
}

/** 把选中的工作区目录同步给后端（契约 6.3），失败只提示不阻断 */
async function syncWorkspace(host: DesktopShellHost, workspacePath: string): Promise<string> {
  try {
    const token = localStorage.getItem('jwt_token');
    const response = await fetch(`${host.backendBaseUrl()}/api/chat/workspace`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
      },
      body: JSON.stringify({ path: workspacePath }),
    });
    return response.ok ? '已同步工作区到后端' : `后端返回 HTTP ${response.status}（接口可能尚未实现）`;
  } catch (error) {
    return `同步失败：${(error as Error).message}`;
  }
}

function mount(host: DesktopShellHost): void {
  if (document.getElementById(SHELL_ID)) {
    return;
  }

  document.head.appendChild(createStyle());

  const root = createElement('div');
  root.id = SHELL_ID;

  // 状态圆点：默认只显示它，绿色代表后端健康
  const fab = createElement('button', 'ds-fab is-unknown');
  fab.id = 'desktop-shell-fab';
  fab.type = 'button';
  fab.title = '后端状态检测中…';
  fab.setAttribute('aria-label', '后端状态');
  fab.setAttribute('aria-expanded', 'false');

  // 展开面板：点圆点才出现
  const panel = createElement('div', 'ds-panel');
  panel.id = 'desktop-shell-panel';

  const head = createElement('div', 'ds-head');
  head.appendChild(createElement('span', 'ds-title', '桌面端外壳'));
  const toggle = createElement('button', 'ds-btn', '收起');
  toggle.id = 'desktop-shell-toggle';
  head.appendChild(toggle);

  const body = createElement('div', 'ds-body');
  const statusRow = createElement('div', 'ds-row');
  const dot = createElement('span', 'ds-dot');
  const statusText = createElement('span', 'ds-status', '后端检测中…');
  statusRow.appendChild(dot);
  statusRow.appendChild(statusText);

  const urlText = createElement('div', 'ds-url', host.backendBaseUrl());

  const actions = createElement('div', 'ds-actions');
  const chooseDirButton = createElement('button', 'ds-btn is-primary', '选择工作区目录');
  chooseDirButton.id = 'desktop-shell-choose-directory';
  const pickFilesButton = createElement('button', 'ds-btn', '选择文件');
  pickFilesButton.id = 'desktop-shell-pick-files';
  const retryButton = createElement('button', 'ds-btn is-hidden', '重试连接');
  retryButton.id = 'desktop-shell-retry';
  actions.appendChild(chooseDirButton);
  actions.appendChild(pickFilesButton);
  actions.appendChild(retryButton);

  const pathText = createElement('div', 'ds-path', '工作区：未选择');
  pathText.id = 'desktop-shell-workspace';
  const hintText = createElement('div', 'ds-hint', '');
  hintText.id = 'desktop-shell-hint';

  body.appendChild(statusRow);
  body.appendChild(urlText);
  body.appendChild(actions);
  body.appendChild(pathText);
  body.appendChild(hintText);

  panel.appendChild(head);
  panel.appendChild(body);

  root.appendChild(panel);
  root.appendChild(fab);
  document.body.appendChild(root);

  const setPanelOpen = (open: boolean): void => {
    panel.className = open ? 'ds-panel is-open' : 'ds-panel';
    fab.setAttribute('aria-expanded', open ? 'true' : 'false');
  };

  const renderStatus = (status: BackendStatus): void => {
    dot.className = status.online ? 'ds-dot is-online' : 'ds-dot is-offline';
    fab.className = status.online ? 'ds-fab is-online' : 'ds-fab is-offline';
    fab.title = status.online
      ? `后端已连接 · ${host.backendBaseUrl()}（点击展开）`
      : `后端未启动 · ${status.message || '请确认后端进程已拉起'}（点击展开）`;
    statusText.textContent = status.online ? '后端已连接' : '后端未启动';
    retryButton.className = status.online ? 'ds-btn is-hidden' : 'ds-btn';
    if (!status.online) {
      hintText.textContent = `后端未启动，点击重试：${status.message}`;
      hintText.className = 'ds-hint is-error';
    } else {
      hintText.textContent = '';
      hintText.className = 'ds-hint';
    }
  };

  chooseDirButton.addEventListener('click', () => {
    void (async () => {
      const result = await host.chooseDirectory();
      if (!result.path) {
        hintText.textContent = '已取消选择';
        hintText.className = 'ds-hint';
        return;
      }
      pathText.textContent = `工作区：${result.path}`;
      hintText.textContent = '正在同步工作区…';
      hintText.className = 'ds-hint';
      const resultText = await syncWorkspace(host, result.path);
      hintText.textContent = resultText;
      hintText.className = resultText.startsWith('已同步') ? 'ds-hint is-ok' : 'ds-hint is-error';
    })();
  });

  pickFilesButton.addEventListener('click', () => {
    void (async () => {
      const result = await host.pickFiles();
      hintText.textContent = result.paths.length === 0 ? '已取消选择' : `已选择 ${result.paths.length} 个文件`;
      hintText.className = 'ds-hint';
    })();
  });

  retryButton.addEventListener('click', () => {
    void (async () => {
      statusText.textContent = '正在重试…';
      renderStatus(await host.refreshBackendStatus());
    })();
  });

  fab.addEventListener('click', () => {
    setPanelOpen(!panel.classList.contains('is-open'));
  });

  toggle.addEventListener('click', () => {
    setPanelOpen(false);
  });

  host.onBackendStatusChanged(renderStatus);
  void host.refreshBackendStatus().then(renderStatus).catch(() => {
    statusText.textContent = '后端状态未知';
    fab.className = 'ds-fab is-unknown';
    fab.title = '后端状态未知（点击展开）';
  });
}

/**
 * 安装桌面端外壳。
 * 不依赖 DOMContentLoaded：外部样式表（如字体 CDN）会阻塞它，
 * 所以优先在 document.body 出现后立即挂载，DOMContentLoaded 只作兜底。
 */
export function installDesktopShell(host: DesktopShellHost): void {
  const tryMount = (): void => {
    if (!document.body || document.getElementById(SHELL_ID)) {
      return;
    }
    mount(host);
  };

  if (document.readyState !== 'loading') {
    tryMount();
    return;
  }
  document.addEventListener('readystatechange', tryMount, { once: true });
  document.addEventListener('DOMContentLoaded', tryMount, { once: true });
}
