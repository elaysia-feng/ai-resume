<template>
  <section class="agent-copilot">
    <div class="agent-copilot__header">
      <div>
        <h3>模块 Copilot</h3>
        <p>{{ sectionTitle }}</p>
      </div>
      <label class="agent-copilot__mock">
        <input type="checkbox" :checked="useMock" @change="toggleMock" />
        Mock
      </label>
    </div>

    <div class="agent-copilot__fields">
      <textarea v-model="jobDescription" rows="4" placeholder="粘贴目标岗位 JD 或招聘要求"></textarea>
      <textarea v-model="userInput" rows="3" placeholder="描述希望 AI 如何优化当前模块"></textarea>
    </div>

    <div class="agent-copilot__actions">
      <button type="button" :disabled="running || !canStart" @click="startRun">
        {{ running ? '运行中...' : '优化当前模块' }}
      </button>
      <button type="button" class="agent-copilot__ghost" :disabled="running || !events.length" @click="resetPanel">
        清空
      </button>
    </div>

    <p v-if="error" class="agent-copilot__error">{{ error }}</p>
    <p v-if="statusText" class="agent-copilot__status">{{ statusText }}</p>

    <div v-if="events.length" class="agent-copilot__events">
      <div v-for="event in events" :key="`${event.runId}-${event.eventSeq}-${event.eventType}`" class="agent-copilot__event">
        <span>{{ event.stageCode || event.eventType }}</span>
        <strong>{{ event.message }}</strong>
      </div>
    </div>

    <div v-if="clarificationPayload?.questions?.length" class="agent-copilot__approval">
      <h4>需要补充信息</h4>
      <div v-for="question in clarificationPayload.questions" :key="question.fieldKey" class="agent-copilot__question">
        <label>{{ question.question }}</label>
        <textarea v-model="clarificationAnswers[question.fieldKey]" rows="2"></textarea>
      </div>
      <button type="button" :disabled="running" @click="continueRun">提交补充</button>
    </div>

    <div v-if="approvalPayload" class="agent-copilot__approval">
      <h4>{{ approvalPayload.summary || '待确认修改' }}</h4>
      <p v-for="note in approvalPayload.riskNotes || []" :key="note" class="agent-copilot__note">{{ note }}</p>
      <div v-for="patch in approvalPayload.patches || []" :key="patch.patchId" class="agent-copilot__patch">
        <div class="agent-copilot__patch-top">
          <span>{{ patch.sectionTitle || sectionTitle }}</span>
          <strong>{{ patch.riskLevel || 'LOW' }}</strong>
        </div>
        <p>{{ patch.changeSummary || patch.reason }}</p>
        <pre>{{ formatJson(patch.afterJson) }}</pre>
      </div>
      <button type="button" :disabled="approving" @click="approveRun">
        {{ approving ? '应用中...' : '确认应用' }}
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

function formatJson(value) {
  return JSON.stringify(value || {}, null, 2);
}
</script>

<style scoped>
.agent-copilot {
  border: 1px solid #d1d5db;
  border-radius: 8px;
  padding: 14px;
  margin-top: 14px;
  background: #f8fafc;
}

.agent-copilot__header,
.agent-copilot__actions,
.agent-copilot__patch-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.agent-copilot h3,
.agent-copilot h4,
.agent-copilot p {
  margin: 0;
}

.agent-copilot__header p,
.agent-copilot__note,
.agent-copilot__status {
  color: #64748b;
  font-size: 12px;
}

.agent-copilot__mock {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #475569;
}

.agent-copilot__fields {
  display: grid;
  gap: 8px;
  margin: 12px 0;
}

.agent-copilot textarea {
  width: 100%;
  resize: vertical;
  border: 1px solid #cbd5e1;
  border-radius: 6px;
  padding: 9px;
  font: inherit;
  box-sizing: border-box;
}

.agent-copilot button {
  border: 0;
  border-radius: 6px;
  padding: 8px 12px;
  background: #2563eb;
  color: #fff;
  font-weight: 700;
  cursor: pointer;
}

.agent-copilot button:disabled {
  cursor: not-allowed;
  opacity: 0.6;
}

.agent-copilot__ghost {
  background: #e2e8f0 !important;
  color: #334155 !important;
}

.agent-copilot__error {
  margin-top: 10px !important;
  color: #b91c1c;
  font-size: 13px;
}

.agent-copilot__events,
.agent-copilot__approval {
  display: grid;
  gap: 8px;
  margin-top: 12px;
}

.agent-copilot__event,
.agent-copilot__patch,
.agent-copilot__question {
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  padding: 10px;
  background: #fff;
}

.agent-copilot__event {
  display: grid;
  grid-template-columns: minmax(82px, 120px) 1fr;
  gap: 8px;
  font-size: 12px;
}

.agent-copilot__event span {
  color: #64748b;
}

.agent-copilot__patch pre {
  max-height: 220px;
  overflow: auto;
  margin: 8px 0 0;
  padding: 10px;
  border-radius: 6px;
  background: #0f172a;
  color: #e2e8f0;
  font-size: 12px;
}
</style>
