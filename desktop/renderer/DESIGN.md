# AI Resume Forge · 桌面端界面设计规范

> 所有界面重设计必须遵守本文件。token 单一来源是
> `src/styles/design-system.css`（`:root` 里的 `--ui-*`）。

## 0. 产品定位（先读这段）

这是一个 **Electron 桌面应用**，不是网站。

- 用户在本地窗口里用它做简历和面试，窗口宽度通常 1280–1920。
- **不要做营销页**：不要 hero 大标题、不要「立即免费试用」CTA、不要滚动叙事、
  不要装饰性大插画、不要大段副标题。
- **要做工作台**：信息密度适中、导航常驻、每个页面第一屏就能开始干活。
- 桌面端的编辑界面必须离线可读，不能用 Google Fonts CDN、不能用在线图标库。
  AI 调用需要访问配置的模型服务；界面字体用系统栈，图标用内置 Phosphor 路径表（见第 4 节）。
- 目标气质：**克制、精确、有厚度**。像 Linear / Arc 这类桌面工具，
  不是 SaaS 落地页。判断标准是「信息是否清晰、控件是否好点」，
  不是「有没有大标题和渐变」。

## 0.5 这一轮的设计升级（2026-10）

本轮采用“石墨与矿物纸”的精密编辑工作台方向：保留松绿品牌识别、暖灰纸面和离线字体；用排版层级、留白与边界建立质感。普通内容面板保持平整，只有浮层和少数主区域使用轻阴影。

页面改造时必须遵守：

1. **图标全部换成 `<AppIcon>`**。禁止再手写 `<svg>` 路径（见第 4 节）。
2. 普通卡片/面板用 `.ui-panel`，不要给所有区域叠阴影和渐变；需要突出层级时用 `.ui-panel--raised`，hover 态只做轻微边界和位移反馈。
3. **页面标题用 `.ui-page-title` + `.ui-page-subtitle`**，
   分区标题用 `.ui-section-head`（左标题右操作）。
   不要自己再写一套 h1/h2 样式。

禁止项：外发光（glow）、霓虹描边、渐变文字、纯黑阴影、超大标题、
纯装饰性的彩色圆点。

## 1. 布局骨架

```
┌──────────┬──────────────────────────────────────┐
│          │  页面自己的标题 / 操作栏               │
│ 侧边栏    ├──────────────────────────────────────┤
│ 232px    │                                      │
│          │  内容区（各自滚动）                    │
│ 图标+文字 │                                      │
│ 分组导航  │                                      │
└──────────┴──────────────────────────────────────┘
```

- 侧边栏宽度 `--ui-sidebar-width: 232px`，收起态 `--ui-sidebar-width-collapsed: 64px`。
- 全局壳层不再额外重复页面顶栏；聊天和简历编辑器使用自己的工具栏，其他页面在内容区显示标题和操作。
- 内容区最大宽度 `--ui-content-max: 1440px`，居中；表单类页面用 720–880px 更合适。
- 侧边栏固定，内容区独立滚动（`overflow-y: auto` + `.ui-scroll`）；页面工具栏留在各自工作区。
- 页面必须能撑满窗口，**不要出现整页横向滚动条**。

### 路由对应的导航分组

| 分组 | 路由 | 说明 |
|---|---|---|
| 工作台 | `/dashboard`（Home） | 独立功能入口、近期活动和简历列表 |
| 简历 | `/editor/:id` `/resume/create` `/dashboard/templates` | 编辑、新建、选择或更换模板 |
| AI 对话 | `/chat` | 独立对话与本地工作区工具 |
| 模拟面试 | `/dashboard/interview` | 独立面试入口、进行中任务和历史结果 |
| 历史与账号 | `/dashboard/history` `/dashboard/profile` | 汇总对话、Agent 任务、面试和个人资料 |
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

