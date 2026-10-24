<template>
  <section class="agent-copilot">
    <header class="agent-copilot__header">
      <div class="agent-copilot__heading">
        <h3 class="agent-copilot__title">模块 Copilot</h3>
        <p class="agent-copilot__subtitle">{{ sectionTitle }}</p>
      </div>
      <label class="switch">
        <input type="checkbox" class="switch__input" :checked="useMock" @change="toggleMock" />
        <span class="switch__track" aria-hidden="true"></span>
        <span class="switch__text">Mock</span>
      </label>
    </header>

    <div class="agent-copilot__fields">
      <div class="agent-copilot__field">
        <label class="ui-label" for="copilot-jd">目标岗位 JD</label>
        <textarea
          id="copilot-jd"
          v-model="jobDescription"
          class="ui-input"
          rows="4"
          placeholder="粘贴目标岗位 JD 或招聘要求"
        ></textarea>
      </div>
      <div class="agent-copilot__field">
        <label class="ui-label" for="copilot-input">优化要求</label>
        <textarea
          id="copilot-input"
          v-model="userInput"
          class="ui-input"
          rows="3"
          placeholder="描述希望 AI 如何优化当前模块"
        ></textarea>
      </div>
    </div>

    <div class="agent-copilot__actions">
      <button
        type="button"
        class="ui-btn ui-btn--sm ui-btn--primary"
        :disabled="running || !canStart"
        @click="startRun"
      >
        {{ running ? '运行中…' : '优化当前模块' }}
      </button>
      <button
        type="button"
        class="ui-btn ui-btn--sm ui-btn--ghost"
        :disabled="running || !events.length"
        @click="resetPanel"
      >
        清空
      </button>
    </div>

    <p v-if="error" class="agent-copilot__error">{{ error }}</p>
    <p v-if="statusText" class="agent-copilot__status">{{ statusText }}</p>

    <div v-if="events.length" class="agent-copilot__events">
      <div
        v-for="event in events"
        :key="`${event.runId}-${event.eventSeq}-${event.eventType}`"
        class="agent-copilot__event"
      >
        <span class="agent-copilot__event-stage">{{ event.stageCode || event.eventType }}</span>
        <span class="agent-copilot__event-message">{{ event.message }}</span>
      </div>
    </div>

    <div v-if="clarificationPayload?.questions?.length" class="agent-copilot__box">
      <h4 class="agent-copilot__box-title">需要补充信息</h4>
      <div v-for="question in clarificationPayload.questions" :key="question.fieldKey" class="agent-copilot__question">
        <label class="ui-label" :for="`copilot-question-${question.fieldKey}`">
          {{ question.question }}
        </label>
        <textarea
          :id="`copilot-question-${question.fieldKey}`"
          v-model="clarificationAnswers[question.fieldKey]"
          class="ui-input"
          rows="2"
        ></textarea>
      </div>
      <button type="button" class="ui-btn ui-btn--sm ui-btn--primary" :disabled="running" @click="continueRun">
        提交补充
      </button>
    </div>

    <div v-if="approvalPayload" class="agent-copilot__box">
      <h4 class="agent-copilot__box-title">{{ approvalPayload.summary || '待确认修改' }}</h4>
      <p v-for="note in approvalPayload.riskNotes || []" :key="note" class="agent-copilot__note">{{ note }}</p>
      <div v-for="patch in approvalPayload.patches || []" :key="patch.patchId" class="agent-copilot__patch">
        <div class="agent-copilot__patch-top">
          <span>{{ patch.sectionTitle || sectionTitle }}</span>
          <span class="ui-badge" :class="riskBadgeClass(patch.riskLevel)">{{ patch.riskLevel || 'LOW' }}</span>
        </div>
        <p class="agent-copilot__patch-summary">{{ patch.changeSummary || patch.reason }}</p>
        <pre class="agent-copilot__code">{{ formatJson(patch.afterJson) }}</pre>
      </div>
      <button type="button" class="ui-btn ui-btn--sm ui-btn--primary" :disabled="approving" @click="approveRun">
        {{ approving ? '应用中…' : '确认应用' }}
      </button>
    </div>
  </section>
</template>

<script setup>
import { computed, ref, watch } from 'vue';
import {
  approveAgentRun,
  continueAgentRun,
  createAgentSession,
  setAgentMockEnabled,
  shouldUseAgentMock,
  streamAgentRun,
} from '../../api/index.js';
import { extractApiMessage } from '../../api/request.js';

const props = defineProps({
  resumeId: {
    type: Number,
    default: null,
  },
  section: {
    type: Object,
    default: null,
  },
});

const emit = defineEmits(['approved']);

const sessionId = ref(null);
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

watch(
  () => props.section?.id,
  () => {
    approvalPayload.value = null;
    clarificationPayload.value = null;
    error.value = '';
    statusText.value = '';
  },
);

async function startRun() {
  if (!canStart.value) return;
  running.value = true;
  error.value = '';
  statusText.value = '';
  approvalPayload.value = null;
  clarificationPayload.value = null;
  events.value = [];

  try {
    const currentSessionId = await ensureSession();
    await streamAgentRun(
      currentSessionId,
      {
        resumeId: props.resumeId,
        sceneCode: 'JD_CUSTOMIZE',
        targetSectionId: props.section.id,
        sectionTitle: props.section.sectionTitle,
        beforeJson: props.section.contentJson,
        userInput: userInput.value,
        jobDescription: jobDescription.value,
      },
      {
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
      },
      { mock: useMock.value },
    );
  } catch (err) {
    error.value = extractApiMessage(err, 'Agent 运行失败');
  } finally {
    running.value = false;
  }
}

