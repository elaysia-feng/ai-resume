<template>
  <button
    v-if="!open"
    class="copilot-launcher"
    type="button"
    title="打开模块 AI 助手"
    aria-label="打开模块 AI 助手"
    @click="open = true"
  >
    <AppIcon name="sparkle" :size="22" />
  </button>

  <section
    v-else
    class="floating-copilot"
    :class="{ minimized }"
    :style="panelStyle"
  >
    <header class="floating-copilot__header" @mousedown="startDrag">
      <div class="floating-copilot__heading">
          <strong class="floating-copilot__title">模块 AI 助手</strong>
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
        <select v-model="sessionId" class="ui-input ui-select" :aria-label="`选择${sectionTitle}的 AI 会话`" @change="onSessionChange">
          <option :value="null">选择当前分区会话</option>
          <optgroup v-if="sessions.length" :label="`${sectionTitle} 会话`">
            <option v-for="session in sessions" :key="session.id" :value="session.id">
              {{ session.sessionTitle || `会话 #${session.id}` }}{{ session.activeRunId ? ' · 处理中' : '' }}
            </option>
          </optgroup>
          <optgroup v-if="legacySessions.length" label="旧版共享会话（只读）">
            <option v-for="session in legacySessions" :key="`legacy-${session.id}`" :value="session.id">
              {{ session.sessionTitle || `会话 #${session.id}` }}
            </option>
          </optgroup>
        </select>
        <button
          type="button"
          class="ui-btn ui-btn--sm ui-btn--outline"
          :disabled="!canCreateSession || creatingSession"
          @click="newSession"
        >
          {{ creatingSession ? '创建中…' : '新建会话' }}
        </button>
      </div>

      <p v-if="isLegacySession" class="floating-copilot__legacy-note" role="note">
        这是旧版共享会话，只能查看历史。新建会话后，{{ sectionTitle }} 的上下文会单独保存。
      </p>
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
        <label v-if="isDevelopment" class="switch">
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

      <p v-if="error" class="ui-callout ui-callout--danger floating-copilot__error">
        <AppIcon name="warning-circle" :size="16" />
        <span>{{ error }}</span>
      </p>
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
        <p class="floating-copilot__review-hint">逐条查看差异，只勾选你确认要应用的内容。</p>
        <div class="floating-copilot__selection-actions">
          <button type="button" class="ui-btn ui-btn--ghost ui-btn--sm" @click="selectAllPatches">全部勾选</button>
          <button type="button" class="ui-btn ui-btn--ghost ui-btn--sm" @click="clearSelectedPatches">清空勾选</button>
        </div>
        <label v-for="patch in approvalPayload.patches || []" :key="patch.patchId" class="floating-copilot__patch">
          <span class="floating-copilot__patch-select">
            <input v-model="selectedPatchIds" type="checkbox" :value="patch.patchId" />
            <span class="floating-copilot__patch-title">{{ patch.sectionTitle || sectionTitle }}</span>
            <span class="ui-badge" :class="riskBadgeClass(patch.riskLevel)">{{ riskLevelLabel(patch.riskLevel) }}</span>
          </span>
          <span class="floating-copilot__patch-summary">{{ patch.changeSummary || patch.reason || 'AI 提供了内容调整建议。' }}</span>
          <details class="floating-copilot__diff">
            <summary>查看修改前后</summary>
            <span class="floating-copilot__diff-grid">
              <span class="floating-copilot__diff-side">
                <strong>修改前</strong>
                <pre class="floating-copilot__code">{{ formatJson(patch.beforeJson) }}</pre>
              </span>
              <span class="floating-copilot__diff-side">
                <strong>建议内容</strong>
                <pre class="floating-copilot__code">{{ formatJson(patch.afterJson) }}</pre>
              </span>
            </span>
          </details>
        </label>
        <button type="button" class="ui-btn ui-btn--sm ui-btn--primary" :disabled="approving || !selectedPatchIds.length" @click="approveRun">
          {{ approving ? '应用中…' : '确认应用' }}
        </button>
        <p class="floating-copilot__review-hint">{{ selectedPatchIds.length }} 项将写入简历；未勾选的内容会保留原样。</p>
      </div>
    </div>
  </section>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref, watch } from 'vue';
import AppIcon from '../common/AppIcon.vue';
import {
  approveAgentRun,
  continueAgentRun,
  createClientRequestId,
  createAgentSession,
  getAgentRun,
  getAgentRunEvents,
  getAgentSessionDetail,
  listAgentSessions,
  setAgentMockEnabled,
  shouldUseAgentMock,
  streamAgentRun,
  streamAgentRunEvents,
  updateAgentSession,
} from '../../api/index.js';
import { extractApiMessage } from '../../api/request.js';

