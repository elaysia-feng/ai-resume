<template>
  <div class="settings-page">
    <header class="page-head">
      <div class="page-head__text">
        <h1 class="page-title">设置</h1>
        <p class="page-subtitle">配置对话模型、Agent 模型、服务地址与后端进程状态。桌面端保存后会写入安装目录的 config/application.yaml。</p>
      </div>
      <div class="page-head__actions">
        <button class="ui-btn ui-btn--outline" :disabled="loading" @click="loadConfig">
          {{ loading ? '加载中…' : '重新加载' }}
        </button>
      </div>
    </header>

    <p v-if="environmentTip" class="notice notice--warning">{{ environmentTip }}</p>

    <div class="settings-layout">
      <!-- 左侧分区目录：点击切换右侧区块 -->
      <nav class="section-nav" aria-label="设置分区目录">
        <p class="section-nav__caption">配置分区</p>
        <button
          v-for="section in SECTIONS"
          :key="section.id"
          type="button"
          class="section-nav__item"
          :class="{ 'is-active': activeSection === section.id }"
          :aria-current="activeSection === section.id ? 'true' : undefined"
          @click="activeSection = section.id"
        >
          <span class="section-nav__title">{{ section.title }}</span>
          <span class="section-nav__meta">{{ sectionMeta(section.id) }}</span>
        </button>
      </nav>

      <div class="section-body">
        <!-- 区块一：对话模型（Java 链路） -->
        <section v-show="activeSection === 'chat'" class="ui-card block">
          <header class="block__head">
            <div>
              <h2 class="block__title">对话模型（Java 链路）</h2>
              <p class="block__desc">这一组配置给 Java 后端的对话循环用，走 Anthropic 兼容端点。</p>
            </div>
            <span class="ui-badge ui-badge--accent">{{ config.chat.model || '未设置模型' }}</span>
          </header>

          <div class="field-grid">
            <div class="field">
              <label class="ui-label" for="chat-base-url">Base URL</label>
              <input id="chat-base-url" v-model="config.chat.baseUrl" class="ui-input" type="text" placeholder="https://api.minimaxi.com/anthropic" />
            </div>
            <div class="field">
              <label class="ui-label" for="chat-api-key">API Key</label>
              <div class="input-with-action">
                <input id="chat-api-key" v-model="config.chat.apiKey" class="ui-input" :type="showChatKey ? 'text' : 'password'" placeholder="sk-…" autocomplete="off" />
                <button type="button" class="ui-btn ui-btn--outline ui-btn--sm" @click="showChatKey = !showChatKey">
                  {{ showChatKey ? '隐藏' : '显示' }}
                </button>
              </div>
              <p class="field__hint">密钥只写入本机配置文件，不会上传到第三方服务。</p>
            </div>
            <div class="field">
              <label class="ui-label" for="chat-model">模型名</label>
              <input id="chat-model" v-model="config.chat.model" class="ui-input" type="text" placeholder="MiniMax-M3.1-Flash" />
            </div>
            <div class="field">
              <label class="ui-label" for="chat-max-tokens">最大输出 token</label>
              <input id="chat-max-tokens" v-model.number="config.chat.maxTokens" class="ui-input" type="number" min="1" step="1" />
              <p class="field__hint">单次请求允许生成的最大 token 数。</p>
            </div>
          </div>
        </section>

        <!-- 区块二：Agent 模型（Python 链路） -->
        <section v-show="activeSection === 'agent'" class="ui-card block">
          <header class="block__head">
            <div>
              <h2 class="block__title">Agent 模型（Python 链路）</h2>
              <p class="block__desc">这一组配置给 Python AI 运行时用，走 OpenAI 兼容端点。</p>
            </div>
            <span class="ui-badge ui-badge--accent">{{ config.agent.provider || 'custom' }}</span>
          </header>

          <div class="field-grid">
            <div class="field">
              <label class="ui-label" for="agent-provider">供应商</label>
              <select id="agent-provider" v-model="config.agent.provider" class="ui-input ui-select">
                <option v-for="option in providerOptions" :key="option" :value="option">{{ option }}</option>
              </select>
              <p class="field__hint">选择 deepseek 或 mimo 会自动填入对应的 Base URL 与模型名，custom 需要自己填写。</p>
            </div>
            <div class="field">
              <label class="ui-label" for="agent-base-url">Base URL</label>
              <input id="agent-base-url" v-model="config.agent.baseUrl" class="ui-input" type="text" placeholder="https://api.deepseek.com/v1" />
            </div>
            <div class="field">
              <label class="ui-label" for="agent-api-key">API Key</label>
              <div class="input-with-action">
                <input id="agent-api-key" v-model="config.agent.apiKey" class="ui-input" :type="showAgentKey ? 'text' : 'password'" placeholder="sk-…" autocomplete="off" />
                <button type="button" class="ui-btn ui-btn--outline ui-btn--sm" @click="showAgentKey = !showAgentKey">
                  {{ showAgentKey ? '隐藏' : '显示' }}
                </button>
              </div>
            </div>
            <div class="field">
              <label class="ui-label" for="agent-model">模型名</label>
              <input id="agent-model" v-model="config.agent.model" class="ui-input" type="text" placeholder="deepseek-chat" />
            </div>
          </div>
        </section>

        <!-- 区块三：服务地址 -->
        <section v-show="activeSection === 'server'" class="ui-card block">
          <header class="block__head">
            <div>
              <h2 class="block__title">服务地址</h2>
              <p class="block__desc">桌面端启动后端进程时使用的监听地址，改完需要重启后端。</p>
            </div>
            <span class="ui-badge">{{ serverAddressText }}</span>
          </header>

          <div class="field-grid">
            <div class="field">
              <label class="ui-label" for="server-host">Java 后端 host</label>
              <input id="server-host" v-model="config.server.host" class="ui-input" type="text" placeholder="127.0.0.1" />
            </div>
            <div class="field">
              <label class="ui-label" for="server-port">Java 后端 port</label>
              <input id="server-port" v-model.number="config.server.port" class="ui-input" type="number" min="1" max="65535" step="1" />
            </div>
            <div class="field">
              <label class="ui-label" for="python-host">Python 运行时 host</label>
              <input id="python-host" v-model="config.python.host" class="ui-input" type="text" placeholder="127.0.0.1" />
            </div>
            <div class="field">
              <label class="ui-label" for="python-port">Python 运行时 port</label>
              <input id="python-port" v-model.number="config.python.port" class="ui-input" type="number" min="1" max="65535" step="1" />
            </div>
            <div class="field field--wide">
              <label class="ui-label" for="internal-token">内部接口令牌 internalToken</label>
              <div class="input-with-action">
                <input id="internal-token" v-model="config.internalToken" class="ui-input" :type="showToken ? 'text' : 'password'" placeholder="change-me" autocomplete="off" />
                <button type="button" class="ui-btn ui-btn--outline ui-btn--sm" @click="showToken = !showToken">
                  {{ showToken ? '隐藏' : '显示' }}
                </button>
              </div>
              <p class="field__hint">Java 与 Python 两端必须一致，否则两端互相调用会失败。</p>
            </div>
          </div>
        </section>

        <!-- 区块四：后端进程状态 -->
        <section v-show="activeSection === 'process'" class="ui-card block">
          <header class="block__head">
            <div>
              <h2 class="block__title">后端进程状态</h2>
              <p class="block__desc">查看两个后端进程是否在运行，以及最近一次退出原因。</p>
            </div>
            <button class="ui-btn ui-btn--outline" :disabled="restarting" @click="restartBackends">
              {{ restarting ? '重启中…' : '重启后端' }}
            </button>
          </header>

          <ul class="process-list">
            <li v-for="item in processStates" :key="item.id" class="process-item">
              <div class="process-item__head">
                <span class="process-item__name">{{ processLabel(item.id) }}</span>
                <span class="ui-badge" :class="processBadge(item).tone">{{ processBadge(item).label }}</span>
              </div>
              <dl class="process-item__meta">
                <div class="process-item__row">
                  <dt>监听地址</dt>
                  <dd>{{ processAddress(item.id) }}</dd>
                </div>
                <div class="process-item__row">
                  <dt>重启次数</dt>
                  <dd>{{ item.restarts }} 次</dd>
                </div>
                <div class="process-item__row">
                  <dt>最近退出</dt>
                  <dd>
                    {{ item.lastExitReason || '无' }}
                    <span v-if="item.lastExitCode !== null">（退出码 {{ item.lastExitCode }}）</span>
                  </dd>
                </div>
              </dl>
            </li>
          </ul>
        </section>
      </div>
    </div>

    <footer class="save-bar">
      <p class="save-message" :class="messageType === 'success' ? 'is-success' : messageType === 'error' ? 'is-error' : ''" role="status" aria-live="polite">
        {{ message }}
      </p>
      <button class="ui-btn ui-btn--primary" :disabled="saving" @click="saveConfig">
        {{ saving ? '保存中…' : '保存配置' }}
      </button>
    </footer>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue';

