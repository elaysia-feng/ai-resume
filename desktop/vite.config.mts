import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';

const here = path.dirname(fileURLToPath(import.meta.url));
const frontendRoot = path.resolve(here, './renderer');

/**
 * 渲染层构建配置。
 * 渲染层源码直接复用 ./renderer，本目录不复制任何 Vue 代码。
 * - 开发模式（vite serve）：root = ./renderer，端口 5173
 * - 生产模式（vite build）：base = './'，产物输出到 ./renderer/dist，
 *   保证用 file:// 加载时静态资源路径是相对路径。
 */
export default defineConfig(({ command }) => ({
  root: frontendRoot,
  base: command === 'serve' ? '/' : './',
  plugins: [vue()],
  // 依赖预构建缓存放在 desktop/ 内，不污染 renderer/
  cacheDir: path.resolve(here, 'node_modules/.vite'),
  server: {
    host: '127.0.0.1',
    port: 5173,
    strictPort: true,
  },
  build: {
    outDir: path.resolve(frontendRoot, 'dist'),
    emptyOutDir: true,
    sourcemap: false,
  },
}));