const props = defineProps({
  resumeId: { type: Number, default: null },
  section: { type: Object, default: null },
  sessionId: { type: [Number, String], default: null },
  beforeApprove: { type: Function, default: null },
});

const isDevelopment = import.meta.env.DEV;

const emit = defineEmits(['approved']);

const open = ref(false);
const minimized = ref(false);
const position = ref({ x: window.innerWidth - 460, y: 92 });
const dragging = ref(false);
const dragOffset = ref({ x: 0, y: 0 });
const sessions = ref([]);
const legacySessions = ref([]);
const sessionId = ref(null);
const messages = ref([]);
const running = ref(false);
const approving = ref(false);
const creatingSession = ref(false);
const useMock = ref(shouldUseAgentMock());
const userInput = ref('');
const jobDescription = ref('');
const events = ref([]);
const approvalPayload = ref(null);
const clarificationPayload = ref(null);
const clarificationAnswers = ref({});
const selectedPatchIds = ref([]);
const error = ref('');
const statusText = ref('');
const runStreamController = ref(null);
let runStreamGeneration = 0;
let lastResolvedResumeId = null;
let lastRequestedSessionId = null;
const pendingStartRequestIds = new Map();

const sectionTitle = computed(() => props.section?.sectionTitle || '未选择模块');
const currentSession = computed(() =>
  [...sessions.value, ...legacySessions.value].find((session) => String(session.id) === String(sessionId.value)) || null
);
const isLegacySession = computed(() => Boolean(currentSession.value && !currentSession.value.targetSectionId));
const canCreateSession = computed(() => Boolean(props.resumeId && props.section?.id));
const canStart = computed(() => canCreateSession.value && !isLegacySession.value);
const panelStyle = computed(() => ({
  transform: `translate(${Math.max(12, position.value.x)}px, ${Math.max(12, position.value.y)}px)`,
}));

watch(() => [props.resumeId, props.section?.id, props.sessionId], ([resumeId, sectionId, requestedSessionId]) => {
  if (!resumeId || !sectionId) {
    return;
  }
  const resumeChanged = String(resumeId) !== String(lastResolvedResumeId);
  const requestedSessionChanged = requestedSessionId
    && String(requestedSessionId) !== String(lastRequestedSessionId);
  const preferredSessionId = resumeChanged || requestedSessionChanged ? requestedSessionId : null;
  lastResolvedResumeId = resumeId;
  lastRequestedSessionId = requestedSessionId;
  runStreamController.value?.abort();
  runStreamController.value = null;
  runStreamGeneration += 1;
  sessionId.value = null;
  sessions.value = [];
  legacySessions.value = [];
  jobDescription.value = '';
  messages.value = [];
  events.value = [];
  approvalPayload.value = null;
  clarificationPayload.value = null;
  selectedPatchIds.value = [];
  running.value = false;
  error.value = '';
  statusText.value = '';
  refreshSessions(preferredSessionId);
}, { immediate: true });

onMounted(() => {
  window.addEventListener('mousemove', onDrag);
  window.addEventListener('mouseup', stopDrag);
});

onUnmounted(() => {
  window.removeEventListener('mousemove', onDrag);
  window.removeEventListener('mouseup', stopDrag);
  runStreamController.value?.abort();
});

async function refreshSessions(preferredSessionId = null) {
  if (!props.resumeId || !props.section?.id) {
    return;
  }
  if (useMock.value) {
    if (preferredSessionId) {
      sessionId.value = preferredSessionId;
      open.value = true;
    }
    return;
  }
  try {
    const [moduleSessions, unboundSessions] = await Promise.all([
      listAgentSessions({
        resumeId: props.resumeId,
        sceneCode: 'JD_CUSTOMIZE',
        targetSectionId: Number(props.section.id),
      }),
      listAgentSessions({ resumeId: props.resumeId, sceneCode: 'JD_CUSTOMIZE', targetSectionId: 0 }),
    ]);
    sessions.value = Array.isArray(moduleSessions) ? moduleSessions : [];
    legacySessions.value = Array.isArray(unboundSessions) ? unboundSessions : [];
    if (preferredSessionId) {
      const requestedSession = [...sessions.value, ...legacySessions.value]
        .find((session) => String(session.id) === String(preferredSessionId));
      if (!requestedSession) {
        sessionId.value = null;
        jobDescription.value = '';
        messages.value = [];
        approvalPayload.value = null;
        clarificationPayload.value = null;
        error.value = '找不到这条会话，或它属于简历的其他分区。请从对应分区的历史重新打开。';
        return;
      }
      sessionId.value = requestedSession.id;
      open.value = true;
      await loadSessionDetail();
      return;
    }
    const selectedSession = [...sessions.value].find((session) => Boolean(session.activeRunId)) || sessions.value[0];
    if (selectedSession) {
      sessionId.value = selectedSession.id;
      await loadSessionDetail();
      return;
    }
    sessionId.value = null;
    messages.value = [];
    jobDescription.value = '';
    approvalPayload.value = null;
    clarificationPayload.value = null;
    selectedPatchIds.value = [];
    events.value = [];
  } catch (err) {
    error.value = extractApiMessage(err, '加载简历 AI 会话失败');
  }
}

