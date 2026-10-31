<template>
  <div class="settings-page">
    <!-- 页头：标题 + 主操作 -->
    <header class="page-head">
      <div class="page-head__text">
        <h1 class="ui-page-title">设置</h1>
        <p class="ui-page-subtitle">配置简历优化、模拟面试和对话共用的模型连接，以及本地服务。</p>
      </div>
      <div class="page-head__actions">
        <button class="ui-btn ui-btn--outline" :disabled="loading" @click="loadConfig">
          <AppIcon :name="loading ? 'spinner-gap' : 'arrows-clockwise'" :size="16" :spin="loading" />
          {{ loading ? '加载中…' : '重新加载' }}
        </button>
      </div>
    </header>

    <!-- 非桌面环境提示 -->
    <p v-if="environmentTip" class="ui-callout notice--warning">
      <AppIcon name="warning-circle" :size="16" />
      <span>{{ environmentTip }}</span>
    </p>

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
          <span class="section-nav__icon"><AppIcon :name="section.icon" :size="16" /></span>
          <span class="section-nav__body">
            <span class="section-nav__title">{{ section.title }}</span>
            <span class="section-nav__meta">{{ sectionMeta(section.id) }}</span>
          </span>
        </button>
      </nav>

      <div class="section-body">
        <!-- 首次加载：骨架屏占位，不用整页遮罩 -->
        <div v-if="!configLoaded" class="skeleton" aria-hidden="true">
          <div class="ui-skeleton skeleton__title"></div>
          <div class="ui-skeleton skeleton__line"></div>
          <div class="ui-skeleton skeleton__field"></div>
          <div class="ui-skeleton skeleton__field"></div>
        </div>

        <template v-else>
          <!-- 统一模型配置由 Python AI 运行时读取。 -->
          <section v-show="activeSection === 'agent'" class="ui-panel block">
            <header class="ui-section-head">
              <div>
                <h2 class="ui-section-head__title">统一 AI 模型</h2>
                <p class="ui-section-head__hint">工作区对话、简历优化和模拟面试共用此模型。切换预设会填入常用地址和模型名，字段仍可编辑。</p>
              </div>
              <span class="ui-badge ui-badge--accent">{{ selectedProviderLabel }}</span>
            </header>

            <hr class="ui-divider block__divider" />

            <div class="field-grid">
              <div class="ui-field">
                <label class="ui-label" for="agent-provider">服务预设</label>
                <select id="agent-provider" v-model="config.agent.provider" class="ui-input ui-select">
                  <option v-for="option in providerOptions" :key="option.value" :value="option.value">{{ option.label }}</option>
                </select>
                <p class="ui-field__hint">预设只填入常见地址和模型名；中转站可选“自定义”，再指定它实现的协议。</p>
              </div>
              <div class="ui-field">
                <label class="ui-label" for="agent-model-provider">模型 API 协议 / 接入方式</label>
                <select id="agent-model-provider" v-model="config.agent.modelProvider" class="ui-input ui-select">
                  <option v-for="option in modelProviderOptions" :key="option.value" :value="option.value">{{ option.label }}</option>
                </select>
                <p class="ui-field__hint">按模型服务或中转站实际实现的请求格式选择。中转站只支持 OpenAI 兼容接口时，选择“OpenAI 兼容”。</p>
              </div>
              <div class="ui-field">
                <label class="ui-label" for="agent-base-url">Base URL</label>
                <input id="agent-base-url" v-model="config.agent.baseUrl" class="ui-input" type="text" :placeholder="config.agent.modelProvider === 'ollama' ? 'http://localhost:11434' : 'https://api.example.com'" />
                <p class="ui-field__hint">填写服务商或中转站的 API 根地址。Bedrock、Google 等特殊端点可在集成参数中填写。</p>
              </div>
              <div class="ui-field">
                <label class="ui-label" for="agent-api-key">API Key</label>
                <div class="input-with-action">
                  <input id="agent-api-key" v-model="config.agent.apiKey" class="ui-input" :type="showAgentKey ? 'text' : 'password'" placeholder="sk-…" autocomplete="off" />
                  <button
                    type="button"
                    class="ui-icon-btn ui-icon-btn--outline"
                    :title="showAgentKey ? '隐藏' : '显示'"
                    :aria-label="showAgentKey ? '隐藏' : '显示'"
                    @click="showAgentKey = !showAgentKey"
                  >
                    <AppIcon :name="showAgentKey ? 'eye-slash' : 'eye'" :size="16" />
                  </button>
                </div>
                <p class="ui-field__hint">密钥保存在本机，并发送到上方地址用于鉴权。Ollama 可留空；Bedrock 也可通过 AWS 配置或集成参数鉴权。</p>
              </div>
              <div class="ui-field">
                <label class="ui-label" for="agent-model">模型名</label>
                <input id="agent-model" v-model="config.agent.model" class="ui-input" type="text" placeholder="填写服务商提供的模型 ID" />
                <p class="ui-field__hint">模型 ID 以服务商控制台为准，可覆盖预设值。</p>
              </div>
              <div class="ui-field">
                <label class="ui-label" for="agent-max-tokens">最大输出 token</label>
                <input id="agent-max-tokens" v-model.number="config.agent.maxTokens" class="ui-input" type="number" min="1" step="1" />
                <p class="ui-field__hint">每次模型请求的输出上限。</p>
                <p v-if="agentMaxTokensError" class="ui-field__error">{{ agentMaxTokensError }}</p>
              </div>
              <div class="ui-field">
                <label class="ui-label" for="agent-extra-headers">中转站额外请求头（可选）</label>
                <textarea
                  id="agent-extra-headers"
                  v-model="extraHeadersText"
                  class="ui-input"
                  rows="4"
                  spellcheck="false"
                  placeholder='{"HTTP-Referer":"https://your-app.example","X-Title":"AI Resume Forge"}'
                ></textarea>
                <p class="ui-field__hint">支持的集成会把这些 HTTP 头发给中转站；部分集成需用下方原生参数配置。API Key 仍放在上方密钥栏。</p>
                <p v-if="extraHeadersError" class="ui-field__error">{{ extraHeadersError }}</p>
              </div>
              <div class="ui-field">
                <label class="ui-label" for="agent-provider-options">集成参数（可选）</label>
                <textarea
                  id="agent-provider-options"
                  v-model="providerOptionsText"
                  class="ui-input"
                  rows="4"
                  spellcheck="false"
                  placeholder='{"region_name":"us-east-1"}'
                ></textarea>
                <p class="ui-field__hint">特殊区域、鉴权和客户端参数可填 JSON，例如 Bedrock 的 region_name 或 Hugging Face 的 backend。中转站若不完整支持所选格式，工具调用或结构化输出可能不可用。</p>
                <p v-if="providerOptionsError" class="ui-field__error">{{ providerOptionsError }}</p>
              </div>
            </div>
          </section>

          <!-- 区块三：服务地址 -->
          <section v-show="activeSection === 'server'" class="ui-panel block">
            <header class="ui-section-head">
              <div>
                <h2 class="ui-section-head__title">服务地址</h2>
                <p class="ui-section-head__hint">桌面端启动后端进程时使用的监听地址，改完需要重启后端。</p>
              </div>
              <span class="ui-badge">{{ serverAddressText }}</span>
            </header>

            <hr class="ui-divider block__divider" />

            <div class="field-grid">
              <div class="ui-field">
                <label class="ui-label" for="server-host">Java 后端 host</label>
                <input id="server-host" v-model="config.server.host" class="ui-input" type="text" placeholder="127.0.0.1" />
                <p class="ui-field__hint">Java 后端监听的本机地址。</p>
              </div>
              <div class="ui-field">
                <label class="ui-label" for="server-port">Java 后端 port</label>
                <input id="server-port" v-model.number="config.server.port" class="ui-input" type="number" min="1" max="65535" step="1" />
                <p class="ui-field__hint">Java 后端监听的端口。</p>
                <p v-if="serverPortError" class="ui-field__error">{{ serverPortError }}</p>
              </div>
              <div class="ui-field">
                <label class="ui-label" for="python-host">Python 运行时 host</label>
                <input id="python-host" v-model="config.python.host" class="ui-input" type="text" placeholder="127.0.0.1" />
                <p class="ui-field__hint">Python 运行时监听的本机地址。</p>
              </div>
              <div class="ui-field">
                <label class="ui-label" for="python-port">Python 运行时 port</label>
                <input id="python-port" v-model.number="config.python.port" class="ui-input" type="number" min="1" max="65535" step="1" />
                <p class="ui-field__hint">Python 运行时监听的端口。</p>
                <p v-if="pythonPortError" class="ui-field__error">{{ pythonPortError }}</p>
              </div>
              <div class="ui-field field-grid__cell--wide">
                <label class="ui-label" for="internal-token">内部接口令牌 internalToken</label>
                <div class="input-with-action">
                  <input id="internal-token" v-model="config.internalToken" class="ui-input" :type="showToken ? 'text' : 'password'" placeholder="change-me" autocomplete="off" />
                  <button
                    type="button"
                    class="ui-icon-btn ui-icon-btn--outline"
                    :title="showToken ? '隐藏' : '显示'"
                    :aria-label="showToken ? '隐藏' : '显示'"
                    @click="showToken = !showToken"
                  >
                    <AppIcon :name="showToken ? 'eye-slash' : 'eye'" :size="16" />
                  </button>
                </div>
                <p class="ui-field__hint">Java 与 Python 两端必须一致，否则两端互相调用会失败。</p>
              </div>
            </div>
          </section>

          <!-- 区块四：后端进程状态 -->
          <section v-show="activeSection === 'process'" class="ui-panel block">
            <header class="ui-section-head">
              <div>
                <h2 class="ui-section-head__title">后端进程状态</h2>
                <p class="ui-section-head__hint">查看两个后端进程是否在运行，以及最近一次退出原因。</p>
              </div>
              <button class="ui-btn ui-btn--outline" :disabled="restarting" @click="restartBackends">
                <AppIcon :name="restarting ? 'spinner-gap' : 'arrows-clockwise'" :size="16" :spin="restarting" />
                {{ restarting ? '重启中…' : '重启后端' }}
              </button>
            </header>

            <hr class="ui-divider block__divider" />

            <ul class="process-list">
              <li v-for="item in processStates" :key="item.id" class="process-item">
                <div class="process-item__head">
                  <span class="process-item__name">
                    <AppIcon :name="processIcon(item.id)" :size="16" />
                    {{ processLabel(item.id) }}
                  </span>
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
        </template>
      </div>
    </div>

    <!-- 保存结果提示：成功走 accent，失败走 danger -->
    <p
      v-if="message"
      class="ui-callout"
      :class="messageType === 'success' ? 'ui-callout--accent' : 'ui-callout--danger'"
      role="status"
      aria-live="polite"
    >
      <AppIcon :name="messageType === 'success' ? 'check-circle' : 'warning-circle'" :size="16" />
      <span>{{ message }}</span>
    </p>

    <footer class="save-bar">
      <button class="ui-btn ui-btn--primary" :disabled="saving" @click="saveConfig">
        <AppIcon :name="saving ? 'spinner-gap' : 'check'" :size="16" :spin="saving" />
        {{ saving ? '保存中…' : '保存配置' }}
      </button>
    </footer>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue';
