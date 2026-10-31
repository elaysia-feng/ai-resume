<template>
  <div class="chat-page">
    <ChatSidebar
      :sessions="sessions"
      :current-session-id="currentSessionId"
      :workspace-path="workspacePath"
      :loading="loadingSessions"
      :can-choose-directory="canChooseDirectory"
      @select="selectSession"
      @create="createSession"
      @remove="deleteSession"
      @choose-workspace="chooseWorkspace"
      @confirm-workspace="saveWorkspace"
    />

    <section class="chat-main">
      <header class="chat-topbar">
        <div class="chat-topbar-info">
          <h2 class="ui-page-title chat-topbar-title" :title="currentSessionTitle">{{ currentSessionTitle }}</h2>
          <p class="ui-page-subtitle chat-topbar-subtitle">{{ messageCountLabel }}</p>
        </div>
        <div class="chat-topbar-actions">
          <span v-if="streaming" class="ui-badge ui-badge--accent">
            <AppIcon name="spinner-gap" :size="12" spin />
            生成中
          </span>
          <button class="ui-btn ui-btn--ghost ui-btn--sm" @click="goBack">
            <AppIcon name="arrow-left" :size="16" />
            返回工作台
          </button>
        </div>
      </header>

      <ChatMessageList
        :messages="messages"
        :loading="loadingMessages"
        :error="errorMessage"
        @use-example="useExample"
      />

      <ChatComposer
        ref="composerRef"
        v-model="composerText"
        :disabled="streaming"
        :streaming="streaming"
        @send="sendMessage"
        @stop="stopStreaming"
      />
    </section>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import ChatSidebar from '../components/chat/ChatSidebar.vue';
import ChatMessageList from '../components/chat/ChatMessageList.vue';
import ChatComposer from '../components/chat/ChatComposer.vue';
import AppIcon from '../components/common/AppIcon.vue';
import {
  createChatSession,
  deleteChatSession,
  getChatWorkspace,
  listChatMessages,
  listChatSessions,
  setChatWorkspace,
  streamChatMessage,
} from '../api/chat.js';
import { extractApiMessage } from '../api/request.js';
import '../styles/chat.css';

const router = useRouter();
const route = useRoute();

const sessions = ref([]);
const currentSessionId = ref(null);
const messages = ref([]);
const composerText = ref('');
const workspacePath = ref('');
const errorMessage = ref('');
const loadingSessions = ref(false);
const loadingMessages = ref(false);
const streaming = ref(false);

const composerRef = ref(null);
let abortController = null;

// Electron 环境才有原生目录选择框
const canChooseDirectory = computed(
  () => typeof window !== 'undefined' && typeof window.desktop?.chooseDirectory === 'function'
);

const currentSessionTitle = computed(() => {
  const session = sessions.value.find((item) => item.id === currentSessionId.value);
  if (session?.title) {
    return session.title;
  }
  return currentSessionId.value ? `会话 #${currentSessionId.value}` : '新对话';
});

// 顶栏副信息：当前会话的消息条数
const messageCountLabel = computed(() =>
  messages.value.length ? `${messages.value.length} 条消息` : '还没有消息'
);

onMounted(async () => {
  await loadSessions();
  await selectRequestedOrRecentSession();
  await loadWorkspace();
});

watch(() => route.query.sessionId, async () => {
  if (!sessions.value.length) {
    await loadSessions();
  }
  await selectRequestedOrRecentSession();
});

onBeforeUnmount(() => {
  abortController?.abort();
});

async function loadSessions(options = {}) {
  if (!options.silent) {
    loadingSessions.value = true;
  }
  try {
    const list = await listChatSessions();
    sessions.value = Array.isArray(list) ? list : [];
  } catch (error) {
    errorMessage.value = `加载会话列表失败：${describeError(error)}`;
  } finally {
    if (!options.silent) {
      loadingSessions.value = false;
    }
  }
}

