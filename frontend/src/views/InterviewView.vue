<template>
  <section class="interview-page">
    <header class="page-header">
      <div>
        <h1 class="page-title">模拟面试</h1>
        <p class="page-subtitle">基于当前后端的 interview run、board 和 question-rounds 接口</p>
      </div>
      <button class="refresh-btn" :disabled="loadingSessions" @click="loadInterviewSessions">刷新会话</button>
    </header>

    <div class="setup-card">
      <div class="setup-grid">
        <div class="field">
          <label>选择简历</label>
          <select v-model="createForm.resumeId" class="field-input">
            <option value="">不关联简历</option>
            <option v-for="resume in resumes" :key="resume.id" :value="resume.id">{{ resume.title }}</option>
          </select>
        </div>
        <div class="field">
          <label>会话标题</label>
          <input v-model="createForm.sessionTitle" class="field-input" type="text" placeholder="例如：Java 后端一面" />
        </div>
      </div>
      <div class="field">
        <label>岗位 JD</label>
        <textarea v-model="createForm.jobDescription" class="field-textarea" rows="4" placeholder="填写岗位要求，留空也可以启动"></textarea>
      </div>
      <button class="primary-btn" :disabled="creating" @click="handleCreateAndStart">
        {{ creating ? '启动中...' : '新建并开始面试' }}
      </button>
    </div>

    <p v-if="message" class="message success">{{ message }}</p>
    <p v-if="error" class="message error">{{ error }}</p>

    <div class="interview-layout">
      <aside class="session-card">
        <div v-if="loadingSessions" class="empty-block">加载中...</div>
        <div v-else-if="sessions.length === 0" class="empty-block">暂无面试会话</div>
        <button
          v-for="session in sessions"
          :key="session.id"
          class="session-item"
          :class="{ active: selectedRunId === session.activeRunId }"
          @click="selectSession(session)"
        >
          <span class="session-title">{{ session.sessionTitle || `面试会话 #${session.id}` }}</span>
          <span class="session-meta">{{ session.activeRunStatus || session.status }} · {{ formatDateTime(session.updatedAt || session.lastMessageAt) }}</span>
        </button>
      </aside>

      <section class="board-card">
        <div v-if="!selectedRunId" class="empty-block">选择或新建一个面试会话</div>
        <template v-else>
          <div class="board-top">
            <div>
              <h2>Run #{{ selectedRunId }}</h2>
              <p>状态：{{ board?.status || '未加载' }}</p>
            </div>
            <button class="ghost-btn" :disabled="loadingBoard" @click="refreshBoard">
              {{ loadingBoard ? '刷新中...' : '刷新当前题' }}
            </button>
          </div>

          <div v-if="board?.errorMessage" class="message error">{{ board.errorMessage }}</div>
          <div v-if="board?.summary" class="summary-box">{{ board.summary }}</div>

          <div v-if="currentQuestion" class="question-card">
            <div class="question-head">
              <span>第 {{ currentQuestion.roundNo }} 轮</span>
              <span>{{ currentQuestion.status }}</span>
            </div>
            <h3>{{ currentQuestion.questionText }}</h3>
            <div class="option-list">
              <label
                v-for="option in currentQuestion.options || []"
                :key="option.key"
                class="option-item"
                :class="{ selected: answerForm.selectedOption === option.key }"
              >
                <input v-model="answerForm.selectedOption" type="radio" :value="option.key" />
                <strong>{{ option.key }}</strong>
                <span>{{ option.text }}</span>
              </label>
            </div>
            <div class="field">
              <label>补充说明</label>
              <textarea v-model="answerForm.supplementText" class="field-textarea" rows="4" placeholder="可以补充你的选择理由或自由回答"></textarea>
            </div>
            <button class="primary-btn" :disabled="submitting" @click="handleSubmitAnswer">
              {{ submitting ? '提交中...' : '提交答案' }}
            </button>
          </div>
          <div v-else class="empty-block">当前没有待回答题目，可稍后刷新。</div>

          <div class="history-header">
            <strong>已回答轮次</strong>
            <button class="ghost-btn small" :disabled="loadingRounds" @click="loadRounds">刷新历史</button>
          </div>
          <div class="round-list">
            <div v-if="rounds.length === 0" class="empty-block small">暂无已回答记录</div>
            <article v-for="round in rounds" :key="round.roundId" class="round-item">
              <div class="round-top">
                <span>第 {{ round.roundNo }} 轮</span>
                <span>{{ round.status }}</span>
              </div>
              <h4>{{ round.questionText }}</h4>
              <p class="answer-text">{{ formatAnswer(round.userAnswer) }}</p>
              <p v-if="round.analysis" class="analysis-text">{{ round.analysis }}</p>
            </article>
          </div>
        </template>
      </section>
    </div>
  </section>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue';
import {
  createAgentSession,
  extractApiMessage,
  getInterviewBoard,
  getInterviewQuestionRounds,
  getResumes,
  listAgentSessions,
  startInterviewRun,
  submitInterviewAnswer,
} from '../api/index.js';
import { formatDateTime } from '../utils/resume.js';