import AppIcon from '../components/common/AppIcon.vue';

// 常用兼容端点预设；API Key 始终由用户填写，模型和地址都允许覆盖。
const PROVIDER_PRESETS = {
  deepseek: { label: 'DeepSeek', modelProvider: 'deepseek', baseUrl: 'https://api.deepseek.com', model: 'deepseek-flash' },
  zhipu: { label: '智谱 GLM', modelProvider: 'openai', baseUrl: 'https://open.bigmodel.cn/api/paas/v4', model: 'glm-4.5' },
  minimax: { label: 'MiniMax', modelProvider: 'anthropic', baseUrl: 'https://api.minimaxi.com/anthropic', model: 'MiniMax-M3.1-Flash' },
  qwen: { label: '通义千问', modelProvider: 'openai', baseUrl: 'https://dashscope.aliyuncs.com/compatible-mode/v1', model: 'qwen-plus' },
  mimo: { label: 'MiMo', modelProvider: 'openai', baseUrl: 'https://api.xiaomimimo.com/v1', model: 'mimo-v2.5-pro' },
  openai: { label: 'OpenAI', modelProvider: 'openai', baseUrl: 'https://api.openai.com/v1', model: 'gpt-4.1-mini' },
  anthropic: { label: 'Anthropic', modelProvider: 'anthropic', baseUrl: 'https://api.anthropic.com', model: 'claude-sonnet-4-5-20250929' },
  openrouter: { label: 'OpenRouter', modelProvider: 'openrouter', baseUrl: 'https://openrouter.ai/api/v1', model: '' },
  ollama: { label: 'Ollama 本地', modelProvider: 'ollama', baseUrl: 'http://localhost:11434', model: '' },
  google_genai: { label: 'Google GenAI', modelProvider: 'google_genai', baseUrl: '', model: '' },
  xai: { label: 'xAI', modelProvider: 'xai', baseUrl: 'https://api.x.ai/v1', model: '' },
  mistralai: { label: 'Mistral AI', modelProvider: 'mistralai', baseUrl: 'https://api.mistral.ai/v1', model: '' },
  groq: { label: 'Groq', modelProvider: 'groq', baseUrl: 'https://api.groq.com/openai/v1', model: '' },
  together: { label: 'Together AI', modelProvider: 'together', baseUrl: 'https://api.together.xyz/v1', model: '' },
  huggingface: { label: 'Hugging Face Endpoint', modelProvider: 'huggingface', baseUrl: '', model: '' },
  bedrock: { label: 'Amazon Bedrock', modelProvider: 'bedrock', baseUrl: '', model: '' },
  litellm: { label: 'LiteLLM', modelProvider: 'litellm', baseUrl: 'http://localhost:4000', model: '' },
  custom: { label: '自定义', modelProvider: 'openai', baseUrl: '', model: '' },
};