// 供应商预设：只在用户切换供应商时用于自动填充 Base URL 与模型名
const PROVIDER_PRESETS = {
  deepseek: { baseUrl: 'https://api.deepseek.com/v1', model: 'deepseek-chat' },
  mimo: { baseUrl: 'https://token-plan-cn.xiaomimimo.com/v1', model: 'mimo-v2.5' },
};

const PROVIDER_OPTIONS = ['deepseek', 'mimo', 'custom'];

// 左侧目录的四个分区，语义与原来的四个卡片一致
const SECTIONS = [
  { id: 'chat', title: '对话模型' },
  { id: 'agent', title: 'Agent 模型' },
  { id: 'server', title: '服务地址' },
  { id: 'process', title: '后端进程' },
];

// 浏览器调试模式下的本地存储键
const LOCAL_CONFIG_KEY = 'ai_resume_forge_app_config';

// 与主进程 defaultConfig() 保持一致，作为读不到配置时的兜底值
function createDefaultConfig() {
  return {
    chat: {
      baseUrl: 'https://api.minimaxi.com/anthropic',
      apiKey: '',
      model: 'MiniMax-M3.1-Flash',
      maxTokens: 4096,
    },
    agent: {
      provider: 'deepseek',
      baseUrl: 'https://api.deepseek.com/v1',
      apiKey: '',
      model: 'deepseek-chat',
    },
    server: {
      host: '127.0.0.1',
      port: 8080,
    },
    python: {
      host: '127.0.0.1',
      port: 8000,
    },
    internalToken: 'change-me',
  };
}

