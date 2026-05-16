<template>
  <button v-if="!open" class="copilot-launcher" type="button" @click="open = true">AI</button>
  <section
    v-else
    class="floating-copilot"
    :class="{ minimized }"
    :style="panelStyle"
  >
    <header class="floating-copilot__header" @mousedown="startDrag">
      <div>
        <strong>Agent Copilot</strong>
        <span>{{ sectionTitle }}</span>
      </div>
      <div class="floating-copilot__tools">
        <button type="button" @click.stop="minimized = !minimized">{{ minimized ? '展开' : '最小化' }}</button>
        <button type="button" @click.stop="open = false">关闭</button>
      </div>
    </header>

    <div v-if="!minimized" class="floating-copilot__body">
      <div class="floating-copilot__row">
        <select v-model="sessionId" @change="loadSessionDetail">
          <option :value="null">选择会话</option>
          <option v-for="session in sessions" :key="session.id" :value="session.id">
            {{ session.sessionTitle || `Session ${session.id}` }}
          </option>
        </select>
        <button type="button" @click="newSession">New Session</button>
      </div>

      <textarea v-model="jobDescription" rows="4" placeholder="目标岗位 JD，会保存在当前 session"></textarea>
      <textarea v-model="userInput" rows="3" placeholder="说明希望 AI 如何优化当前模块"></textarea>

      <div class="floating-copilot__actions">
        <label><input type="checkbox" :checked="useMock" @change="toggleMock" /> Mock</label>
        <button type="button" :disabled="running || !canStart" @click="startRun">
          {{ running ? '运行中...' : '优化当前模块' }}
        </button>
      </div>

      <p v-if="error" class="floating-copilot__error">{{ error }}</p>
      <p v-if="statusText" class="floating-copilot__status">{{ statusText }}</p>

      <div v-if="messages.length" class="floating-copilot__history">
        <strong>最近历史</strong>
        <p v-for="message in messages.slice(-4)" :key="message.id">
          {{ message.role }}：{{ message.content }}
        </p>
      </div>

      <div v-if="events.length" class="floating-copilot__events">
        <p v-for="event in events" :key="`${event.runId}-${event.eventSeq}-${event.eventType}`">
          <span>{{ event.stageCode || event.eventType }}</span>{{ event.message }}
        </p>
      </div>

      <div v-if="clarificationPayload?.questions?.length" class="floating-copilot__box">
        <strong>需要补充信息</strong>
        <label v-for="question in clarificationPayload.questions" :key="question.fieldKey">
          {{ question.question }}
          <textarea v-model="clarificationAnswers[question.fieldKey]" rows="2"></textarea>
        </label>
        <button type="button" :disabled="running" @click="continueRun">提交补充</button>
      </div>

      <div v-if="approvalPayload" class="floating-copilot__box">
        <strong>{{ approvalPayload.summary || '待确认修改' }}</strong>
        <div v-for="patch in approvalPayload.patches || []" :key="patch.patchId" class="floating-copilot__patch">
          <span>{{ patch.sectionTitle || sectionTitle }}</span>
          <p>{{ patch.changeSummary || patch.reason }}</p>
          <pre>{{ formatJson(patch.afterJson) }}</pre>
        </div>
        <button type="button" :disabled="approving" @click="approveRun">
          {{ approving ? '应用中...' : '确认应用' }}
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
    const response = await approveAgentRun(approvalPayload.value.runId, {
      resumeId: props.resumeId,
      approvedPatchIds: patches.map((patch) => patch.patchId),
    }, { mock: useMock.value });
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
.copilot-launcher {
  position: fixed;
  right: 24px;
  bottom: 24px;
  z-index: 80;
  width: 54px;
  height: 54px;
  border: 0;
  border-radius: 50%;
  background: #16a34a;
  color: #fff;
  font-weight: 800;
  box-shadow: 0 16px 36px rgba(22, 163, 74, 0.28);
  cursor: pointer;
}

.floating-copilot {
  position: fixed;
  left: 0;
  top: 0;
  z-index: 80;
  width: 420px;
  max-height: min(760px, calc(100vh - 24px));
  border: 1px solid #d1fae5;
  border-radius: 10px;
  background: #ffffff;
  box-shadow: 0 20px 54px rgba(15, 23, 42, 0.22);
  overflow: hidden;
}

.floating-copilot.minimized {
  width: 320px;
}

.floating-copilot__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 14px;
  background: #f0fdf4;
  cursor: move;
}

.floating-copilot__header div:first-child {
  display: grid;
  gap: 2px;
}

.floating-copilot__header span,
.floating-copilot__status,
.floating-copilot__history p,
.floating-copilot__events p {
  color: #64748b;
  font-size: 12px;
}

.floating-copilot__tools,
.floating-copilot__row,
.floating-copilot__actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.floating-copilot__body {
  display: grid;
  gap: 10px;
  padding: 12px;
  max-height: calc(100vh - 120px);
  overflow: auto;
}

.floating-copilot textarea,
.floating-copilot select {
  width: 100%;
  box-sizing: border-box;
  border: 1px solid #cbd5e1;
  border-radius: 7px;
  padding: 8px;
  font: inherit;
}

.floating-copilot button {
  border: 0;
  border-radius: 7px;
  padding: 7px 10px;
  background: #16a34a;
  color: #fff;
  font-weight: 700;
  cursor: pointer;
  white-space: nowrap;
}

.floating-copilot button:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

.floating-copilot__tools button {
  background: #dcfce7;
  color: #166534;
}

.floating-copilot__error {
  margin: 0;
  color: #b91c1c;
  font-size: 13px;
}

.floating-copilot__history,
.floating-copilot__events,
.floating-copilot__box,
.floating-copilot__patch {
  display: grid;
  gap: 7px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 9px;
  background: #f8fafc;
}

.floating-copilot__events span {
  display: inline-block;
  min-width: 96px;
  color: #16a34a;
  font-weight: 700;
}

.floating-copilot__box label {
  display: grid;
  gap: 5px;
  color: #334155;
  font-size: 13px;
}

.floating-copilot__patch pre {
  max-height: 180px;
  overflow: auto;
  margin: 0;
  padding: 8px;
  border-radius: 6px;
  background: #0f172a;
  color: #e2e8f0;
  font-size: 12px;
}

@media (max-width: 720px) {
  .floating-copilot {
    left: 0;
    right: 0;
    top: auto;
    bottom: 0;
    width: 100%;
    max-height: 82vh;
    border-radius: 14px 14px 0 0;
    transform: none !important;
  }
}
</style>