const PROVIDER_OPTIONS = Object.entries(PROVIDER_PRESETS).map(([value, preset]) => ({ value, label: preset.label }));
const MODEL_PROVIDER_OPTIONS = [
  { value: 'openai', label: 'OpenAI 兼容' },
  { value: 'anthropic', label: 'Anthropic' },
  { value: 'deepseek', label: 'DeepSeek' },
  { value: 'google_genai', label: 'Google GenAI' },
  { value: 'openrouter', label: 'OpenRouter' },
  { value: 'ollama', label: 'Ollama 本地模型' },
  { value: 'xai', label: 'xAI' },
  { value: 'mistralai', label: 'Mistral AI' },
  { value: 'groq', label: 'Groq' },
  { value: 'together', label: 'Together AI' },
  { value: 'huggingface', label: 'Hugging Face' },
  { value: 'bedrock', label: 'Amazon Bedrock' },
  { value: 'litellm', label: 'LiteLLM Proxy' },
];

// 左侧目录只保留统一模型配置、服务地址和进程状态。
const SECTIONS = [
  { id: 'agent', title: 'AI 模型', icon: 'robot' },
  { id: 'server', title: '服务地址', icon: 'desktop' },
  { id: 'process', title: '后端进程', icon: 'pulse' },
];

// 浏览器调试模式下的本地存储键
const LOCAL_CONFIG_KEY = 'ai_resume_forge_app_config';

