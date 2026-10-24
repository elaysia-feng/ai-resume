<template>
  <button
    v-if="!open"
    class="copilot-launcher"
    type="button"
    title="打开 AI 助手"
    aria-label="打开 AI 助手"
    @click="open = true"
  >
    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" aria-hidden="true">
      <path
        d="M12 3l1.9 4.6L18.5 9.5l-4.6 1.9L12 16l-1.9-4.6L5.5 9.5l4.6-1.9L12 3z"
        stroke="currentColor"
        stroke-width="1.6"
        stroke-linejoin="round"
      />
      <path d="M18 15.5l.8 1.9 1.9.8-1.9.8-.8 1.9-.8-1.9-1.9-.8 1.9-.8.8-1.9z" stroke="currentColor" stroke-width="1.4" stroke-linejoin="round" />
    </svg>
  </button>

  <section
    v-else
    class="floating-copilot"
    :class="{ minimized }"
    :style="panelStyle"
  >
    <header class="floating-copilot__header" @mousedown="startDrag">
      <div class="floating-copilot__heading">
        <strong class="floating-copilot__title">AI 助手</strong>
        <span class="floating-copilot__subtitle">{{ sectionTitle }}</span>
      </div>
      <div class="floating-copilot__tools">
        <button type="button" class="ui-btn ui-btn--sm ui-btn--ghost" @click.stop="minimized = !minimized">
          {{ minimized ? '展开' : '最小化' }}
        </button>
        <button type="button" class="ui-btn ui-btn--sm ui-btn--ghost" @click.stop="open = false">关闭</button>
      </div>
    </header>

    <div v-if="!minimized" class="floating-copilot__body">
      <div class="floating-copilot__row">
        <select v-model="sessionId" class="ui-input ui-select" aria-label="选择会话" @change="loadSessionDetail">
          <option :value="null">选择会话</option>
          <option v-for="session in sessions" :key="session.id" :value="session.id">
            {{ session.sessionTitle || `Session ${session.id}` }}
          </option>
        </select>
        <button type="button" class="ui-btn ui-btn--sm ui-btn--outline" @click="newSession">新建会话</button>
      </div>

      <div class="floating-copilot__field">
        <label class="ui-label" for="floating-copilot-jd">目标岗位 JD</label>
        <textarea
          id="floating-copilot-jd"
          v-model="jobDescription"
          class="ui-input"
          rows="4"
          placeholder="目标岗位 JD，会保存在当前 session"
        ></textarea>
      </div>

      <div class="floating-copilot__field">
        <label class="ui-label" for="floating-copilot-input">优化要求</label>
        <textarea
          id="floating-copilot-input"
          v-model="userInput"
          class="ui-input"
          rows="3"
          placeholder="说明希望 AI 如何优化当前模块"
        ></textarea>
      </div>

      <div class="floating-copilot__actions">
        <label class="switch">
          <input type="checkbox" class="switch__input" :checked="useMock" @change="toggleMock" />
          <span class="switch__track" aria-hidden="true"></span>
          <span class="switch__text">Mock</span>
        </label>
        <button
          type="button"
          class="ui-btn ui-btn--sm ui-btn--primary"
          :disabled="running || !canStart"
          @click="startRun"
        >
          {{ running ? '运行中…' : '优化当前模块' }}
        </button>
      </div>

      <p v-if="error" class="floating-copilot__error">{{ error }}</p>
      <p v-if="statusText" class="floating-copilot__status">{{ statusText }}</p>

      <div v-if="messages.length" class="floating-copilot__history">
        <strong class="floating-copilot__block-title">最近历史</strong>
        <p v-for="message in messages.slice(-4)" :key="message.id">
          <span class="floating-copilot__role">{{ message.role }}</span>{{ message.content }}
        </p>
      </div>

      <div v-if="events.length" class="floating-copilot__events">
        <p v-for="event in events" :key="`${event.runId}-${event.eventSeq}-${event.eventType}`">
          <span class="floating-copilot__stage">{{ event.stageCode || event.eventType }}</span>{{ event.message }}
        </p>
      </div>

      <div v-if="clarificationPayload?.questions?.length" class="floating-copilot__box">
        <strong class="floating-copilot__block-title">需要补充信息</strong>
        <label v-for="question in clarificationPayload.questions" :key="question.fieldKey" class="floating-copilot__question">
          <span class="ui-label">{{ question.question }}</span>
          <textarea v-model="clarificationAnswers[question.fieldKey]" class="ui-input" rows="2"></textarea>
        </label>
        <button type="button" class="ui-btn ui-btn--sm ui-btn--primary" :disabled="running" @click="continueRun">
          提交补充
        </button>
      </div>

      <div v-if="approvalPayload" class="floating-copilot__box">
        <strong class="floating-copilot__block-title">{{ approvalPayload.summary || '待确认修改' }}</strong>
        <div v-for="patch in approvalPayload.patches || []" :key="patch.patchId" class="floating-copilot__patch">
          <span class="floating-copilot__patch-title">{{ patch.sectionTitle || sectionTitle }}</span>
          <p class="floating-copilot__patch-summary">{{ patch.changeSummary || patch.reason }}</p>
          <pre class="floating-copilot__code">{{ formatJson(patch.afterJson) }}</pre>
        </div>
        <button type="button" class="ui-btn ui-btn--sm ui-btn--primary" :disabled="approving" @click="approveRun">
          {{ approving ? '应用中…' : '确认应用' }}
        </button>
      </div>
    </div>
  </section>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref, watch } from 'vue';