const POLL_INTERVAL_MS = 2500;

const loadingSessions = ref(false);
const loadingBoard = ref(false);
const loadingRounds = ref(false);
const creating = ref(false);
const submitting = ref(false);
const message = ref('');
const error = ref('');
const resumes = ref([]);
const sessions = ref([]);
const selectedRunId = ref(null);
const board = ref(null);
const rounds = ref([]);
const activeRoundId = ref(null);
let pollTimer = null;

const createForm = reactive({
  resumeId: '',
  sessionTitle: '',
  jobDescription: '',
});

const answerForm = reactive({
  selectedOption: '',
  supplementText: '',
});

const currentQuestion = computed(() => board.value?.currentQuestion || null);

onMounted(async () => {
  await Promise.all([loadResumes(), loadInterviewSessions()]);
});

onBeforeUnmount(() => {
  stopPolling();
});

function setStatus(successMessage = '', errorMessage = '') {
  message.value = successMessage;
  error.value = errorMessage;
}

async function loadResumes() {
  try {
    resumes.value = await getResumes();
  } catch (err) {
    setStatus('', extractApiMessage(err, '加载简历列表失败'));
  }
}

async function loadInterviewSessions() {
  loadingSessions.value = true;
  try {
    sessions.value = await listAgentSessions({ sceneCode: 'INTERVIEW' });
    if (!selectedRunId.value) {
      const activeSession = sessions.value.find((session) => session.activeRunId);
      if (activeSession) {
        await selectSession(activeSession);
      }
    }
  } catch (err) {
    setStatus('', extractApiMessage(err, '加载面试会话失败'));
  } finally {
    loadingSessions.value = false;
  }
}

async function handleCreateAndStart() {
  creating.value = true;
  setStatus();
  try {
    const session = await createAgentSession({
      resumeId: createForm.resumeId || null,
      sceneCode: 'INTERVIEW',
      sessionTitle: createForm.sessionTitle || '模拟面试',
      jobDescription: createForm.jobDescription || null,
    });
    const runId = await startInterviewRun({ sessionId: session.id });
    selectedRunId.value = runId;
    createForm.resumeId = '';
    createForm.sessionTitle = '';
    createForm.jobDescription = '';
    await loadInterviewSessions();
    await refreshBoard();
    await loadRounds();
    startPolling();
    setStatus('面试已启动');
  } catch (err) {
    setStatus('', extractApiMessage(err, '启动面试失败'));
  } finally {
    creating.value = false;
  }
}

async function selectSession(session) {
  if (!session.activeRunId) {
    selectedRunId.value = null;
    board.value = null;
    rounds.value = [];
    stopPolling();
    setStatus('', '该会话暂无进行中的面试');
    return;
  }
  selectedRunId.value = session.activeRunId;
  setStatus();
  await refreshBoard();
  await loadRounds();
  if (shouldKeepPolling(board.value?.status)) {
    startPolling();
  }
}

async function refreshBoard() {
  if (!selectedRunId.value) {
    return;
  }
  loadingBoard.value = true;
  try {
    const nextBoard = await getInterviewBoard(selectedRunId.value);
    const nextRoundId = nextBoard?.currentQuestion?.roundId || null;
    if (activeRoundId.value !== nextRoundId) {
      resetAnswerForm();
      activeRoundId.value = nextRoundId;
    }
    board.value = nextBoard;
    if (!shouldKeepPolling(nextBoard?.status)) {
      stopPolling();
    }
  } catch (err) {
    setStatus('', extractApiMessage(err, '加载当前题失败'));
  } finally {
    loadingBoard.value = false;
  }
}

async function loadRounds() {
  if (!selectedRunId.value) {
    return;
  }
  loadingRounds.value = true;
  try {
    const page = await getInterviewQuestionRounds(selectedRunId.value, { pageNum: 1, pageSize: 20 });
    rounds.value = page.records || [];
  } catch (err) {
    setStatus('', extractApiMessage(err, '加载面试历史失败'));
  } finally {
    loadingRounds.value = false;
  }
}

async function handleSubmitAnswer() {
  if (!currentQuestion.value?.roundId) {
    setStatus('', '当前没有可提交的题目');
    return;
  }
  if (!answerForm.selectedOption && !answerForm.supplementText.trim()) {
    setStatus('', '请先选择选项或填写补充说明');
    return;
  }
  submitting.value = true;
  setStatus();
  try {
    await submitInterviewAnswer(currentQuestion.value.roundId, {
      status: 'ANSWERED',
      userAnswer: JSON.stringify({
        selectedOption: answerForm.selectedOption || null,
        supplementText: answerForm.supplementText.trim() || null,
      }),
    });
    await refreshBoard();
    await loadRounds();
    if (shouldKeepPolling(board.value?.status)) {
      startPolling();
    }
    setStatus('答案已提交');
  } catch (err) {
    setStatus('', extractApiMessage(err, '提交答案失败'));
  } finally {
    submitting.value = false;
  }
}

