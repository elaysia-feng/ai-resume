/**
 * 抓取本地 Electron 窗口截图（仅开发用）。
 * 用法：node scripts/shot.mjs <url> <out.png> [--port 9333]
 */
import fs from 'node:fs';

const args = process.argv.slice(2);
const url = args[0];
const out = args[1];
const portIdx = args.indexOf('--port');
const cdpPort = portIdx >= 0 ? Number(args[portIdx + 1]) : 9333;

const sleep = (ms) => new Promise((r) => setTimeout(r, ms));

class Cdp {
  constructor(ws) {
    this.socket = new WebSocket(ws);
    this.nextId = 0;
    this.pending = new Map();
  }
  async connect() {
    await new Promise((resolve, reject) => {
      this.socket.addEventListener('open', resolve, { once: true });
      this.socket.addEventListener('error', reject, { once: true });
    });
    this.socket.addEventListener('message', (e) => {
      const m = JSON.parse(e.data);
      const h = this.pending.get(m.id);
      if (h) { this.pending.delete(m.id); h(m); }
    });
  }
  send(method, params = {}) {
    const id = ++this.nextId;
    return new Promise((resolve, reject) => {
      this.pending.set(id, (m) => {
        if (m.error) reject(new Error(`${method}: ${JSON.stringify(m.error)}`));
        else resolve(m.result);
      });
      this.socket.send(JSON.stringify({ id, method, params }));
    });
  }
}

async function findTarget() {
  const deadline = Date.now() + 30000;
  while (Date.now() < deadline) {
    try {
      const res = await fetch(`http://127.0.0.1:${cdpPort}/json/list`);
      const list = await res.json();
      const page = list.find((t) => t.type === 'page' && t.webSocketDebuggerUrl);
      if (page) return page;
    } catch { /* 等 */ }
    await sleep(300);
  }
  throw new Error('CDP 未就绪');
}

const target = await findTarget();
const cdp = new Cdp(target.webSocketDebuggerUrl);
await cdp.connect();
await cdp.send('Page.enable');
await cdp.send('Runtime.enable');

// 可选：先导航到指定路由
if (url && url !== '-') {
  const current = await cdp.send('Runtime.evaluate', { expression: 'location.href', returnByValue: true });
  const cur = current.result.value;
  const hash = url.startsWith('#') ? url : `#${url}`;
  if (!cur.includes(hash)) {
    await cdp.send('Runtime.evaluate', { expression: `location.hash = ${JSON.stringify(hash)}` });
    await sleep(1800);
  }
}
await sleep(1200);

const shot = await cdp.send('Page.captureScreenshot', { format: 'png', captureBeyondViewport: false });
fs.writeFileSync(out, Buffer.from(shot.data, 'base64'));

// 顺带把控制台错误捞出来
const errs = await cdp.send('Runtime.evaluate', {
  expression: 'JSON.stringify({ href: location.href, title: document.title })',
  returnByValue: true,
});
console.log(`[shot] ${out} :: ${errs.result.value}`);
process.exit(0);
