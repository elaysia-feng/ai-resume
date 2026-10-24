/**
 * 打包前检查：确认两个后端运行时产物已就位。
 *
 * electron-builder 的 extraResources 指向仓库里的构建产物：
 *   backend/java/build/jpackage/ai-resume-backend   （jpackage app-image）
 *   backend/python/dist/desktop/ai-resume-api        （PyInstaller onedir）
 *
 * 缺任何一个，electron-builder 只会报一句含糊的 "file source doesn't exist"。
 * 这里提前拦下来，把该跑什么命令写清楚。
 */
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const here = path.dirname(fileURLToPath(import.meta.url));
const repoRoot = path.resolve(here, '..', '..');

const REQUIRED = [
  {
    label: 'Java 后端（jpackage app-image，自带 JRE）',
    target: path.join(repoRoot, 'backend', 'java', 'build', 'jpackage', 'ai-resume-backend', 'ai-resume-backend.exe'),
    build: [
      'cd backend/java',
      'mvn -B clean package',
      'New-Item -ItemType Directory -Force -Path build/stage',
      'Copy-Item target/ai-resume-forge-backend.jar -Destination build/stage',
      'jpackage --type app-image --name ai-resume-backend --input build/stage --main-jar ai-resume-forge-backend.jar --dest build/jpackage --app-version 0.1.0',
    ],
  },
  {
    label: 'Python AI 运行时（PyInstaller onedir）',
    target: path.join(repoRoot, 'backend', 'python', 'dist', 'desktop', 'ai-resume-api', 'ai-resume-api.exe'),
    build: [
      'cd backend/python',
      'uv sync --extra dev --python 3.14',
      'uv run python scripts/build_desktop_bundle.py --clean',
    ],
  },
];

const missing = REQUIRED.filter((item) => !fs.existsSync(item.target));

if (missing.length > 0) {
  console.error('[ensure-backends] 缺少后端运行时产物，无法打包成自包含安装包：\n');
  for (const item of missing) {
    console.error(`  · ${item.label}`);
    console.error(`    期望路径：${item.target}`);
    console.error(`    构建命令：${item.build.join('\n                 ')}\n`);
  }
  console.error('CI 里这两步由 .github/workflows/release.yml 自动完成。');
  process.exit(1);
}

console.log('[ensure-backends] 后端运行时产物就位');