async function loadSessionDetail() {
  if (!sessionId.value || useMock.value) return;
  const sessionIdAtStart = sessionId.value;
  const generation = ++runStreamGeneration;
  error.value = '';
  statusText.value = '';
  try {
    const detail = await getAgentSessionDetail(sessionIdAtStart);
    if (!isCurrentSession(sessionIdAtStart, generation)) {
      return;
    }
    const session = sessions.value.find((item) => String(item.id) === String(sessionIdAtStart));
    jobDescription.value = detail?.jobDescription || '';
    messages.value = detail?.messages || [];
    approvalPayload.value = null;
    clarificationPayload.value = null;
    selectedPatchIds.value = [];
    events.value = [];
    if (session?.activeRunId) {
      await restoreRun(session.activeRunId, sessionIdAtStart, generation);
    }
  } catch (err) {
    if (isCurrentSession(sessionIdAtStart, generation)) {
      error.value = extractApiMessage(err, '加载会话详情失败');
    }
  }
}

function onSessionChange() {
  runStreamController.value?.abort();
  runStreamController.value = null;
  runStreamGeneration += 1;
  running.value = false;
  error.value = '';
  statusText.value = '';
  messages.value = [];
  events.value = [];
  approvalPayload.value = null;
  clarificationPayload.value = null;
  selectedPatchIds.value = [];
  if (!sessionId.value) {
    jobDescription.value = '';
    return;
  }
  loadSessionDetail();
}

async function restoreRun(runId, sessionIdAtStart, generation) {
  try {
    const [run, eventList] = await Promise.all([
      getAgentRun(runId),
      getAgentRunEvents(runId, { page: 1, pageSize: 200 }),
    ]);
    if (!isCurrentSession(sessionIdAtStart, generation)) {
      return;
    }
    events.value = Array.isArray(eventList) ? eventList : [];
    approvalPayload.value = run?.approvalPayload || null;
    clarificationPayload.value = run?.clarificationPayload || null;
    selectedPatchIds.value = [];
    statusText.value = runStatusText(run?.status);
    if (['QUEUED', 'RUNNING'].includes(run?.status)) {
      running.value = true;
      const afterSeq = Math.max(0, ...events.value.map((event) => Number(event.eventSeq) || 0));
      const controller = new AbortController();
      runStreamController.value = controller;
      streamAgentRunEvents(runId, createRunHandlers(sessionIdAtStart, generation), {
        afterSeq,
        signal: controller.signal,
      })
        .catch((err) => {
          if (isCurrentSession(sessionIdAtStart, generation)) {
            error.value = extractApiMessage(err, '恢复运行状态失败');
          }
        })
        .finally(() => {
          if (isCurrentSession(sessionIdAtStart, generation)) {
            running.value = false;
            runStreamController.value = null;
            refreshSessions();
          }
        });
    }
  } catch (err) {
    if (isCurrentSession(sessionIdAtStart, generation)) {
      error.value = extractApiMessage(err, '恢复待处理任务失败');
    }
  }
}