async function continueRun() {
  if (!clarificationPayload.value?.runId) return;
  running.value = true;
  error.value = '';
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
  error.value = '';
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
    emit('approved', response);
  } catch (err) {
    error.value = extractApiMessage(err, '确认应用失败');
  } finally {
    approving.value = false;
  }
}

async function ensureSession() {
  if (sessionId.value) {
    return sessionId.value;
  }
  if (useMock.value) {
    sessionId.value = Date.now();
    return sessionId.value;
  }
  const session = await createAgentSession({
    resumeId: props.resumeId,
    sceneCode: 'JD_CUSTOMIZE',
    sessionTitle: `${sectionTitle.value} Copilot`,
  });
  sessionId.value = session.id;
  return sessionId.value;
}

function handleEvent(event) {
  events.value.push(event);
}

function resetPanel() {
  events.value = [];
  approvalPayload.value = null;
  clarificationPayload.value = null;
  clarificationAnswers.value = {};
  error.value = '';
  statusText.value = '';
}

function toggleMock(event) {
  useMock.value = event.target.checked;
  setAgentMockEnabled(useMock.value);
}

function riskBadgeClass(riskLevel) {
  const level = String(riskLevel || '').toUpperCase();
  if (level === 'HIGH') return 'ui-badge--danger';
  if (level === 'MEDIUM') return 'ui-badge--warning';
  return 'ui-badge--success';
}

function formatJson(value) {
  return JSON.stringify(value || {}, null, 2);
}
</script>

<style scoped>
.agent-copilot {
  display: flex;
  flex-direction: column;
  gap: var(--ui-space-3);
  padding: var(--ui-space-4);
  border: 1px solid var(--ui-border);
  border-radius: var(--ui-radius-lg);
  background: var(--ui-bg-surface);
  color: var(--ui-fg-primary);
}

.agent-copilot__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--ui-space-3);
}

.agent-copilot__heading {
  min-width: 0;
}

.agent-copilot__title {
  margin: 0;
  font-size: var(--ui-text-md);
  font-weight: 600;
  color: var(--ui-fg-primary);
}

.agent-copilot__subtitle {
  margin: 2px 0 0;
  font-size: var(--ui-text-sm);
  color: var(--ui-fg-secondary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.agent-copilot__fields {
  display: flex;
  flex-direction: column;
  gap: var(--ui-space-3);
}

.agent-copilot__field {
  display: flex;
  flex-direction: column;
}

.agent-copilot__field :deep(.ui-label) {
  margin-bottom: var(--ui-space-1);
}

.agent-copilot__field :deep(textarea) {
  resize: vertical;
  font-family: inherit;
}

.agent-copilot__actions {
  display: flex;
  align-items: center;
  gap: var(--ui-space-2);
}

.agent-copilot__error {
  margin: 0;
  padding: var(--ui-space-2) var(--ui-space-3);
  border: 1px solid var(--ui-danger-border);
  border-radius: var(--ui-radius-md);
  background: var(--ui-danger-soft);
  color: var(--ui-danger);
  font-size: var(--ui-text-sm);
}

.agent-copilot__status {
  margin: 0;
  font-size: var(--ui-text-xs);
  color: var(--ui-fg-muted);
}

.agent-copilot__events {
  display: flex;
  flex-direction: column;
  gap: var(--ui-space-1);
  max-height: 220px;
  overflow-y: auto;
  padding: var(--ui-space-2);
  border: 1px solid var(--ui-border);
  border-radius: var(--ui-radius-md);
  background: var(--ui-bg-sunken);
}

.agent-copilot__event {
  display: grid;
  grid-template-columns: minmax(76px, 116px) 1fr;
  gap: var(--ui-space-2);
  align-items: baseline;
  font-size: var(--ui-text-xs);
}

.agent-copilot__event-stage {
  color: var(--ui-fg-accent);
  font-weight: 600;
  font-family: var(--ui-font-mono);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.agent-copilot__event-message {
  color: var(--ui-fg-secondary);
  word-break: break-word;
}

.agent-copilot__box {
  display: flex;
  flex-direction: column;
  gap: var(--ui-space-3);
  padding: var(--ui-space-3);
  border: 1px solid var(--ui-border);
  border-radius: var(--ui-radius-md);
  background: var(--ui-bg-sunken);
}

.agent-copilot__box-title {
  margin: 0;
  font-size: var(--ui-text-sm);
  font-weight: 600;
  color: var(--ui-fg-primary);
}

.agent-copilot__question {
  display: flex;
  flex-direction: column;
  gap: var(--ui-space-1);
}

.agent-copilot__question :deep(.ui-label) {
  margin-bottom: 0;
}

.agent-copilot__question :deep(textarea) {
  resize: vertical;
  font-family: inherit;
}

.agent-copilot__note {
  margin: 0;
  font-size: var(--ui-text-xs);
  color: var(--ui-fg-secondary);
}

.agent-copilot__patch {
  display: flex;
  flex-direction: column;
  gap: var(--ui-space-2);
  padding: var(--ui-space-3);
  border: 1px solid var(--ui-border);
  border-radius: var(--ui-radius-md);
  background: var(--ui-bg-surface);
}

.agent-copilot__patch-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--ui-space-2);
  font-size: var(--ui-text-xs);
  font-weight: 600;
  color: var(--ui-fg-secondary);
}

.agent-copilot__patch-summary {
  margin: 0;
  font-size: var(--ui-text-sm);
  color: var(--ui-fg-primary);
}

.agent-copilot__code {
  max-height: 220px;
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
  flex-shrink: 0;
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
</style>