import {
  approveAgentRun,
  continueAgentRun,
  createAgentSession,
  getAgentSessionDetail,
  listAgentSessions,
  setAgentMockEnabled,
  shouldUseAgentMock,
  streamAgentRun,
  updateAgentSession,
} from '../../api/index.js';
import { extractApiMessage } from '../../api/request.js';

const props = defineProps({
  resumeId: { type: Number, default: null },
  section: { type: Object, default: null },
});

const emit = defineEmits(['approved']);

const open = ref(false);
const minimized = ref(false);
const position = ref({ x: window.innerWidth - 460, y: 92 });
const dragging = ref(false);
const dragOffset = ref({ x: 0, y: 0 });
const sessions = ref([]);
const sessionId = ref(null);
const messages = ref([]);
const running = ref(false);
const approving = ref(false);
const useMock = ref(shouldUseAgentMock());
const userInput = ref('');
const jobDescription = ref('');
const events = ref([]);
const approvalPayload = ref(null);
const clarificationPayload = ref(null);
const clarificationAnswers = ref({});
const error = ref('');
const statusText = ref('');

const sectionTitle = computed(() => props.section?.sectionTitle || '未选择模块');
const canStart = computed(() => Boolean(props.resumeId && props.section?.id));
const panelStyle = computed(() => ({
  transform: `translate(${Math.max(12, position.value.x)}px, ${Math.max(12, position.value.y)}px)`,
}));

watch(() => props.resumeId, refreshSessions, { immediate: true });
watch(() => props.section?.id, () => {
  approvalPayload.value = null;
  clarificationPayload.value = null;
  statusText.value = '';
});

onMounted(() => {
  window.addEventListener('mousemove', onDrag);
  window.addEventListener('mouseup', stopDrag);
});

onUnmounted(() => {
  window.removeEventListener('mousemove', onDrag);
  window.removeEventListener('mouseup', stopDrag);
});

async function refreshSessions() {
  if (!props.resumeId || useMock.value) return;
  sessions.value = await listAgentSessions({ resumeId: props.resumeId, sceneCode: 'JD_CUSTOMIZE' });
  if (!sessionId.value && sessions.value.length) {
    sessionId.value = sessions.value[0].id;
    await loadSessionDetail();
  }
}

