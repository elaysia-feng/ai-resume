/**
 * 从 @phosphor-icons/core 的 regular 权重 SVG 里抽取路径数据，
 * 生成项目内自用的图标路径表（src/icons/paths.js）。
 *
 * 为什么不用 @phosphor-icons/vue：
 * - 该包有 4500+ 文件，Vite 预构建会明显拖慢启动
 * - 我们只需要一小撮图标，且要统一视觉规格（同一 viewBox / 同一 currentColor 着色）
 *
 * 抽取后图标是纯路径数据，随包体积可忽略，且完全离线可用。
 */
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const here = path.dirname(fileURLToPath(import.meta.url));
const assetsDir = path.resolve(here, '../node_modules/@phosphor-icons/core/assets/regular');
const outFile = path.resolve(here, '../src/icons/paths.js');

/** 项目会用到的图标（kebab-case，对应 phosphor regular 权重文件名） */
const ICONS = [
  // 导航与壳层
  'squares-four', 'layout', 'file-plus', 'user', 'microphone', 'clock-counter-clockwise',
  'chat-circle', 'gear', 'sign-out', 'caret-double-left', 'caret-double-right',
  'house', 'house-line', 'sidebar-simple',
  // 文件与编辑
  'file-text', 'files', 'file-pdf', 'file-doc', 'download-simple', 'upload-simple',
  'trash', 'copy', 'pencil-simple', 'note-pencil', 'note-blank', 'plus', 'minus',
  'check', 'x', 'magnifying-glass', 'funnel', 'funnel-simple', 'dots-three',
  'dots-three-vertical', 'caret-right', 'caret-down', 'caret-up', 'caret-left',
  'arrow-right', 'arrow-left', 'arrow-up', 'arrow-down', 'arrows-clockwise', 'arrows-counter-clockwise',
  'arrow-square-out', 'arrow-u-up-left',
  // 内容与能力
  'sparkle', 'magic-wand', 'robot', 'brain', 'chat-circle-dots', 'chat-centered-dots',
  'lightbulb', 'target', 'medal', 'trophy', 'certificate', 'graduation-cap',
  'briefcase', 'buildings', 'phone', 'envelope', 'envelope-simple', 'link', 'link-simple',
  'calendar', 'calendar-blank', 'clock', 'star', 'heart', 'bookmark-simple', 'list-checks',
  'list', 'tree-structure', 'git-branch', 'code', 'terminal-window', 'seal-check',
  'identification-card', 'users-three', 'user-circle', 'user-plus',
  // 状态与反馈
  'check-circle', 'x-circle', 'warning-circle', 'warning', 'info', 'spinner-gap',
  'circle-notch', 'hourglass-medium', 'paper-plane-tilt', 'thumbs-up',
  // 媒体
  'image', 'images', 'camera', 'video-camera', 'play', 'pause', 'speaker-high',
  'music-notes', 'palette',
  // 设置
  'sliders', 'toggle-left', 'toggle-right', 'eye', 'eye-slash', 'lock', 'key',
  'text-aa', 'moon', 'sun', 'desktop', 'database', 'cloud-arrow-up',
  'hard-drives', 'cpu', 'pulse', 'chart-line', 'chart-bar', 'table',
  'paperclip', 'bell', 'question', 'sign-in', 'translate', 'keyboard',
  'paint-brush', 'shuffle', 'swap', 'columns', 'rows', 'selection',
];

function extract(svg) {
  // regular 权重的结构统一是 <svg ...><path d="..."/></svg>，
  // 少量图标含第二个带 opacity 的 path（如 duotone 才有，这里 regular 一般只有一个）
  const paths = [];
  const re = /<path[^>]*\sd="([^"]+)"/g;
  let m;
  while ((m = re.exec(svg)) !== null) {
    paths.push(m[1]);
  }
  return paths;
}

const entries = [];
const missing = [];
for (const name of ICONS) {
  const file = path.join(assetsDir, `${name}.svg`);
  if (!fs.existsSync(file)) {
    missing.push(name);
    continue;
  }
  const svg = fs.readFileSync(file, 'utf8');
  const paths = extract(svg);
  if (paths.length === 0) {
    missing.push(name);
    continue;
  }
  entries.push([name, paths]);
}

const body = entries
  .map(([name, paths]) => `  '${name}': ${JSON.stringify(paths)},`)
  .join('\n');

const output = `/**
 * 图标路径表（自动生成，勿手改）
 *
 * 数据来源：@phosphor-icons/core 的 regular 权重（MIT License）
 * 生成脚本：desktop/renderer/scripts/extract-icons.mjs
 *
 * 统一规格：viewBox 0 0 256 256，fill=currentColor，随文字颜色走。
 * 渲染入口：src/components/common/AppIcon.vue
 */
export const ICON_PATHS = {
${body}
};

/** 图标名清单 */
export const ICON_NAMES = Object.keys(ICON_PATHS);
`;

fs.mkdirSync(path.dirname(outFile), { recursive: true });
fs.writeFileSync(outFile, output, 'utf8');

console.log(`[extract-icons] 已写入 ${entries.length} 个图标 -> ${outFile}`);
if (missing.length > 0) {
  console.log(`[extract-icons] 未找到 ${missing.length} 个: ${missing.join(', ')}`);
}