async function newSession() {
  const sourceSessionId = sessionId.value;
  const nextSessionNumber = sessions.value.length + 1;
  runStreamController.value?.abort();
  runStreamController.value = null;
  runStreamGeneration += 1;
  running.value = false;
  creatingSession.value = true;
  error.value = '';
  if (useMock.value) {
    sessionId.value = Date.now();
    sessions.value = [{
      id: sessionId.value,
      resumeId: props.resumeId,
      targetSectionId: Number(props.section.id),
      sessionTitle: `${sectionTitle.value} · 会话 ${nextSessionNumber}`,
    }, ...sessions.value];
    messages.value = [];
    creatingSession.value = false;
    return sessionId.value;
  }
  try {
    const session = await createAgentSession({
      resumeId: props.resumeId,
      targetSectionId: Number(props.section.id),
      sceneCode: 'JD_CUSTOMIZE',
      sessionTitle: `${sectionTitle.value} · 会话 ${nextSessionNumber}`,
      copyFromSessionId: sourceSessionId,
      copyJobDescription: Boolean(sourceSessionId),
      jobDescription: sourceSessionId ? null : jobDescription.value,
    });
    sessionId.value = session.id;
    await refreshSessions(session.id);
    return session.id;
  } catch (err) {
    error.value = extractApiMessage(err, '创建当前分区的 AI 会话失败');
    if (sourceSessionId) {
      await refreshSessions(sourceSessionId);
    }
    return null;
  } finally {
    creatingSession.value = false;
  }
}

async function ensureSession() {
  if (sessionId.value && !isLegacySession.value) {
    return sessionId.value;
  }
  const createdSessionId = await newSession();
  if (!createdSessionId) {
    throw new Error(error.value || '创建当前分区的 AI 会话失败');
  }
  return sessionId.value;
}

async function startRun() {
  if (!canStart.value) return;
  const resumeIdAtStart = props.resumeId;
  const sectionIdAtStart = props.section.id;
  running.value = true;
  error.value = '';
  approvalPayload.value = null;
  clarificationPayload.value = null;
  events.value = [];
  let generation = runStreamGeneration;
  try {
    const currentSessionId = await ensureSession();
    if (String(props.resumeId) !== String(resumeIdAtStart)
        || String(props.section?.id) !== String(sectionIdAtStart)) {
      return;
    }
    generation = ++runStreamGeneration;
    const controller = new AbortController();
    runStreamController.value = controller;
    const requestPayload = {
      resumeId: props.resumeId,
      sceneCode: 'JD_CUSTOMIZE',
      targetSectionId: props.section.id,
      userInput: userInput.value,
      jobDescription: jobDescription.value,
    };
    const requestFingerprint = JSON.stringify([currentSessionId, requestPayload]);
    const clientRequestId = pendingStartRequestIds.get(requestFingerprint) || createClientRequestId();
    pendingStartRequestIds.set(requestFingerprint, clientRequestId);
    if (!useMock.value) {
      await updateAgentSession(currentSessionId, { jobDescription: jobDescription.value });
    }
    await streamAgentRun(currentSessionId, {
      ...requestPayload,
      clientRequestId,
    }, createRunHandlers(currentSessionId, generation), { mock: useMock.value, signal: controller.signal });
    pendingStartRequestIds.delete(requestFingerprint);
  } catch (err) {
    if (generation === runStreamGeneration) {
      error.value = extractApiMessage(err, 'Agent 运行失败');
    }
  } finally {
    if (generation === runStreamGeneration) {
      running.value = false;
      runStreamController.value = null;
    }
  }
}

async function continueRun() {
  if (!clarificationPayload.value?.runId) return;
  running.value = true;
  const currentSessionId = sessionId.value;
  const generation = ++runStreamGeneration;
  const controller = new AbortController();
  runStreamController.value = controller;
  try {
    const answers = Object.entries(clarificationAnswers.value)
      .filter(([, value]) => String(value || '').trim())
      .map(([fieldKey, value]) => ({ fieldKey, value }));
    await continueAgentRun(
      clarificationPayload.value.runId,
      { answers },
      createRunHandlers(currentSessionId, generation),
      { mock: useMock.value, signal: controller.signal },
    );
  } catch (err) {
    if (isCurrentSession(currentSessionId, generation)) {
      error.value = extractApiMessage(err, '继续运行失败');
    }
  } finally {
    if (isCurrentSession(currentSessionId, generation)) {
      running.value = false;
      runStreamController.value = null;
    }
  }
}

async function approveRun() {
  if (!approvalPayload.value?.runId) {
    return;
  }
  const runId = approvalPayload.value.runId;
  const sessionIdAtStart = sessionId.value;
  const generation = runStreamGeneration;
  approving.value = true;
  error.value = '';
  try {
    const approvedPatchIds = [...selectedPatchIds.value];
    if (!approvedPatchIds.length) {
      error.value = '至少勾选一项修改后再应用。';
      return;
    }
    await props.beforeApprove?.();
    if (!isCurrentSession(sessionIdAtStart, generation)) {
      return;
    }
    const response = await approveAgentRun(
      runId,
      {
        resumeId: props.resumeId,
        approvedPatchIds,
      },
      { mock: useMock.value },
    );
    if (isCurrentSession(sessionIdAtStart, generation)) {
      statusText.value = `已应用 ${response?.appliedPatchCount ?? approvedPatchIds.length} 项，未勾选内容保持原样`;
      await loadSessionDetail();
    }
    emit('approved', response);
  } catch (err) {
    if (isCurrentSession(sessionIdAtStart, generation)) {
      error.value = extractApiMessage(err, '确认应用失败');
    }
  } finally {
    approving.value = false;
  }
}