async function selectRequestedOrRecentSession() {
  const requestedId = route.query.sessionId;
  if (requestedId) {
    const requestedSession = sessions.value.find((session) => String(session.id) === String(requestedId));
    if (!requestedSession) {
      currentSessionId.value = null;
      messages.value = [];
      errorMessage.value = '找不到这条对话记录。它可能已删除，请从工作台重新选择。';
      return;
    }
    if (String(currentSessionId.value) !== String(requestedSession.id)) {
      await selectSession(requestedSession.id);
    }
    return;
  }

  // 未指定记录时回到最近会话；首次进入没有会话则等待用户发起对话。
  if (!currentSessionId.value && sessions.value.length > 0) {
    await selectSession(sessions.value[0].id);
  }
}

async function selectSession(sessionId) {
  if (streaming.value) {
    return;
  }
  currentSessionId.value = sessionId;
  if (String(route.query.sessionId || '') !== String(sessionId)) {
    router.replace({ query: { ...route.query, sessionId: String(sessionId) } });
  }
  messages.value = [];
  errorMessage.value = '';
  loadingMessages.value = true;
  try {
    const list = await listChatMessages(sessionId);
    messages.value = (Array.isArray(list) ? list : []).map(normalizeMessage);
  } catch (error) {
    errorMessage.value = `加载消息失败：${describeError(error)}`;
  } finally {
    loadingMessages.value = false;
  }
}

async function createSession() {
  if (streaming.value) {
    return null;
  }
  try {
    const data = await createChatSession({});
    const sessionId = data?.sessionId;
    if (!sessionId) {
      errorMessage.value = '创建会话失败：后端未返回 sessionId';
      return null;
    }
    sessions.value = [{ id: sessionId, title: '新会话', messageCount: 0 }, ...sessions.value];
    currentSessionId.value = sessionId;
    messages.value = [];
    errorMessage.value = '';
    router.replace({ query: { ...route.query, sessionId: String(sessionId) } });
    return sessionId;
  } catch (error) {
    errorMessage.value = `创建会话失败：${describeError(error)}`;
    return null;
  }
}

async function deleteSession(sessionId) {
  try {
    await deleteChatSession(sessionId);
    const removedCurrentSession = String(currentSessionId.value) === String(sessionId);
    if (removedCurrentSession) {
      currentSessionId.value = null;
      messages.value = [];
      await router.replace({ query: { ...route.query, sessionId: undefined } });
    }
    await loadSessions();
    if (removedCurrentSession && sessions.value.length) {
      await selectSession(sessions.value[0].id);
    }
  } catch (error) {
    errorMessage.value = `删除会话失败：${describeError(error)}`;
  }
}

async function sendMessage() {
  const content = composerText.value.trim();
  if (!content || streaming.value) {
    return;
  }

  errorMessage.value = '';
  composerText.value = '';

  // 会话不存在（例如首次进入）时先自动创建一个
  if (!currentSessionId.value) {
    const created = await createSession();
    if (!created) {
      composerText.value = content;
      return;
    }
  }

  messages.value.push({
    id: `local-user-${Date.now()}`,
    role: 'user',
    content,
    createdAt: new Date().toISOString(),
    tools: [],
  });

  // 用 reactive 保证流式追加文本时能触发视图更新
  const assistantMessage = reactive({
    id: `local-assistant-${Date.now()}`,
    role: 'assistant',
    content: '',
    createdAt: new Date().toISOString(),
    streaming: true,
    tools: [],
  });
  messages.value.push(assistantMessage);

  streaming.value = true;
  abortController = new AbortController();

  try {
    await streamChatMessage(
      currentSessionId.value,
      { content, workspacePath: workspacePath.value || undefined },
      {
        onTextDelta: (data) => {
          assistantMessage.content += data?.text ?? '';
        },
        onToolCall: (data) => {
          assistantMessage.tools.push({
            id: data?.id ?? `tool-${Date.now()}`,
            name: data?.name,
            input: data?.input,
            status: 'running',
            summary: '',
          });
        },
        onToolResult: (data) => {
          const tool =
            assistantMessage.tools.find((item) => item.id === data?.id) ||
            assistantMessage.tools[assistantMessage.tools.length - 1];
          if (tool) {
            tool.status = data?.ok === false ? 'failed' : 'success';
            tool.summary = data?.summary ?? '';
          }
        },
        onDone: (data) => {
          if (data?.messageId) {
            assistantMessage.id = data.messageId;
          }
        },
        onError: (data) => {
          errorMessage.value = data?.message || '对话出错';
        },
      },
      { signal: abortController.signal }
    );
  } catch (error) {
    if (error?.name === 'AbortError') {
      // 用户主动停止，保留已经生成的内容
      assistantMessage.content = assistantMessage.content || '（已停止生成）';
    } else {
      errorMessage.value = `对话失败：${describeError(error)}`;
    }
  } finally {
    assistantMessage.streaming = false;
    streaming.value = false;
    abortController = null;
    // 会话标题与消息数可能已更新，静默刷新
    loadSessions({ silent: true });
  }
}