// 保存成功提示的自动收起时间（毫秒）
const MESSAGE_AUTO_HIDE_MS = 3500;

// 与主进程 defaultConfig() 保持一致，作为读不到配置时的兜底值
function createDefaultConfig() {
  return {
    agent: {
      provider: 'deepseek',
      modelProvider: 'deepseek',
      baseUrl: 'https://api.deepseek.com',
      apiKey: '',
      model: 'deepseek-flash',
      maxTokens: 4096,
      extraHeaders: {},
      providerOptions: {},
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
const showAgentKey = ref(false);
const extraHeadersText = ref('{}');
const providerOptionsText = ref('{}');
const showToken = ref(false);
const message = ref('');
const messageType = ref('');
const activeSection = ref('agent');
// 首次配置是否已读完，未读完时用骨架屏占位
const configLoaded = ref(false);

// 初始化期间不触发供应商自动填充，避免覆盖已保存的配置
const providerReady = ref(false);

let unsubscribeProcess = null;
let messageTimer = null;

const desktop = computed(() => (typeof window !== 'undefined' ? window.desktop : undefined));

const providerOptions = PROVIDER_OPTIONS;
const modelProviderOptions = MODEL_PROVIDER_OPTIONS;
const selectedProviderLabel = computed(() => PROVIDER_PRESETS[config.value.agent.provider]?.label || '自定义');

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

const agentMaxTokensError = computed(() => {
  const parsed = Number(config.value.agent.maxTokens);
  if (!Number.isFinite(parsed) || parsed <= 0) {
    return '最大输出 token 需为大于 0 的整数。';
  }
  return '';
});

const extraHeadersError = computed(() => {
  try {
    const headers = JSON.parse(extraHeadersText.value || '{}');
    if (!headers || typeof headers !== 'object' || Array.isArray(headers)) {
      return '额外请求头必须是 JSON 对象。';
    }
    if (Object.entries(headers).some(([name, value]) => !name.trim() || typeof value !== 'string')) {
      return '请求头名称不能为空，值必须是字符串。';
    }
    return '';
  } catch {
    return '额外请求头不是有效的 JSON。';
  }
});

const providerOptionsError = computed(() => {
  try {
    const options = JSON.parse(providerOptionsText.value || '{}');
    if (!options || typeof options !== 'object' || Array.isArray(options)) {
      return '集成参数必须是 JSON 对象。';
    }
    return '';
  } catch {
    return '集成参数不是有效的 JSON。';
  }
});

const serverPortError = computed(() => {
  const parsed = Number(config.value.server.port);
  if (!Number.isFinite(parsed) || parsed <= 0 || parsed > 65535) {
    return 'Java 后端 port 需为 1-65535 之间的整数。';
  }
  return '';
});

const pythonPortError = computed(() => {
  const parsed = Number(config.value.python.port);
  if (!Number.isFinite(parsed) || parsed <= 0 || parsed > 65535) {
    return 'Python 运行时 port 需为 1-65535 之间的整数。';
  }
  return '';
});

// 目录右侧的一行摘要，让每个分区当前配置一目了然
function sectionMeta(sectionId) {
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
    if (!preset || provider === 'custom') {
      // 自定义端点由用户自己填写，不覆盖协议和地址。
      return;
    }
    config.value.agent.modelProvider = preset.modelProvider;
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
  if (messageTimer) {
    clearTimeout(messageTimer);
    messageTimer = null;
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
    configLoaded.value = true;
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
  extraHeadersText.value = JSON.stringify(config.value.agent.extraHeaders || {}, null, 2);
  providerOptionsText.value = JSON.stringify(config.value.agent.providerOptions || {}, null, 2);
  providerReady.value = true;
}

function normalizeConfig(source) {
  const base = createDefaultConfig();
  if (!source || typeof source !== 'object') {
    return base;
  }
  let agent = { ...base.agent, ...(source.agent || {}) };
  if (!source.agent?.modelProvider) {
    agent.modelProvider = agent.provider === 'deepseek' ? 'deepseek' : 'openai';
  }
  const legacyChat = source.chat || {};
  if (!String(agent.apiKey || '').trim() && String(legacyChat.apiKey || '').trim()) {
    const baseUrl = String(legacyChat.baseUrl || '').trim();
    const modelProvider = /\/anthropic(?:\/|$)/i.test(baseUrl) ? 'anthropic' : 'openai';
    agent = {
      ...agent,
      provider: modelProvider === 'anthropic' && /minimaxi/i.test(baseUrl) ? 'minimax' : 'custom',
      modelProvider,
      baseUrl,
      apiKey: String(legacyChat.apiKey),
      model: String(legacyChat.model || agent.model),
      maxTokens: toPositiveInt(legacyChat.maxTokens, agent.maxTokens),
    };
  }
  if (!agent.extraHeaders || typeof agent.extraHeaders !== 'object' || Array.isArray(agent.extraHeaders)) {
    agent.extraHeaders = {};
  }
  if (!agent.providerOptions || typeof agent.providerOptions !== 'object' || Array.isArray(agent.providerOptions)) {
    agent.providerOptions = {};
  }
  return {
    agent,
    server: { ...base.server, ...(source.server || {}) },
    python: { ...base.python, ...(source.python || {}) },
    internalToken: source.internalToken ?? base.internalToken,
  };
}

function parseExtraHeaders() {
  if (extraHeadersError.value) {
    throw new Error(extraHeadersError.value);
  }
  return JSON.parse(extraHeadersText.value || '{}');
}

function parseProviderOptions() {
  if (providerOptionsError.value) {
    throw new Error(providerOptionsError.value);
  }
  return JSON.parse(providerOptionsText.value || '{}');
}

// 组装保存用的配置，端口与 token 数兜底成合法数字
function buildPayload() {
  const base = createDefaultConfig();
  return {
    agent: {
      provider: String(config.value.agent.provider || 'custom').trim(),
      modelProvider: String(config.value.agent.modelProvider || 'openai').trim(),
      baseUrl: String(config.value.agent.baseUrl || '').trim(),
      apiKey: String(config.value.agent.apiKey || '').trim(),
      model: String(config.value.agent.model || '').trim(),
      maxTokens: toPositiveInt(config.value.agent.maxTokens, base.agent.maxTokens),
      extraHeaders: parseExtraHeaders(),
      providerOptions: parseProviderOptions(),
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
  if (messageTimer) {
    clearTimeout(messageTimer);
    messageTimer = null;
  }
  if (type === 'success') {
    // 成功提示短暂停留后自动收起，失败提示保留到下一次操作
    messageTimer = setTimeout(() => {
      message.value = '';
      messageType.value = '';
      messageTimer = null;
    }, MESSAGE_AUTO_HIDE_MS);
  }
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

function processIcon(id) {
  return id === 'python' ? 'terminal-window' : 'desktop';
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
}

/* ---------- 页头 ---------- */
.page-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--ui-space-4);
}

.page-head__actions {
  display: flex;
  flex-shrink: 0;
  gap: var(--ui-space-2);
}

/* 标签与控件之间交给 .ui-field 的间距控制 */
.ui-label {
  margin-bottom: 0;
}

/* ---------- 环境提示 ---------- */
.notice--warning {
  background: var(--ui-warning-soft);
  border-color: var(--ui-warning-border);
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
  align-items: center;
  gap: var(--ui-space-2);
  width: 100%;
  padding: var(--ui-space-2) var(--ui-space-3);
  border: 1px solid transparent;
  border-radius: var(--ui-radius-md);
  background: transparent;
  color: var(--ui-fg-secondary);
  text-align: left;
  cursor: pointer;
  transition: background var(--ui-duration-fast) var(--ui-ease-out),
    color var(--ui-duration-fast) var(--ui-ease-out),
    border-color var(--ui-duration-fast) var(--ui-ease-out);
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

.section-nav__icon {
  display: inline-flex;
  color: var(--ui-fg-muted);
}

.section-nav__item.is-active .section-nav__icon {
  color: var(--ui-fg-accent);
}

.section-nav__body {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
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

/* ---------- 分区面板 ---------- */
.section-body {
  min-width: 0;
}

.block {
  padding: var(--ui-space-6);
}

/* 分区标题与表单之间用一道细线分开 */
.block .ui-section-head {
  margin-bottom: var(--ui-space-3);
}

.block__divider {
  margin: 0 0 var(--ui-space-5);
}

/* ---------- 骨架屏 ---------- */
.skeleton {
  display: flex;
  flex-direction: column;
  gap: var(--ui-space-4);
  padding: var(--ui-space-6);
  border: 1px solid var(--ui-border);
  border-radius: var(--ui-radius-lg);
  background: var(--ui-bg-surface);
}

.skeleton__title {
  width: 180px;
  height: 20px;
}

.skeleton__line {
  width: 60%;
  height: 14px;
}

.skeleton__field {
  height: 40px;
  border-radius: var(--ui-radius-md);
}

/* ---------- 表单 ---------- */
.field-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--ui-space-5) var(--ui-space-6);
}

/* 允许输入框在窄栏里收缩，避免长内容把网格撑破 */
.field-grid > .ui-field {
  min-width: 0;
}

.field-grid__cell--wide {
  grid-column: 1 / -1;
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

/* ---------- 进程状态 ---------- */
.process-list {
  display: flex;
  flex-direction: column;
  margin: 0;
  padding: 0;
  list-style: none;
}

/* 进程之间只留一条细线，不给每一行画框 */
.process-item {
  padding: var(--ui-space-4) 0;
}

.process-item + .process-item {
  border-top: 1px solid var(--ui-border-hairline);
}

.process-item__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--ui-space-3);
}

.process-item__name {
  display: inline-flex;
  align-items: center;
  gap: var(--ui-space-2);
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
  gap: var(--ui-space-3);
  padding: var(--ui-space-3) var(--ui-space-4);
  border: 1px solid var(--ui-border);
  border-radius: var(--ui-radius-lg);
  background: var(--ui-bg-surface);
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
