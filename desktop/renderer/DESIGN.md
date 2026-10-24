# AI Resume Forge · 桌面端界面设计规范

> 所有界面重设计必须遵守本文件。token 单一来源是
> `src/styles/design-system.css`（`:root` 里的 `--ui-*`）。

## 0. 产品定位（先读这段）

这是一个 **Electron 桌面应用**，不是网站。

- 用户在本地窗口里用它做简历和面试，窗口宽度通常 1280–1920。
- **不要做营销页**：不要 hero 大标题、不要「立即免费试用」CTA、不要滚动叙事、
  不要装饰性大插画、不要大段副标题。
- **要做工作台**：信息密度适中、导航常驻、每个页面第一屏就能开始干活。
- 桌面端可能**完全离线**，不能用 Google Fonts CDN、不能用在线图标库。
  图标一律用内联 SVG 或 Unicode 符号，字体一律用系统字体栈。

## 1. 布局骨架

```
┌──────────┬──────────────────────────────────────┐
│          │  顶栏 56px：页面标题 / 主操作 / 状态   │
│ 侧边栏    ├──────────────────────────────────────┤
│ 232px    │                                      │
│          │  内容区（各自滚动）                    │
│ 图标+文字 │                                      │
│ 分组导航  │                                      │
└──────────┴──────────────────────────────────────┘
```

- 侧边栏宽度 `--ui-sidebar-width: 232px`，收起态 `--ui-sidebar-width-collapsed: 64px`。
- 顶栏高度 `--ui-header-height: 56px`。
- 内容区最大宽度 `--ui-content-max: 1440px`，居中；表单类页面用 720–880px 更合适。
- 侧边栏、顶栏固定，内容区独立滚动（`overflow-y: auto` + `.ui-scroll`）。
- 页面必须能撑满窗口，**不要出现整页横向滚动条**。

### 路由对应的导航分组

| 分组 | 路由 | 说明 |
|---|---|---|
| 工作台 | `/dashboard`（Home） | 简历列表、快捷入口 |
| 创作 | `/dashboard/templates` `/editor/:id` `/resume/create` | 模板市场、简历编辑 |
| 成长 | `/dashboard/profile` `/dashboard/interview` `/dashboard/history` | 档案、模拟面试、历史 |
| 对话 | `/chat` | AI 对话 |
| 设置 | `/settings` | 模型与后端配置 |

`src/App.vue` 是**应用壳**，由「壳 + 认证」那组负责；其余页面按上表分组，
壳里已有对应导航项，页面不要再自己画全局导航。

## 2. 颜色与 token

只用 `--ui-*`，**禁止在组件里写死十六进制颜色**（品牌色渐变、插画除外）。

| 用途 | 变量 |
|---|---|
| 应用底色 / 画布 | `--ui-bg-app` `--ui-bg-canvas` |
| 卡片 / 面板 | `--ui-bg-surface` `--ui-bg-raised` |
| 悬停 / 选中 | `--ui-bg-hover` `--ui-bg-active` `--ui-bg-selected` |
| 主文字 / 次文字 / 弱化 | `--ui-fg-primary` `--ui-fg-secondary` `--ui-fg-muted` |
| 强调色 | `--ui-accent` `--ui-accent-hover` `--ui-accent-soft` `--ui-accent-fg` |
| 描边 | `--ui-border` `--ui-border-strong` `--ui-border-subtle` |
| 状态色 | `--ui-success` `--ui-warning` `--ui-danger` `--ui-info`（各带 `-soft` / `-border`） |

品牌色阶 `--ui-brand-50..900`，中性色阶 `--ui-neutral-0..950`。

**暗色模式**已内置：跟随系统 `prefers-color-scheme`，也可
`document.documentElement.dataset.theme = 'dark'` 强制。写样式时不要假设亮色。

## 3. 字体与排版

- `--ui-font-sans`：正文与界面（Inter → 系统 UI 字体 → 微软雅黑）
- `--ui-font-display`：仅用于简历模板里的标题装饰（Playfair/宋体）
- `--ui-font-mono`：路径、代码、版本号

字号用 `--ui-text-xs .. --ui-text-4xl`（12 / 13 / 14 / 16 / 18 / 20 / 24 / 30 / 36px）。