const config = ref(createDefaultConfig());
const processes = ref([]);
const loading = ref(false);
const saving = ref(false);
const restarting = ref(false);
const showChatKey = ref(false);
const showAgentKey = ref(false);
const showToken = ref(false);
const message = ref('');
const messageType = ref('');
const activeSection = ref('chat');

// 初始化期间不触发供应商自动填充，避免覆盖已保存的配置
const providerReady = ref(false);

let unsubscribeProcess = null;

const desktop = computed(() => (typeof window !== 'undefined' ? window.desktop : undefined));

const providerOptions = PROVIDER_OPTIONS;

const environmentTip = computed(() =>
  typeof desktop.value?.getAppConfig === 'function'
    ? ''
    : '当前不在桌面端环境（缺少 preload 桥接），配置只会保存在浏览器本地，不会写入 config/application.yaml。'
);

const processStates = computed(() => {
  const known = new Map(processes.value.map((item) => [item.id, item]));
  return ['java', 'python'].map((id) => known.get(id) || {
    id,
    running: false,
    restarts: 0,
    lastExitCode: null,
    lastExitReason: '',
  });
});

const serverAddressText = computed(() => {
  const host = config.value.server.host || '127.0.0.1';
  return `http://${host}:${config.value.server.port}`;
});

// 目录右侧的一行摘要，让每个分区当前配置一目了然
function sectionMeta(sectionId) {
  if (sectionId === 'chat') {
    return config.value.chat.model || '未设置模型';
  }
  if (sectionId === 'agent') {
    return config.value.agent.model || '未设置模型';
  }
  if (sectionId === 'server') {
    return serverAddressText.value;
  }
  const running = processStates.value.filter((item) => item.running).length;
  return `${running}/${processStates.value.length} 运行中`;
}

watch(
  () => config.value.agent.provider,
  (provider) => {
    if (!providerReady.value) {
      return;
    }
    const preset = PROVIDER_PRESETS[provider];
    if (!preset) {
      // custom 由用户自己填写，不做任何自动修改
      return;
    }
    config.value.agent.baseUrl = preset.baseUrl;
    config.value.agent.model = preset.model;
  },
  { flush: 'sync' }
);