async function loadSessionDetail() {
  if (!sessionId.value || useMock.value) return;
  const detail = await getAgentSessionDetail(sessionId.value);
  jobDescription.value = detail?.jobDescription || '';
  messages.value = detail?.messages || [];
}

async function newSession() {
  const sourceSessionId = sessionId.value;
  if (useMock.value) {
    sessionId.value = Date.now();
    messages.value = [];
    return;
  }
  const session = await createAgentSession({
    resumeId: props.resumeId,
    sceneCode: 'JD_CUSTOMIZE',
    sessionTitle: `${sectionTitle.value} Copilot`,
    copyFromSessionId: sourceSessionId,
    copyJobDescription: true,
    jobDescription: sourceSessionId ? null : jobDescription.value,
  });
  sessionId.value = session.id;
  await refreshSessions();
  await loadSessionDetail();
}

async function ensureSession() {
  if (sessionId.value) return sessionId.value;
  await newSession();
  return sessionId.value;
}

async function startRun() {
  if (!canStart.value) return;
  running.value = true;
  error.value = '';
  approvalPayload.value = null;
  clarificationPayload.value = null;
  events.value = [];
  try {
    const currentSessionId = await ensureSession();
    if (!useMock.value) {
      await updateAgentSession(currentSessionId, { jobDescription: jobDescription.value });
    }
    await streamAgentRun(currentSessionId, {
      resumeId: props.resumeId,
      sceneCode: 'JD_CUSTOMIZE',
      targetSectionId: props.section.id,
      userInput: userInput.value,
      jobDescription: jobDescription.value,
    }, {
      onQueued: () => {
        statusText.value = '已进入队列';
      },
      onEvent: handleEvent,
      onApproval: (payload) => {
        approvalPayload.value = payload;
        statusText.value = '等待确认应用';
      },
      onClarification: (payload) => {
        clarificationPayload.value = payload;
        statusText.value = '等待补充信息';
      },
    }, { mock: useMock.value });
  } catch (err) {
    error.value = extractApiMessage(err, 'Agent 运行失败');
  } finally {
    running.value = false;
  }
}

async function continueRun() {
  if (!clarificationPayload.value?.runId) return;
  running.value = true;
  try {
    const answers = Object.entries(clarificationAnswers.value)
      .filter(([, value]) => String(value || '').trim())
      .map(([fieldKey, value]) => ({ fieldKey, value }));
    await continueAgentRun(clarificationPayload.value.runId, { answers }, {
      onQueued: () => {
        statusText.value = '已重新进入队列';
      },
      onEvent: handleEvent,
      onApproval: (payload) => {
        approvalPayload.value = payload;
        statusText.value = '等待确认应用';
      },
      onClarification: (payload) => {
        clarificationPayload.value = payload;
        statusText.value = '等待补充信息';
      },
    }, { mock: useMock.value });
  } catch (err) {
    error.value = extractApiMessage(err, '继续运行失败');
  } finally {
    running.value = false;
  }
}

async function approveRun() {
  if (!approvalPayload.value?.runId) return;
  approving.value = true;
  try {
    const patches = approvalPayload.value.patches || [];
    const response = await approveAgentRun(
      approvalPayload.value.runId,
      {
        resumeId: props.resumeId,
        approvedPatchIds: patches.map((patch) => patch.patchId),
      },
      { mock: useMock.value },
    );
    statusText.value = '已确认应用';
    await loadSessionDetail();
    emit('approved', response);
  } catch (err) {
    error.value = extractApiMessage(err, '确认应用失败');
  } finally {
    approving.value = false;
  }
}

function handleEvent(event) {
  events.value.push(event);
}

function toggleMock(event) {
  useMock.value = event.target.checked;
  setAgentMockEnabled(useMock.value);
}

function startDrag(event) {
  if (window.innerWidth <= 720) return;
  dragging.value = true;
  dragOffset.value = { x: event.clientX - position.value.x, y: event.clientY - position.value.y };
}

