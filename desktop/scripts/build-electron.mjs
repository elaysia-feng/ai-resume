/**
 * 用 esbuild 把主进程与 preload 打包成单文件 CJS。
 * 打包而不是 tsc 逐文件编译，是因为 sandbox: true 的 preload 只能 require
 * 内置模块，不能 require 本地文件（shell.ts 必须被打进 preload.js）。
 */
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { build } from 'esbuild';

const here = path.dirname(fileURLToPath(import.meta.url));
const root = path.resolve(here, '..');

await build({
  entryPoints: [path.join(root, 'electron/main.ts'), path.join(root, 'electron/preload.ts')],
  outbase: path.join(root, 'electron'),
  outdir: path.join(root, 'dist/electron'),
  bundle: true,
  platform: 'node',
  target: 'node20',
  format: 'cjs',
  external: ['electron'],
  logLevel: 'info',
});