function stopStreaming() {
  abortController?.abort();
}

async function loadWorkspace() {
  try {
    const data = await getChatWorkspace();
    workspacePath.value = data?.path || '';
  } catch {
    // 工作区只是附加信息，取不到时保持为空即可
    workspacePath.value = '';
  }
}

async function chooseWorkspace() {
  errorMessage.value = '';
  if (!canChooseDirectory.value) {
    errorMessage.value = '当前环境无法打开系统目录选择框，请在左下方手动填写工作区绝对路径';
    return;
  }
  try {
    const result = await window.desktop.chooseDirectory();
    if (!result?.path) {
      return;
    }
    await saveWorkspace(result.path);
  } catch (error) {
    errorMessage.value = `选择目录失败：${describeError(error)}`;
  }
}

async function saveWorkspace(path) {
  const trimmed = (path || '').trim();
  if (!trimmed) {
    errorMessage.value = '工作区路径不能为空';
    return;
  }
  try {
    const data = await setChatWorkspace(trimmed);
    workspacePath.value = data?.path ?? trimmed;
    errorMessage.value = '';
  } catch (error) {
    errorMessage.value = `设置工作区失败：${describeError(error)}`;
  }
}

function useExample(text) {
  composerText.value = text;
  composerRef.value?.focus();
}

function goBack() {
  router.push('/dashboard');
}

// 把后端消息转成界面用的结构
function normalizeMessage(raw) {
  const role = String(raw?.role || '').toUpperCase();
  const rawContent = raw?.content ?? '';
  const storedContent = parseStoredMessageContent(raw);
  const message = {
    id: raw?.id ?? `local-${Math.random().toString(36).slice(2)}`,
    role: role === 'USER' ? 'user' : role === 'TOOL' ? 'tool' : role === 'SYSTEM' ? 'system' : 'assistant',
    content: rawContent,
    toolName: raw?.toolName ?? '',
    createdAt: raw?.createdAt ?? '',
    streaming: false,
    tools: [],
  };

  if (message.role === 'tool') {
    const historyResult = isWorkspaceToolResult(storedContent)
      ? storedContent
      : findLegacyToolResult(storedContent);
    message.tools = historyResult
      ? [{
          id: historyResult.id || `tool-${message.id}`,
          name: historyResult.name || message.toolName || 'tool',
          status: historyResult.ok === false || historyResult.is_error ? 'failed' : 'success',
          summary: storedContentToText(historyResult.summary ?? historyResult.content ?? message.content),
        }]
      : [{ id: `tool-${message.id}`, name: message.toolName || 'tool', status: 'success', summary: message.content }];
    message.content = '';
  } else if (message.role === 'assistant' && isWorkspaceToolCalls(storedContent)) {
    message.content = storedContent.text || '';
    message.tools = (storedContent.calls || []).map((call, index) => ({
      id: call.id || `tool-${message.id}-${index}`,
      name: call.name || 'tool',
      input: call.input || {},
      status: 'success',
      summary: '工具调用',
    }));
  } else if (message.role === 'assistant' && Array.isArray(storedContent)) {
    const text = storedContent
      .filter((block) => block?.type === 'text')
      .map((block) => block.text || '')
      .join('');
    const calls = storedContent.filter((block) => block?.type === 'tool_use');
    if (calls.length) {
      message.content = text;
      message.tools = calls.map((call, index) => ({
        id: call.id || `tool-${message.id}-${index}`,
        name: call.name || 'tool',
        input: call.input || {},
        status: 'success',
        summary: '工具调用',
      }));
    }
  } else if (message.role === 'assistant' && raw?.toolName) {
    message.tools = [
      { id: `tool-${message.id}`, name: raw.toolName, status: 'success', summary: message.content },
    ];
  }

  return message;
}