function isCurrentSession(sessionIdAtStart, generation) {
  return generation === runStreamGeneration && String(sessionId.value) === String(sessionIdAtStart);
}

function createRunHandlers(sessionIdAtStart = sessionId.value, generation = runStreamGeneration) {
  const isCurrent = () => isCurrentSession(sessionIdAtStart, generation);
  return {
    onQueued: () => {
      if (isCurrent()) {
        statusText.value = '已进入队列';
      }
    },
    onEvent: (event) => {
      if (isCurrent()) {
        events.value.push(event);
      }
    },
    onApproval: (payload) => {
      if (!isCurrent()) {
        return;
      }
      approvalPayload.value = payload;
      selectedPatchIds.value = [];
      clarificationPayload.value = null;
      statusText.value = '等待确认应用';
    },
    onClarification: (payload) => {
      if (!isCurrent()) {
        return;
      }
      clarificationPayload.value = payload;
      approvalPayload.value = null;
      selectedPatchIds.value = [];
      statusText.value = '等待补充信息';
    },
  };
}

function selectAllPatches() {
  selectedPatchIds.value = (approvalPayload.value?.patches || []).map((patch) => patch.patchId);
}

function clearSelectedPatches() {
  selectedPatchIds.value = [];
}

function riskBadgeClass(riskLevel) {
  const level = String(riskLevel || '').toUpperCase();
  if (level === 'HIGH') {
    return 'ui-badge--danger';
  }
  if (level === 'MEDIUM') {
    return 'ui-badge--warning';
  }
  return 'ui-badge--success';
}

function riskLevelLabel(riskLevel) {
  const labels = { LOW: '低风险', MEDIUM: '中风险', HIGH: '高风险' };
  return labels[String(riskLevel || '').toUpperCase()] || '待审阅';
}

function runStatusText(status) {
  const labels = {
    QUEUED: '已进入队列，正在等待处理',
    RUNNING: '任务处理中，状态会自动恢复',
    WAITING_USER: '需要补充信息后继续',
    WAITING_CONFIRM: '修改建议已生成，等待你确认',
    SUCCESS: '任务已完成',
    FAILED: '任务执行失败，可重新尝试',
    CANCELLED: '任务已取消',
  };
  return labels[status] || '';
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
  box-shadow: var(--ui-shadow-float);
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
  box-shadow: var(--ui-ring), var(--ui-shadow-float);
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
  background-image: var(--ui-surface-wash);
  color: var(--ui-fg-primary);
  box-shadow: var(--ui-shadow-float);
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

.floating-copilot__legacy-note {
  margin: 0;
  padding: var(--ui-space-2) var(--ui-space-3);
  border-left: 2px solid var(--ui-warning);
  background: var(--ui-warning-soft);
  color: var(--ui-fg-secondary);
  font-size: var(--ui-text-xs);
  line-height: 1.5;
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
  align-items: flex-start;
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

.floating-copilot__review-hint {
  margin: 0;
  color: var(--ui-fg-muted);
  font-size: var(--ui-text-xs);
  line-height: var(--ui-line-normal);
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

.floating-copilot__patch-select {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  align-items: center;
  gap: var(--ui-space-2);
}

.floating-copilot__patch-select input {
  width: 16px;
  height: 16px;
  margin: 0;
  accent-color: var(--ui-accent);
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

.floating-copilot__diff summary {
  width: fit-content;
  color: var(--ui-fg-accent);
  font-size: var(--ui-text-xs);
  cursor: pointer;
}

.floating-copilot__diff-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--ui-space-2);
  margin-top: var(--ui-space-2);
}

.floating-copilot__diff-side {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: var(--ui-space-1);
}

.floating-copilot__diff-side strong {
  color: var(--ui-fg-secondary);
  font-size: var(--ui-text-xs);
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
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}

@media (max-width: 520px) {
  .floating-copilot__diff-grid {
    grid-template-columns: minmax(0, 1fr);
  }
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
