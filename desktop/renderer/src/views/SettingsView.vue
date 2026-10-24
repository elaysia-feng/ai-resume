<template>
  <div class="settings-page">
    <header class="settings-header">
      <div>
        <h1 class="page-title">设置</h1>
        <p class="page-subtitle">配置模型接入与后端地址，桌面端会写入安装目录的 config/application.yaml。</p>
      </div>
      <div class="header-actions">
        <button class="ghost-btn" :disabled="loading" @click="loadConfig">
          {{ loading ? '加载中...' : '重新加载' }}
        </button>
      </div>
    </header>

    <p v-if="environmentTip" class="environment-tip">{{ environmentTip }}</p>

    <!-- 区块一：对话模型（Java 链路） -->
    <section class="panel-card">
      <div class="panel-head">
        <div>
          <h3>对话模型（Java 链路）</h3>
          <p>这一组配置给 Java 后端的对话循环用，走 Anthropic 兼容端点。</p>
        </div>
      </div>
      <div class="field-grid">
        <div class="field">
          <label class="field-label" for="chat-base-url">Base URL</label>
          <input id="chat-base-url" v-model="config.chat.baseUrl" type="text" placeholder="https://api.minimaxi.com/anthropic" />
        </div>
        <div class="field">
          <label class="field-label" for="chat-api-key">API Key</label>
          <div class="input-action">
            <input id="chat-api-key" v-model="config.chat.apiKey" :type="showChatKey ? 'text' : 'password'" placeholder="sk-..." autocomplete="off" />
            <button class="toggle-btn" type="button" @click="showChatKey = !showChatKey">
              {{ showChatKey ? '隐藏' : '显示' }}
            </button>
          </div>
        </div>
        <div class="field">
          <label class="field-label" for="chat-model">模型名</label>
          <input id="chat-model" v-model="config.chat.model" type="text" placeholder="MiniMax-M3.1-Flash" />
        </div>
        <div class="field">
          <label class="field-label" for="chat-max-tokens">最大输出 token</label>
          <input id="chat-max-tokens" v-model.number="config.chat.maxTokens" type="number" min="1" step="1" />
        </div>
      </div>
    </section>

    <!-- 区块二：Agent 模型（Python 链路） -->
    <section class="panel-card">
      <div class="panel-head">
        <div>
          <h3>Agent 模型（Python 链路）</h3>
          <p>这一组配置给 Python AI 运行时用，走 OpenAI 兼容端点。</p>
        </div>
      </div>
      <div class="field-grid">
        <div class="field">
          <label class="field-label" for="agent-provider">供应商</label>
          <select id="agent-provider" v-model="config.agent.provider">
            <option v-for="option in providerOptions" :key="option" :value="option">{{ option }}</option>
          </select>
          <span class="field-hint">选择 deepseek 或 mimo 会自动填入对应的 Base URL 与模型名，custom 需要自己填写。</span>
        </div>
        <div class="field">
          <label class="field-label" for="agent-base-url">Base URL</label>
          <input id="agent-base-url" v-model="config.agent.baseUrl" type="text" placeholder="https://api.deepseek.com/v1" />
        </div>
        <div class="field">
          <label class="field-label" for="agent-api-key">API Key</label>
          <div class="input-action">
            <input id="agent-api-key" v-model="config.agent.apiKey" :type="showAgentKey ? 'text' : 'password'" placeholder="sk-..." autocomplete="off" />
            <button class="toggle-btn" type="button" @click="showAgentKey = !showAgentKey">
              {{ showAgentKey ? '隐藏' : '显示' }}
            </button>
          </div>
        </div>
        <div class="field">
          <label class="field-label" for="agent-model">模型名</label>
          <input id="agent-model" v-model="config.agent.model" type="text" placeholder="deepseek-chat" />
        </div>
      </div>
    </section>

    <!-- 区块三：服务地址 -->
    <section class="panel-card">
      <div class="panel-head">
        <div>
          <h3>服务地址</h3>
          <p>桌面端启动后端进程时使用的监听地址，改完需要重启后端。</p>
        </div>
      </div>
      <div class="field-grid">
        <div class="field">
          <label class="field-label" for="server-host">Java 后端 host</label>
          <input id="server-host" v-model="config.server.host" type="text" placeholder="127.0.0.1" />
        </div>
        <div class="field">
          <label class="field-label" for="server-port">Java 后端 port</label>
          <input id="server-port" v-model.number="config.server.port" type="number" min="1" max="65535" step="1" />
        </div>
        <div class="field">
          <label class="field-label" for="python-host">Python 运行时 host</label>
          <input id="python-host" v-model="config.python.host" type="text" placeholder="127.0.0.1" />
        </div>
        <div class="field">
          <label class="field-label" for="python-port">Python 运行时 port</label>
          <input id="python-port" v-model.number="config.python.port" type="number" min="1" max="65535" step="1" />
        </div>
        <div class="field field-wide">
          <label class="field-label" for="internal-token">内部接口令牌 internalToken</label>
          <input id="internal-token" v-model="config.internalToken" type="text" placeholder="change-me" autocomplete="off" />
          <span class="field-hint">Java 与 Python 两端必须一致，否则两端互相调用会失败。</span>
        </div>
      </div>
    </section>

    <!-- 区块四：后端进程状态 -->
    <section class="panel-card">
      <div class="panel-head">
        <div>
          <h3>后端进程状态</h3>
          <p>查看两个后端进程是否在运行，以及最近一次退出原因。</p>
        </div>
        <button class="ghost-btn" :disabled="restarting" @click="restartBackends">
          {{ restarting ? '重启中...' : '重启后端' }}
        </button>
      </div>
      <div class="process-list">
        <div v-for="item in processStates" :key="item.id" class="process-item">
          <div class="process-main">
            <span class="process-name">{{ processLabel(item.id) }}</span>
            <span class="status-badge" :class="item.running ? 'running' : 'stopped'">
              {{ item.running ? '运行中' : '未运行' }}
            </span>
          </div>
          <div class="process-meta">
            <span>已重启 {{ item.restarts }} 次</span>
            <span>
              最近退出：{{ item.lastExitReason || '无' }}
              <template v-if="item.lastExitCode !== null">（退出码 {{ item.lastExitCode }}）</template>
            </span>
          </div>
        </div>
      </div>
    </section>

    <footer class="settings-footer">
      <p v-if="message" class="save-message" :class="messageType">{{ message }}</p>
      <button class="primary-btn" :disabled="saving" @click="saveConfig">
        {{ saving ? '保存中...' : '保存配置' }}
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
const message = ref('');
const messageType = ref('');

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