function resetAnswerForm() {
  answerForm.selectedOption = '';
  answerForm.supplementText = '';
}

function startPolling() {
  stopPolling();
  pollTimer = window.setInterval(async () => {
    if (!selectedRunId.value || loadingBoard.value) {
      return;
    }
    await refreshBoard();
    await loadRounds();
  }, POLL_INTERVAL_MS);
}

function stopPolling() {
  if (pollTimer) {
    window.clearInterval(pollTimer);
    pollTimer = null;
  }
}

function shouldKeepPolling(status) {
  return !['SUCCESS', 'FAILED', 'CANCELLED'].includes(status || '');
}

function formatAnswer(rawAnswer) {
  if (!rawAnswer) {
    return '未记录答案';
  }
  try {
    const answer = JSON.parse(rawAnswer);
    const parts = [];
    if (answer.selectedOption) {
      parts.push(`选择：${answer.selectedOption}`);
    }
    if (answer.supplementText) {
      parts.push(`补充：${answer.supplementText}`);
    }
    return parts.join('；') || rawAnswer;
  } catch {
    return rawAnswer;
  }
}
</script>

<style scoped>
.interview-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.page-header,
.setup-grid,
.board-top,
.question-head,
.history-header,
.round-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.page-title {
  margin: 0;
  font-size: 24px;
  font-weight: 700;
  color: #1a1a2e;
}

.page-subtitle {
  margin: 6px 0 0;
  font-size: 14px;
  color: #6b7280;
}

.setup-card,
.session-card,
.board-card,
.question-card {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  padding: 16px;
}

.setup-card,
.board-card,
.question-card,
.round-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.setup-grid {
  align-items: end;
}

.field {
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: 6px;
}

.field label {
  font-size: 13px;
  font-weight: 600;
  color: #374151;
}

.field-input,
.field-textarea {
  width: 100%;
  box-sizing: border-box;
  border: 1px solid #d1d5db;
  border-radius: 8px;
  padding: 10px 12px;
  font-size: 14px;
}

.field-textarea {
  resize: vertical;
  min-height: 96px;
  font-family: inherit;
}

.refresh-btn,
.ghost-btn,
.primary-btn {
  border-radius: 8px;
  padding: 10px 14px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
}

.refresh-btn,
.ghost-btn {
  border: 1px solid #d1d5db;
  background: #fff;
  color: #374151;
}

.ghost-btn.small {
  padding: 6px 10px;
}

.primary-btn {
  align-self: flex-start;
  border: none;
  background: #22c55e;
  color: #fff;
}

button:disabled {
  cursor: not-allowed;
  opacity: 0.65;
}

.message {
  margin: 0;
  padding: 10px 12px;
  border-radius: 8px;
  font-size: 13px;
}

.message.success {
  background: #dcfce7;
  color: #166534;
}

.message.error {
  background: #fee2e2;
  color: #991b1b;
}

.interview-layout {
  display: grid;
  grid-template-columns: 320px 1fr;
  gap: 16px;
  min-height: 560px;
}

.session-card {
  display: flex;
  flex-direction: column;
  gap: 10px;
  overflow-y: auto;
}

.session-item {
  display: flex;
  width: 100%;
  flex-direction: column;
  gap: 8px;
  border: 1px solid #e5e7eb;
  background: #fff;
  border-radius: 10px;
  padding: 12px;
  text-align: left;
  cursor: pointer;
}

.session-item.active {
  border-color: #22c55e;
  background: #f0fdf4;
}

.session-title {
  font-size: 14px;
  font-weight: 600;
  color: #111827;
}

.session-meta,
.question-head,
.round-top {
  font-size: 12px;
  color: #6b7280;
}

.board-top {
  align-items: flex-start;
}

.board-top h2,
.question-card h3,
.round-item h4 {
  margin: 0;
  color: #111827;
}

.board-top p {
  margin: 8px 0 0;
  color: #6b7280;
  font-size: 13px;
}

.summary-box {
  border-radius: 10px;
  background: #f9fafb;
  padding: 12px;
  color: #374151;
  font-size: 13px;
  line-height: 1.7;
}

.option-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.option-item {
  display: grid;
  grid-template-columns: 20px 28px 1fr;
  align-items: center;
  gap: 8px;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  padding: 10px 12px;
  cursor: pointer;
}

.option-item.selected {
  border-color: #22c55e;
  background: #f0fdf4;
}

.round-item {
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  padding: 12px;
  background: #fff;
}

.answer-text,
.analysis-text {
  margin: 8px 0 0;
  color: #374151;
  font-size: 13px;
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
}

.analysis-text {
  color: #047857;
}

.empty-block {
  display: flex;
  min-height: 180px;
  align-items: center;
  justify-content: center;
  color: #9ca3af;
  font-size: 13px;
}

.empty-block.small {
  min-height: 100px;
}

@media (max-width: 1100px) {
  .interview-layout {
    grid-template-columns: 1fr;
  }

  .page-header,
  .setup-grid,
  .board-top {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
