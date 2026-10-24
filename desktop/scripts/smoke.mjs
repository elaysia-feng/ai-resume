/**
 * 桌面端冒烟验证脚本（仅开发用，不参与打包）。
 * 启动 Electron -> 通过 CDP 检查 window.desktop 与桌面端外壳 -> 截图 -> 验证原生目录选择框。
 *
 * 用法：
 *   node scripts/smoke.mjs --url http://127.0.0.1:5173 [--hold-dialog 8] [--out <png>]
 */
import { spawn } from 'node:child_process';
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import electronPath from 'electron';

const here = path.dirname(fileURLToPath(import.meta.url));
const root = path.resolve(here, '..');

function parseArgs(argv) {
  const result = {};
  for (let i = 0; i < argv.length; i += 1) {
    const item = argv[i];
    if (!item.startsWith('--')) {
      continue;
    }
    const key = item.slice(2);
    const next = argv[i + 1];
    if (next === undefined || next.startsWith('--')) {
      result[key] = true;
    } else {
      result[key] = next;
      i += 1;
    }
  }
  return result;
}

const args = parseArgs(process.argv.slice(2));
const rendererUrl = typeof args.url === 'string' ? args.url : 'http://127.0.0.1:5173';
const holdDialog = Number(args['hold-dialog'] ?? 0);
const outPath = typeof args.out === 'string' ? args.out : path.join(os.tmpdir(), 'desktop-smoke.png');
const cdpPort = Number(args.port ?? 9222);

const sleep = (ms) => new Promise((resolve) => setTimeout(resolve, ms));

class CdpClient {
  constructor(url) {
    this.socket = new WebSocket(url);
    this.nextId = 0;
    this.pending = new Map();
  }

  async connect() {
    await new Promise((resolve, reject) => {
      this.socket.addEventListener('open', resolve, { once: true });
      this.socket.addEventListener('error', reject, { once: true });
    });
    this.socket.addEventListener('message', (event) => {
      const message = JSON.parse(event.data);
      const handler = this.pending.get(message.id);
      if (handler) {
        this.pending.delete(message.id);
        handler(message);
      }
    });
  }

  send(method, params = {}) {
    const id = ++this.nextId;
    return new Promise((resolve, reject) => {
      this.pending.set(id, (message) => {
        if (message.error) {
          reject(new Error(`${method} 失败：${JSON.stringify(message.error)}`));
        } else {
          resolve(message.result);
        }
      });
      this.socket.send(JSON.stringify({ id, method, params }));
    });
  }
}

async function waitForPageTarget() {
  const deadline = Date.now() + 60000;
  while (Date.now() < deadline) {
    try {
      const response = await fetch(`http://127.0.0.1:${cdpPort}/json/list`);
      const targets = await response.json();
      const page = targets.find((item) => item.type === 'page' && item.webSocketDebuggerUrl);
      if (page) {
        return page;
      }
    } catch {
      // CDP 还没起来，继续等
    }
    await sleep(300);
  }
  throw new Error('CDP 调试端口未就绪');
}

async function evaluate(client, expression, awaitPromise = true) {
  const result = await client.send('Runtime.evaluate', { expression, awaitPromise, returnByValue: true });
  if (result.exceptionDetails) {
    throw new Error(`执行表达式失败：${result.exceptionDetails.exception?.description ?? result.exceptionDetails.text}`);
  }
  return result.result.value;
}

const child = spawn(electronPath, ['.', `--remote-debugging-port=${cdpPort}`], {
  cwd: root,
  env: {
    ...process.env,
    AI_RESUME_DEV_SERVER_URL: rendererUrl.startsWith('http') ? rendererUrl : '',
  },
  stdio: ['ignore', 'pipe', 'pipe'],
});
child.stdout.on('data', (chunk) => process.stdout.write(`[electron] ${chunk}`));
child.stderr.on('data', (chunk) => process.stderr.write(`[electron] ${chunk}`));

const report = { rendererUrl, checks: {}, dialog: {}, screenshot: '' };

try {
  const target = await waitForPageTarget();
  const client = new CdpClient(target.webSocketDebuggerUrl);
  await client.connect();
  await client.send('Page.enable');
  // 等渲染层把外壳挂上（外部字体等慢资源会拖慢首屏）
  await sleep(2500);

  report.checks.title = await evaluate(client, 'document.title');
  report.checks.apiKeys = await evaluate(client, 'Object.keys(window.desktop || {})');
  report.checks.backendBaseUrl = await evaluate(client, 'window.desktop.backendBaseUrl()');
  report.checks.platform = await evaluate(client, 'window.desktop.platform');
  report.checks.version = await evaluate(client, 'window.desktop.version');
  report.checks.backendStatus = await evaluate(client, 'window.desktop.getBackendStatus()');
  report.checks.shellText = await evaluate(
    client,
    'document.getElementById("desktop-shell") ? document.getElementById("desktop-shell").innerText : null',
  );
  report.checks.shellButtons = await evaluate(
    client,
    'Array.from(document.querySelectorAll("#desktop-shell button")).map((b) => b.textContent)',
  );

  const shot = await client.send('Page.captureScreenshot', { format: 'png' });
  fs.writeFileSync(outPath, Buffer.from(shot.data, 'base64'));
  report.screenshot = outPath;

  // 原生目录选择框：调用后主进程会弹出模态对话框，promise 在对话框关闭前不会 resolve
  await evaluate(
    client,
    'window.__dialogDone = false; window.__dialogResult = null;' +
      'window.desktop.chooseDirectory().then((r) => { window.__dialogResult = r; window.__dialogDone = true; });',
    false,
  );
  await sleep(2500);
  report.dialog.stillPending = await evaluate(client, 'window.__dialogDone === false');
  if (holdDialog > 0) {
    await sleep(holdDialog * 1000);
  }
} catch (error) {
  report.error = error instanceof Error ? error.message : String(error);
} finally {
  child.kill();
  await sleep(500);
  console.log(`\n=== SMOKE REPORT ===\n${JSON.stringify(report, null, 2)}`);
  process.exit(report.error ? 1 : 0);
}