**暗色模式**已内置：默认跟随系统，也可通过 `localStorage` 的 `resume_forge_theme`
或 `document.documentElement.dataset.theme = 'dark'` 固定主题。写样式时不要假设亮色。

## 3. 字体与排版

- `--ui-font-sans`：正文与界面（Segoe UI Variable → 苹方 → 雅黑，全离线）
- `--ui-font-display`：界面标题（与 sans 同族，只靠字重和字距拉开层级）
- `--ui-font-mono`：路径、代码、版本号

字号用 `--ui-text-xs .. --ui-text-4xl`（12 / 13 / 15 / 16 / 18 / 20 / 24 / 30 / 36px）。

**字距**（本轮新增，高级感的关键）：
- 大字号标题用 `--ui-tracking-tight`（-0.02em）
- 中等字号用 `--ui-tracking-snug`（-0.01em）
- 小字号正文用 `--ui-tracking-normal`
- 不要对中文做 `text-transform: uppercase`

中文排版：
- 正文行高 `--ui-line-normal`（1.5），标题 `--ui-line-tight`（1.25）
- 中英混排不要手动加空格，用 `text-wrap: pretty` / `word-break: break-word`
- 数字用 `font-variant-numeric: tabular-nums`（`.ui-stat__value` 已带）

## 4. 图标体系（统一用 AppIcon，禁止手写 SVG）

图标来自 **Phosphor Icons**（regular 权重，MIT 许可），已抽取成纯路径表：

- 路径表：`src/icons/paths.js`（132 个图标，键名即图标名）
- 组件：`src/components/common/AppIcon.vue`
- 重新生成：`cd desktop/renderer && node scripts/extract-icons.mjs`

用法：

```vue
<script setup>
import AppIcon from '../components/common/AppIcon.vue';
</script>

<template>
  <AppIcon name="gear" :size="18" />
  <AppIcon name="spinner-gap" :size="16" spin />
</template>
```

规则：

- **禁止再手写 `<svg>` 图标**（本文件此前允许内联 SVG，本轮起作废）。
- `size` 默认 20px。导航 18px、正文内联 16px、大图标 24px。
- 图标颜色跟 `currentColor`，用 CSS 控制，不要给 `<AppIcon>` 写死颜色。
- 图标必须带 `aria-hidden`（组件已内置），纯装饰图标不要再写 `alt`。
- 文字按钮优先纯文字；「图标 + 文字」时图标给 16–18px。
- 常用图标名速查：
  `gear` `user` `chat-circle` `chat-circle-dots` `sparkle` `magic-wand` `robot` `brain`
  `file-text` `files` `file-plus` `folder` `paper-plane-tilt` `target` `lightbulb`
  `plus` `minus` `x` `check` `check-circle` `x-circle` `warning-circle` `info`
  `magnifying-glass` `funnel` `dots-three` `dots-three-vertical` `caret-right` `caret-down`
  `arrow-right` `arrow-left` `trash` `copy` `pencil-simple` `note-pencil`
  `download-simple` `upload-simple` `image` `camera` `microphone`
  `clock` `clock-counter-clockwise` `calendar` `star` `heart` `medal` `trophy`
  `certificate` `graduation-cap` `briefcase` `buildings` `envelope` `link`
  `sign-out` `sign-in` `moon` `sun` `sliders` `toggle-left` `eye` `eye-slash`
  `lock` `key` `squares-four` `layout` `templates` `user-circle` `users-three`
  `code` `terminal-window` `table` `chart-line` `chart-bar` `pulse` `desktop`
  `database` `cloud-arrow-up` `paperclip` `bell` `question` `list` `list-checks`

  完整清单看 `src/icons/paths.js`。**不要新增图标名**：需要新图标时，
  把 phosphor 的 kebab-case 名字加进 `scripts/extract-icons.mjs` 的 `ICONS` 数组
  并重跑脚本。

## 4.5 复用现成组件类

`design-system.css` 已提供，**优先用它们，别再各写各的按钮和输入框**：

