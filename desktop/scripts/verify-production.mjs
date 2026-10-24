/**
 * 桌面端生产模式（file://）验证：
 * 1. 渲染层能加载起来（router-view 不是空白）
 * 2. 走的是 hash 路由（location.hash 有值，pathname 是磁盘路径）
 * 3. 注入真实 JWT 后能进入 #/chat 并渲染出 chat-first 界面
 * 4. window.desktop API 齐全，后端探活在线
 */
import { spawn } from 'node:child_process';
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import electronPath from 'electron';

const here = path.dirname(fileURLToPath(import.meta.url));
const root = path.resolve(here, '..');
const cdpPort = 9333;
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
      /* 等 CDP 起来 */
    }
    await sleep(300);
  }
  throw new Error('CDP 调试端口未就绪');
}

async function evaluate(client, expression, awaitPromise = true) {
  const result = await client.send('Runtime.evaluate', { expression, awaitPromise, returnByValue: true });
  if (result.exceptionDetails) {
    throw new Error(`表达式执行失败：${result.exceptionDetails.exception?.description ?? result.exceptionDetails.text}`);
  }
  return result.result.value;
}

const checks = [];
function record(name, ok, detail = '') {
  checks.push({ name, ok, detail });
  console.log(`${ok ? '[PASS]' : '[FAIL]'} ${name}${detail ? ' | ' + detail : ''}`);
}

const child = spawn(electronPath, ['.', `--remote-debugging-port=${cdpPort}`], {
  cwd: root,
  env: { ...process.env, AI_RESUME_DEV_SERVER_URL: '', AI_RESUME_BACKEND_URL: 'http://localhost:8080' },
  stdio: ['ignore', 'pipe', 'pipe'],
});
child.stdout.on('data', (c) => process.stdout.write(`[electron] ${c}`));
child.stderr.on('data', (c) => process.stderr.write(`[electron] ${c}`));

try {
  const target = await waitForPageTarget();
  const client = new CdpClient(target.webSocketDebuggerUrl);
  await client.connect();
  await client.send('Page.enable');
  await client.send('Runtime.enable');
  await sleep(3000);

  const href = await evaluate(client, 'location.href');
  const pathname = await evaluate(client, 'location.pathname');
  record('渲染层用 file:// 加载', href.startsWith('file://'), href.slice(0, 90));

  const appText = await evaluate(client, 'document.getElementById("app") ? document.getElementById("app").innerText.trim() : ""');
  record('router-view 不是空白', appText.length > 0, `首屏文字长度=${appText.length}`);

  const desktopKeys = await evaluate(client, 'Object.keys(window.desktop || {})');
  record('window.desktop API 齐全', Array.isArray(desktopKeys) && desktopKeys.length >= 8, JSON.stringify(desktopKeys));

  const status = await evaluate(client, 'window.desktop.getBackendStatus()');
  record('后端探活在线', Boolean(status && status.online), JSON.stringify(status));

  // 注入真实登录态，再打开 #/chat
  const token = process.env.AI_RESUME_JWT;
  record('拿到登录 token', Boolean(token), token ? `len=${token.length}` : '缺失');
  if (token) {
    await evaluate(client, `localStorage.setItem('jwt_token', ${JSON.stringify(token)}); localStorage.setItem('resume_forge_user', JSON.stringify({userId:1,username:'e2e',email:'e2e@example.com',avatarUrl:''})); true`);
    await evaluate(client, "location.hash = '#/chat'; true");
    await sleep(1500);
    // 登录态是 store 初始化时读 localStorage 的，注入后必须整页重载才会生效
    await evaluate(client, 'location.reload(); true');
    await sleep(5000);
    const hash = await evaluate(client, 'location.hash');
    record('hash 路由生效', hash === '#/chat', `location.hash=${hash} pathname=${pathname}`);
    const chatText = await evaluate(client, "document.body.innerText");
    const hasComposer = chatText.includes('发送') || chatText.includes('Enter');
    const hasSidebar = chatText.includes('新建会话') || chatText.includes('会话');
    record('chat-first 界面渲染出来', hasComposer && hasSidebar,
      `含输入框=${hasComposer} 含会话栏=${hasSidebar} 正文前 200 字=${chatText.slice(0, 200).replace(/\s+/g, ' ')}`);
    const shot = await client.send('Page.captureScreenshot', { format: 'png' });
    const out = path.join(os.tmpdir(), 'desktop-production-chat.png');
    fs.writeFileSync(out, Buffer.from(shot.data, 'base64'));
    console.log('截图:', out);
  }
} catch (error) {
  record('脚本执行', false, error instanceof Error ? error.message : String(error));
} finally {
  child.kill();
  await sleep(500);
  const failed = checks.filter((c) => !c.ok);
  console.log(`\n==== 桌面端生产模式：${checks.length - failed.length}/${checks.length} 通过 ====`);
  process.exit(failed.length ? 1 : 0);
}