onMounted(() => {
  loadConfig();
  loadProcesses();
  if (typeof desktop.value?.onBackendProcessChanged === 'function') {
    unsubscribeProcess = desktop.value.onBackendProcessChanged((state) => {
      if (!state?.id) {
        return;
      }
      const index = processes.value.findIndex((item) => item.id === state.id);
      if (index >= 0) {
        processes.value[index] = { ...processes.value[index], ...state };
      } else {
        processes.value = [...processes.value, state];
      }
    });
  }
});

onBeforeUnmount(() => {
  if (typeof unsubscribeProcess === 'function') {
    unsubscribeProcess();
    unsubscribeProcess = null;
  }
});

async function loadConfig() {
  loading.value = true;
  try {
    const loaded = typeof desktop.value?.getAppConfig === 'function'
      ? await desktop.value.getAppConfig()
      : readLocalConfig();
    applyConfig(loaded);
    message.value = '';
    messageType.value = '';
  } catch (error) {
    applyConfig(createDefaultConfig());
    setMessage(`读取配置失败：${describeError(error)}`, 'error');
  } finally {
    loading.value = false;
  }
}

async function saveConfig() {
  saving.value = true;
  try {
    const payload = buildPayload();
    if (typeof desktop.value?.saveAppConfig === 'function') {
      const saved = await desktop.value.saveAppConfig(payload);
      applyConfig(saved);
    } else {
      writeLocalConfig(payload);
    }
    setMessage('配置已保存，重启后端后生效', 'success');
  } catch (error) {
    setMessage(`保存配置失败：${describeError(error)}`, 'error');
  } finally {
    saving.value = false;
  }
}

async function restartBackends() {
  if (typeof desktop.value?.restartBackends !== 'function') {
    setMessage('当前不在桌面端环境，无法重启后端', 'error');
    return;
  }
  restarting.value = true;
  try {
    await desktop.value.restartBackends();
    setMessage('已触发后端重启', 'success');
    await loadProcesses();
  } catch (error) {
    setMessage(`重启后端失败：${describeError(error)}`, 'error');
  } finally {
    restarting.value = false;
  }
}

async function loadProcesses() {
  if (typeof desktop.value?.getBackendProcesses !== 'function') {
    return;
  }
  try {
    const list = await desktop.value.getBackendProcesses();
    processes.value = Array.isArray(list) ? list : [];
  } catch (error) {
    console.error('读取后端进程状态失败', error);
  }
}

// 用新配置覆盖表单，期间关闭供应商自动填充
function applyConfig(next) {
  providerReady.value = false;
  config.value = normalizeConfig(next);
  providerReady.value = true;
}

function normalizeConfig(source) {
  const base = createDefaultConfig();
  if (!source || typeof source !== 'object') {
    return base;
  }
  return {
    chat: { ...base.chat, ...(source.chat || {}) },
    agent: { ...base.agent, ...(source.agent || {}) },
    server: { ...base.server, ...(source.server || {}) },
    python: { ...base.python, ...(source.python || {}) },
    internalToken: source.internalToken ?? base.internalToken,
  };
}

// 组装保存用的配置，端口与 token 数兜底成合法数字
function buildPayload() {
  const base = createDefaultConfig();
  return {
    chat: {
      baseUrl: String(config.value.chat.baseUrl || '').trim(),
      apiKey: String(config.value.chat.apiKey || '').trim(),
      model: String(config.value.chat.model || '').trim(),
      maxTokens: toPositiveInt(config.value.chat.maxTokens, base.chat.maxTokens),
    },
    agent: {
      provider: String(config.value.agent.provider || 'custom').trim(),
      baseUrl: String(config.value.agent.baseUrl || '').trim(),
      apiKey: String(config.value.agent.apiKey || '').trim(),
      model: String(config.value.agent.model || '').trim(),
    },
    server: {
      host: String(config.value.server.host || '').trim(),
      port: toPositiveInt(config.value.server.port, base.server.port, 65535),
    },
    python: {
      host: String(config.value.python.host || '').trim(),
      port: toPositiveInt(config.value.python.port, base.python.port, 65535),
    },
    internalToken: String(config.value.internalToken || '').trim(),
  };
}