基础类：
- `.ui-card` 面板/卡片（平面底色与细边界）
- `.ui-panel` 内容面板，加 `--hover` 出轻悬浮态
- `.ui-panel--raised` 仅用于菜单、重要摘要和短暂浮层等少数需要抬升的区域
- `.ui-panel--flat` 无阴影 / `--flat` 变体
- `.ui-btn` + `--primary` / `--outline` / `--ghost` / `--subtle` / `--danger` / `--sm` / `--lg` / `--block`
- `.ui-input` 输入框/文本域/下拉，配 `.ui-textarea` 多行
- `.ui-label` 表表标签；`.ui-field` + `__hint` + `__error` 表单字段组
- `.ui-badge` + `--accent` / `--success` / `--warning` / `--danger`
- `.ui-empty` + `__title` `__hint` `__icon` 空状态
- `.ui-divider` 分隔线
- `.ui-scroll` 细滚动条容器
- `.ui-page-title` / `.ui-page-subtitle` 页面标题
- `.ui-section-head` + `__title` `__hint` 分区标题

图标与控件：
- `.ui-icon-tile` + `--lg` / `--sm` / `--muted` 图标底座
- `.ui-icon-btn` + `--outline` 纯图标按钮
- `.ui-segmented` + `__item`（`.is-active`）分段控件

状态与数据：
- `.ui-skeleton` 骨架屏（加载态优先用它，别用整页遮罩）
- `.ui-stat` + `__value` `__label` 统计块
- `.ui-callout` + `--accent` / `--danger` 提示条

## 5. 间距 / 圆角 / 阴影

- 间距用 `--ui-space-1..20`（4 / 8 / 12 / 16 / 20 / 24 / 28 / 32 / 36 / 40 / 44 / 48 / 56 / 64 / 80px）
- 页面内边距：常规 `--ui-space-8`，密集表单 `--ui-space-6`
- 圆角：控件 `--ui-radius-md`，卡片 `--ui-radius-lg`，徽标 `--ui-radius-full`
  （圆角刻度只有一套，别自造）
- 阴影只用 `--ui-shadow-xs/sm/md/lg/xl/float/accent`，
  不要自定义 rgba 阴影。阴影**必须带底色染色**，禁止纯黑投影。
- 抬升层级 `--ui-shadow-xs/sm`：只用于少数需要从纸面浮起的工作区或浮层
- 表面材质 `--ui-surface-wash` / `--ui-bg-app-wash` / `--ui-accent-gradient`
- 动效用 `--ui-duration-fast/normal/slow` + `--ui-ease-out/in-out`
- 按下反馈：可点元素 `:active` 时 `translateY(1px)`（`.ui-btn` 已带）

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

1. **不新增 npm 依赖**（UI 库一律不装；图标用内置 `src/icons/paths.js`）
2. 不改 `src/styles/design-system.css`、`src/styles/main.css`、
   `src/styles/green-theme.css`、`src/main.js`、`src/router/index.js`、
   `src/store/**`、`src/api/**`、`src/utils/**`、`src/icons/paths.js`、
   `src/components/common/AppIcon.vue`（除非任务书明确授权）
3. 不定义新的 `:root` 变量；不要写 `#22c55e` 这类硬编码颜色
4. 不改 `src/components/resume/**` 里不属于你负责的文件（避免并行冲突）
5. 不重命名 Vue 组件名、props、emit 事件、store 的 action/getter
6. 不动 `desktop/electron/**`（主进程）和 `backend/**`（后端）
7. **不手写 `<svg>` 图标**，一律用 `<AppIcon>`（见第 4 节）
8. **不用外发光 / 霓虹描边 / 渐变文字 / 纯黑阴影**
9. **不用中文破折号「——」当装饰**，也不要用「·」串起一长串元信息
10. 不用假数据占位（假头像、假统计数字如「99.9%」），空态就明确写空态

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
