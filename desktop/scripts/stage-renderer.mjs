/**
 * 把 renderer/dist 暂存到 desktop/dist-renderer。
 * electron-builder 只收录 desktop 目录内的文件，所以打包前需要复制一份。
 */
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const here = path.dirname(fileURLToPath(import.meta.url));
const root = path.resolve(here, '..');
const source = path.join(root, 'renderer', 'dist');
const target = path.join(root, 'dist-renderer');

if (!fs.existsSync(path.join(source, 'index.html'))) {
  console.error(`[stage-renderer] 渲染层产物不存在：${source}，请先执行 npm run build:renderer`);
  process.exit(1);
}

fs.mkdirSync(target, { recursive: true });
fs.cpSync(source, target, { recursive: true, force: true });
console.log(`[stage-renderer] 已复制 ${source} -> ${target}`);