中文排版：
- 不要 `text-transform: uppercase`（中文无意义）
- 正文行高 `--ui-line-normal`（1.5），标题 `--ui-line-tight`（1.25）
- 中英混排不要手动加空格，用 `text-wrap: pretty` / `word-break: break-word`

## 4. 复用现成组件类

`design-system.css` 已提供，**优先用它们，别再各写各的按钮和输入框**：

- `.ui-card` 面板/卡片
- `.ui-btn` + `--primary` / `--outline` / `--ghost` / `--danger` / `--sm` / `--lg` / `--block`
- `.ui-input` 输入框/文本域/下拉
- `.ui-label` 表单标签
- `.ui-badge` + `--accent` / `--success` / `--warning` / `--danger`
- `.ui-empty` + `__title` `__hint` 空状态
- `.ui-divider` 分隔线
- `.ui-scroll` 细滚动条容器

## 5. 间距 / 圆角 / 阴影

- 间距用 `--ui-space-1..12`（4 / 8 / 12 / 16 / 20 / 24 / 28 / 32 / 36 / 40 / 44 / 48px）
- 页面内边距：常规 `--ui-space-8`，密集表单 `--ui-space-6`
- 圆角：控件 `--ui-radius-md`，卡片 `--ui-radius-lg`，徽标 `--ui-radius-full`
- 阴影只用 `--ui-shadow-xs/sm/md/lg`，不要自定义 rgba 阴影
- 动效用 `--ui-duration-fast/normal/slow` + `--ui-ease-out/in-out`

## 6. 交互

- 所有可点元素都要有 hover / focus-visible 态；`:focus-visible` 用 `--ui-ring`
- 键盘可达：Tab 顺序合理，`Enter`/`Space` 能触发，弹层能 `Esc` 关闭
- 危险操作要有二次确认
- 加载态用骨架屏或 `.loading-spinner`，不要用整页遮罩
- 空列表用 `.ui-empty`，给出下一步动作按钮
- 错误信息要说明**发生了什么 + 怎么办**，不要只抛错误码

## 7. 中文文案

- 界面全部中文，术语跟项目现有文案保持一致（简历、模板市场、模拟面试…）
- 按钮用动词短语：「生成简历」「保存」「导出 PDF」
- 不要出现 lorem ipsum、英文占位符、"TODO"

## 8. 禁止事项

1. 不新增 npm 依赖（没有 UI 库，不要自己装）
2. 不改 `src/styles/design-system.css`、`src/styles/main.css`、
   `src/styles/green-theme.css`、`src/main.js`、`src/router/index.js`、
   `src/store/**`、`src/api/**`、`src/utils/**`（除非任务书明确授权）
3. 不定义新的 `:root` 变量；不要写 `#22c55e` 这类硬编码颜色
4. 不改 `src/components/resume/**` 里不属于你负责的文件（避免并行冲突）
5. 不重命名 Vue 组件名、props、emit 事件、store 的 action/getter
6. 不动 `desktop/electron/**`（主进程）和 `backend/**`（后端）

## 9. 验收

1. **构建验证**（必须做）。注意配置有两套，别用错：
   - `desktop/vite.config.mts` —— **生产构建用的**（root = renderer，base = './'，
     产物落 `desktop/renderer/dist`），桌面端打包走它
   - `desktop/renderer/vite.config.js` —— 简化版，没有 base = './'，
     只能用来做语法/编译检查

   正确命令（**用独立输出目录**，避免多个 agent 抢同一个 dist）：
   ```
   cd F:\myInterestingProgram\AI-Resume-Forge\desktop
   npx vite build --config vite.config.mts --outDir .tmp-verify-<你的代号>
   ```
   产物会落在 `desktop/renderer/.tmp-verify-<代号>`，验证完删掉。
   （只做编译检查也可以：`cd desktop/renderer && npx vite build --outDir .tmp-verify-<代号>`）

2. 不引入 console 报错；不改接口调用方式（`src/api/**` 的函数签名不变）
3. 页面在 1280×800 和 1920×1080 下都不出现横向滚动条、不出现元素重叠
4. 中文文本不溢出、不截断成半个字
5. 深浅色下都要可读（正文对比度 ≥ 4.5:1）
6. **页面自带内边距不要和壳层内边距叠加**：壳层（App.vue）已经给了内容区
   padding，页面内部**不要再加外层 padding**，用内部区块的 `--ui-space-*` 排版即可