function describeError(error) {
  return error?.message || String(error || '未知错误');
}
</script>

<style scoped>
.settings-page {
  display: flex;
  flex-direction: column;
  gap: 24px;
  max-width: 960px;
  margin: 0 auto;
  padding: 32px;
}

.settings-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.page-title {
  font-size: 24px;
  font-weight: 700;
  color: #1a1a2e;
  margin: 0;
}

.page-subtitle {
  margin: 8px 0 0;
  font-size: 14px;
  color: #6b7280;
}

.header-actions {
  display: flex;
  gap: 12px;
}

.environment-tip {
  margin: 0;
  padding: 10px 14px;
  font-size: 13px;
  line-height: 1.6;
  color: #92400e;
  background: #fffbeb;
  border: 1px solid #fde68a;
  border-radius: 12px;
}

.panel-card {
  background: #fff;
  border-radius: 18px;
  padding: 22px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.06);
}

.panel-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 18px;
}

.panel-head h3 {
  margin: 0;
  font-size: 18px;
  color: #111827;
}

.panel-head p {
  margin: 6px 0 0;
  font-size: 13px;
  color: #6b7280;
}

.field-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.field {
  display: flex;
  flex-direction: column;
  gap: 8px;
  min-width: 0;
}

.field-wide {
  grid-column: 1 / -1;
}

.field-label {
  font-size: 13px;
  font-weight: 600;
  color: #374151;
}

.field-hint {
  font-size: 12px;
  line-height: 1.6;
  color: #6b7280;
}

.field input,
.field select {
  width: 100%;
  padding: 10px 12px;
  font-size: 14px;
  color: #111827;
  background: #fff;
  border: 1px solid #d1d5db;
  border-radius: 10px;
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}

.field input:focus,
.field select:focus {
  outline: none;
  border-color: #22c55e;
  box-shadow: 0 0 0 3px rgba(34, 197, 94, 0.15);
}

.input-action {
  display: flex;
  align-items: center;
  gap: 8px;
}

.input-action input {
  flex: 1;
  min-width: 0;
}

.toggle-btn {
  flex-shrink: 0;
  padding: 8px 12px;
  font-size: 13px;
  font-weight: 600;
  color: #374151;
  background: #fff;
  border: 1px solid #d1d5db;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.2s;
}

.toggle-btn:hover {
  border-color: #86efac;
  color: #15803d;
}

.process-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.process-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 16px;
  border: 1px solid #e5e7eb;
  border-radius: 14px;
  background: #f8fafc;
}

.process-main {
  display: flex;
  align-items: center;
  gap: 10px;
}

.process-name {
  font-size: 15px;
  font-weight: 700;
  color: #111827;
}

.status-badge {
  padding: 4px 10px;
  font-size: 12px;
  font-weight: 700;
  border-radius: 999px;
}

.status-badge.running {
  color: #166534;
  background: #dcfce7;
}

.status-badge.stopped {
  color: #991b1b;
  background: #fee2e2;
}

.process-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  font-size: 12px;
  color: #6b7280;
}

.settings-footer {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 16px;
}

.save-message {
  margin: 0;
  font-size: 14px;
  font-weight: 600;
}

.save-message.success {
  color: #16a34a;
}

.save-message.error {
  color: #dc2626;
}

.primary-btn,
.ghost-btn {
  border: none;
  cursor: pointer;
  transition: all 0.2s;
}

.primary-btn {
  padding: 10px 18px;
  border-radius: 10px;
  background: #22c55e;
  color: #fff;
  font-size: 14px;
  font-weight: 600;
}

.primary-btn:hover:not(:disabled) {
  background: #16a34a;
}

.ghost-btn {
  padding: 10px 16px;
  border-radius: 10px;
  background: #fff;
  border: 1px solid #d1d5db;
  font-size: 14px;
  font-weight: 600;
  color: #374151;
}

.ghost-btn:hover:not(:disabled) {
  border-color: #86efac;
  color: #15803d;
}

.primary-btn:disabled,
.ghost-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

@media (max-width: 900px) {
  .field-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .settings-page {
    padding: 20px 16px;
  }

  .settings-header,
  .settings-footer {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