function toPositiveInt(value, fallback, max = Number.MAX_SAFE_INTEGER) {
  const parsed = Number.parseInt(value, 10);
  if (!Number.isFinite(parsed) || parsed <= 0 || parsed > max) {
    return fallback;
  }
  return parsed;
}

function readLocalConfig() {
  try {
    const raw = window.localStorage.getItem(LOCAL_CONFIG_KEY);
    return raw ? JSON.parse(raw) : createDefaultConfig();
  } catch {
    return createDefaultConfig();
  }
}

function writeLocalConfig(payload) {
  try {
    window.localStorage.setItem(LOCAL_CONFIG_KEY, JSON.stringify(payload));
  } catch {
    // 浏览器禁用了本地存储时忽略，表单内容仍然保留
  }
}

function setMessage(text, type) {
  message.value = text;
  messageType.value = type;
}

function processLabel(id) {
  if (id === 'java') {
    return 'Java 后端';
  }
  if (id === 'python') {
    return 'Python 运行时';
  }
  return id;
}

// 进程状态徽标：运行中 / 重启中 / 异常退出 / 已停止
function processBadge(item) {
  if (restarting.value) {
    return { label: '重启中', tone: 'ui-badge--warning' };
  }
  if (item.running) {
    return { label: '运行中', tone: 'ui-badge--success' };
  }
  if (item.lastExitCode !== null && item.lastExitCode !== 0) {
    return { label: '异常退出', tone: 'ui-badge--danger' };
  }
  return { label: '已停止', tone: '' };
}

function processAddress(id) {
  if (id === 'python') {
    return `${config.value.python.host || '127.0.0.1'}:${config.value.python.port}`;
  }
  return `${config.value.server.host || '127.0.0.1'}:${config.value.server.port}`;
}

function describeError(error) {
  return error?.message || String(error || '未知错误');
}
</script>

<style scoped>
.settings-page {
  display: flex;
  flex-direction: column;
  gap: var(--ui-space-6);
  max-width: 1080px;
  margin: 0 auto;
  padding: 0;
}

/* ---------- 页头 ---------- */
.page-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--ui-space-4);
}

.page-title {
  margin: 0;
  font-size: var(--ui-text-2xl);
  font-weight: 700;
  line-height: var(--ui-line-tight);
  color: var(--ui-fg-primary);
}

.page-subtitle {
  margin: var(--ui-space-2) 0 0;
  font-size: var(--ui-text-base);
  color: var(--ui-fg-secondary);
}

.page-head__actions {
  display: flex;
  flex-shrink: 0;
  gap: var(--ui-space-2);
}

/* ---------- 环境提示 ---------- */
.notice {
  margin: 0;
  padding: var(--ui-space-3) var(--ui-space-4);
  border-radius: var(--ui-radius-md);
  font-size: var(--ui-text-sm);
  line-height: var(--ui-line-normal);
}

.notice--warning {
  background: var(--ui-warning-soft);
  border: 1px solid var(--ui-warning-border);
  color: var(--ui-fg-primary);
}

/* ---------- 布局：左侧目录 + 右侧分区 ---------- */
.settings-layout {
  display: grid;
  grid-template-columns: 208px minmax(0, 1fr);
  gap: var(--ui-space-6);
  align-items: start;
}

.section-nav {
  display: flex;
  flex-direction: column;
  gap: var(--ui-space-1);
  position: sticky;
  top: 0;
}

.section-nav__caption {
  margin: 0 0 var(--ui-space-2);
  padding-left: var(--ui-space-3);
  font-size: var(--ui-text-xs);
  color: var(--ui-fg-muted);
}

.section-nav__item {
  display: flex;
  flex-direction: column;
  gap: 2px;
  width: 100%;
  padding: var(--ui-space-2) var(--ui-space-3);
  border: 1px solid transparent;
  border-radius: var(--ui-radius-md);
  background: transparent;
  color: var(--ui-fg-secondary);
  text-align: left;
  cursor: pointer;
  transition: background var(--ui-duration-fast) var(--ui-ease-in-out),
    color var(--ui-duration-fast) var(--ui-ease-in-out),
    border-color var(--ui-duration-fast) var(--ui-ease-in-out);
}

.section-nav__item:hover {
  background: var(--ui-bg-hover);
  color: var(--ui-fg-primary);
}

.section-nav__item:focus-visible {
  outline: none;
  box-shadow: var(--ui-ring);
}