function parseStoredMessageContent(raw) {
  if (String(raw?.contentType || '').toUpperCase() !== 'JSON' || typeof raw?.content !== 'string') {
    return null;
  }
  try {
    return JSON.parse(raw.content);
  } catch {
    return null;
  }
}

function isWorkspaceToolCalls(value) {
  return value && typeof value === 'object' && value.type === 'workspace_tool_calls';
}

function isWorkspaceToolResult(value) {
  return value && typeof value === 'object' && value.type === 'workspace_tool_result';
}

function findLegacyToolResult(value) {
  if (!Array.isArray(value)) {
    return null;
  }
  return value.find((block) => block?.type === 'tool_result') || null;
}

function storedContentToText(value) {
  if (typeof value === 'string') {
    return value;
  }
  if (Array.isArray(value)) {
    return value
      .map((part) => (typeof part === 'string' ? part : part?.text || ''))
      .join('');
  }
  if (value == null) {
    return '';
  }
  return JSON.stringify(value);
}

// 后端没起来时给出可读提示，而不是抛原始网络错误
function describeError(error) {
  const message = extractApiMessage(error, '请求失败');
  if (/network error|failed to fetch|load failed/i.test(message)) {
    return '无法连接后端服务（默认 http://localhost:8080），请确认后端已启动';
  }
  return message;
}
</script>

<style scoped>
.chat-page {
  display: flex;
  /* 铺满应用壳的内容区（高度由壳给出），消息流自己滚动、输入区固定在底部 */
  height: 100%;
  min-height: calc(100vh - var(--ui-header-height) - var(--ui-space-8) * 2);
  min-width: 0;
  overflow: hidden;
  background: var(--ui-bg-canvas);
}

.chat-main {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-width: 0;
  height: 100%;
}

/* 上下文栏：只显示当前会话与状态，不重复全局导航 */
.chat-topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--ui-space-4);
  flex: none;
  height: var(--ui-header-height);
  padding: 0 var(--ui-space-6);
  background: var(--ui-bg-surface);
  background-image: var(--ui-surface-wash);
  border-bottom: 1px solid var(--ui-border-hairline);
}

/* 标题与副标题同一行靠左排，栏高才能压住 */
.chat-topbar-info {
  display: flex;
  align-items: baseline;
  gap: var(--ui-space-3);
  min-width: 0;
}

.chat-topbar-title {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.chat-topbar-subtitle {
  margin: 0;
  font-size: var(--ui-text-sm);
  color: var(--ui-fg-muted);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.chat-topbar-actions {
  display: flex;
  align-items: center;
  gap: var(--ui-space-2);
  flex: none;
}

@media (max-width: 768px) {
  .chat-topbar {
    padding: 0 var(--ui-space-4);
  }
}

/* 窄窗口下壳层内容区外边距变小，这里同步 */
@media (max-width: 1100px) {
  .chat-page {
    min-height: calc(100vh - var(--ui-header-height) - var(--ui-space-6) * 2);
  }
}
</style>