function onDrag(event) {
  if (!dragging.value) return;
  position.value = {
    x: Math.min(window.innerWidth - 430, Math.max(12, event.clientX - dragOffset.value.x)),
    y: Math.min(window.innerHeight - 120, Math.max(12, event.clientY - dragOffset.value.y)),
  };
}

function stopDrag() {
  dragging.value = false;
}

function formatJson(value) {
  return JSON.stringify(value || {}, null, 2);
}
</script>

<style scoped>
/* ---------- 悬浮球 ---------- */
.copilot-launcher {
  position: fixed;
  right: var(--ui-space-6);
  bottom: var(--ui-space-6);
  z-index: 80;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 48px;
  height: 48px;
  padding: 0;
  border: 1px solid var(--ui-accent-border);
  border-radius: 50%;
  background: var(--ui-accent);
  color: var(--ui-accent-fg);
  cursor: pointer;
  box-shadow: var(--ui-shadow-lg);
  transition: transform var(--ui-duration-fast) var(--ui-ease-out),
    background var(--ui-duration-fast) var(--ui-ease-out),
    box-shadow var(--ui-duration-fast) var(--ui-ease-out);
}

.copilot-launcher:hover {
  transform: translateY(-2px);
  background: var(--ui-accent-hover);
  box-shadow: var(--ui-shadow-xl);
}

.copilot-launcher:active {
  transform: translateY(0);
  background: var(--ui-accent-active);
}

.copilot-launcher:focus-visible {
  outline: none;
  box-shadow: var(--ui-ring), var(--ui-shadow-lg);
}

/* ---------- 浮动面板 ---------- */
.floating-copilot {
  position: fixed;
  left: 0;
  top: 0;
  z-index: 80;
  display: flex;
  flex-direction: column;
  width: 420px;
  max-height: min(760px, calc(100vh - 24px));
  border: 1px solid var(--ui-border);
  border-radius: var(--ui-radius-lg);
  background: var(--ui-bg-surface);
  color: var(--ui-fg-primary);
  box-shadow: var(--ui-shadow-xl);
  overflow: hidden;
}

.floating-copilot.minimized {
  width: 320px;
}

.floating-copilot__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--ui-space-3);
  padding: var(--ui-space-3) var(--ui-space-4);
  border-bottom: 1px solid var(--ui-border);
  background: var(--ui-bg-sunken);
  cursor: move;
}