.section-nav__item.is-active {
  background: var(--ui-bg-selected);
  border-color: var(--ui-accent-border);
  color: var(--ui-fg-primary);
}

.section-nav__title {
  font-size: var(--ui-text-base);
  font-weight: 600;
}

.section-nav__meta {
  font-size: var(--ui-text-xs);
  color: var(--ui-fg-muted);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.section-nav__item.is-active .section-nav__meta {
  color: var(--ui-fg-secondary);
}

/* ---------- 分区卡片 ---------- */
.section-body {
  min-width: 0;
}

.block {
  padding: var(--ui-space-6);
}

.block__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--ui-space-4);
  padding-bottom: var(--ui-space-4);
  margin-bottom: var(--ui-space-5);
  border-bottom: 1px solid var(--ui-border);
}

.block__title {
  margin: 0;
  font-size: var(--ui-text-lg);
  font-weight: 600;
  color: var(--ui-fg-primary);
}

.block__desc {
  margin: var(--ui-space-1) 0 0;
  font-size: var(--ui-text-sm);
  color: var(--ui-fg-secondary);
}

/* ---------- 表单 ---------- */
.field-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--ui-space-5);
}

.field {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.field--wide {
  grid-column: 1 / -1;
}

.field__hint {
  margin: var(--ui-space-2) 0 0;
  font-size: var(--ui-text-xs);
  line-height: var(--ui-line-normal);
  color: var(--ui-fg-muted);
}

.input-with-action {
  display: flex;
  align-items: center;
  gap: var(--ui-space-2);
}

.input-with-action .ui-input {
  flex: 1;
  min-width: 0;
}

/* 下拉框复用 .ui-input 的边框与内边距，这里补一个跟随主题的下拉箭头 */
.ui-select {
  appearance: none;
  padding-right: var(--ui-space-8);
  background-image: linear-gradient(45deg, transparent 50%, currentColor 50%),
    linear-gradient(135deg, currentColor 50%, transparent 50%);
  background-position: calc(100% - 16px) calc(50% + 1px), calc(100% - 11px) calc(50% + 1px);
  background-size: 5px 5px, 5px 5px;
  background-repeat: no-repeat;
}

/* ---------- 进程状态 ---------- */
.process-list {
  display: flex;
  flex-direction: column;
  gap: var(--ui-space-3);
  margin: 0;
  padding: 0;
  list-style: none;
}

.process-item {
  padding: var(--ui-space-4);
  border: 1px solid var(--ui-border);
  border-radius: var(--ui-radius-md);
  background: var(--ui-bg-sunken);
}

.process-item__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--ui-space-3);
}

.process-item__name {
  font-size: var(--ui-text-base);
  font-weight: 600;
  color: var(--ui-fg-primary);
}

.process-item__meta {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: var(--ui-space-3);
  margin: var(--ui-space-3) 0 0;
}

.process-item__row {
  display: flex;
  flex-direction: column;
  gap: var(--ui-space-1);
  min-width: 0;
}

.process-item__row dt {
  font-size: var(--ui-text-xs);
  color: var(--ui-fg-muted);
}

.process-item__row dd {
  margin: 0;
  font-size: var(--ui-text-sm);
  color: var(--ui-fg-secondary);
  word-break: break-word;
}

/* ---------- 保存栏 ---------- */
.save-bar {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: var(--ui-space-4);
  padding: var(--ui-space-3) var(--ui-space-4);
  border: 1px solid var(--ui-border);
  border-radius: var(--ui-radius-lg);
  background: var(--ui-bg-surface);
}

.save-message {
  margin: 0;
  flex: 1;
  font-size: var(--ui-text-sm);
  color: var(--ui-fg-secondary);
}

.save-message.is-success {
  color: var(--ui-success);
}

.save-message.is-error {
  color: var(--ui-danger);
}

@media (max-width: 1100px) {
  .settings-layout {
    grid-template-columns: 1fr;
  }

  .section-nav {
    position: static;
    flex-direction: row;
    flex-wrap: wrap;
  }

  .section-nav__caption {
    display: none;
  }

  .section-nav__item {
    width: auto;
  }
}

@media (max-width: 860px) {
  .field-grid,
  .process-item__meta {
    grid-template-columns: 1fr;
  }

  .page-head,
  .save-bar {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