.floating-copilot__heading {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.floating-copilot__title {
  font-size: var(--ui-text-md);
  font-weight: 600;
  color: var(--ui-fg-primary);
}

.floating-copilot__subtitle {
  font-size: var(--ui-text-xs);
  color: var(--ui-fg-secondary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.floating-copilot__tools {
  display: flex;
  align-items: center;
  gap: var(--ui-space-1);
  flex-shrink: 0;
}

.floating-copilot__body {
  display: flex;
  flex-direction: column;
  gap: var(--ui-space-3);
  padding: var(--ui-space-4);
  min-height: 0;
  overflow-y: auto;
}

.floating-copilot__row {
  display: flex;
  align-items: center;
  gap: var(--ui-space-2);
}

.floating-copilot__row :deep(select) {
  flex: 1;
  min-width: 0;
}

.floating-copilot__field {
  display: flex;
  flex-direction: column;
}

.floating-copilot__field :deep(.ui-label) {
  margin-bottom: var(--ui-space-1);
}

.floating-copilot__field :deep(textarea) {
  resize: vertical;
  font-family: inherit;
}

.floating-copilot__actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--ui-space-2);
}

.floating-copilot__error {
  margin: 0;
  padding: var(--ui-space-2) var(--ui-space-3);
  border: 1px solid var(--ui-danger-border);
  border-radius: var(--ui-radius-md);
  background: var(--ui-danger-soft);
  color: var(--ui-danger);
  font-size: var(--ui-text-sm);
}

.floating-copilot__status {
  margin: 0;
  font-size: var(--ui-text-xs);
  color: var(--ui-fg-muted);
}

.floating-copilot__history,
.floating-copilot__events,
.floating-copilot__box {
  display: flex;
  flex-direction: column;
  gap: var(--ui-space-2);
  padding: var(--ui-space-3);
  border: 1px solid var(--ui-border);
  border-radius: var(--ui-radius-md);
  background: var(--ui-bg-sunken);
}

.floating-copilot__block-title {
  font-size: var(--ui-text-sm);
  font-weight: 600;
  color: var(--ui-fg-primary);
}

.floating-copilot__history p,
.floating-copilot__events p {
  margin: 0;
  font-size: var(--ui-text-xs);
  color: var(--ui-fg-secondary);
  word-break: break-word;
}

.floating-copilot__role {
  margin-right: 4px;
  color: var(--ui-fg-muted);
}

.floating-copilot__stage {
  display: inline-block;
  min-width: 88px;
  color: var(--ui-fg-accent);
  font-weight: 600;
  font-family: var(--ui-font-mono);
}

.floating-copilot__question {
  display: flex;
  flex-direction: column;
  gap: var(--ui-space-1);
}

.floating-copilot__question :deep(.ui-label) {
  margin-bottom: 0;
}

.floating-copilot__question :deep(textarea) {
  resize: vertical;
  font-family: inherit;
}

.floating-copilot__patch {
  display: flex;
  flex-direction: column;
  gap: var(--ui-space-2);
  padding: var(--ui-space-3);
  border: 1px solid var(--ui-border);
  border-radius: var(--ui-radius-md);
  background: var(--ui-bg-surface);
}

.floating-copilot__patch-title {
  font-size: var(--ui-text-xs);
  font-weight: 600;
  color: var(--ui-fg-secondary);
}

.floating-copilot__patch-summary {
  margin: 0;
  font-size: var(--ui-text-sm);
  color: var(--ui-fg-primary);
}

.floating-copilot__code {
  max-height: 180px;
  overflow: auto;
  margin: 0;
  padding: var(--ui-space-3);
  border-radius: var(--ui-radius-sm);
  background: var(--ui-bg-inverse);
  color: var(--ui-fg-inverse);
  font-family: var(--ui-font-mono);
  font-size: var(--ui-text-xs);
  line-height: 1.6;
}

/* ---------- 开关 ---------- */
.switch {
  display: inline-flex;
  align-items: center;
  gap: var(--ui-space-2);
  cursor: pointer;
}

.switch__input {
  position: absolute;
  opacity: 0;
  width: 0;
  height: 0;
}

.switch__track {
  position: relative;
  width: 34px;
  height: 18px;
  border-radius: var(--ui-radius-full);
  background: var(--ui-bg-active);
  transition: background var(--ui-duration-fast) var(--ui-ease-out);
}

.switch__track::after {
  content: '';
  position: absolute;
  top: 2px;
  left: 2px;
  width: 14px;
  height: 14px;
  border-radius: 50%;
  background: var(--ui-bg-surface);
  box-shadow: var(--ui-shadow-xs);
  transition: transform var(--ui-duration-fast) var(--ui-ease-out);
}

.switch__input:checked + .switch__track {
  background: var(--ui-accent);
}

.switch__input:checked + .switch__track::after {
  transform: translateX(16px);
}

.switch__input:focus-visible + .switch__track {
  box-shadow: var(--ui-ring);
}

.switch__text {
  font-size: var(--ui-text-xs);
  color: var(--ui-fg-secondary);
}

@media (max-width: 720px) {
  .floating-copilot {
    left: 0;
    right: 0;
    top: auto;
    bottom: 0;
    width: 100%;
    max-height: 82vh;
    border-radius: var(--ui-radius-lg) var(--ui-radius-lg) 0 0;
    transform: none !important;
  }
}
</style>
