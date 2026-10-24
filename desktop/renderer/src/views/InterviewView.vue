<template>
  <div class="interview-page">
    <header class="page-head">
      <div class="page-head__text">
        <h1 class="page-title">模拟面试</h1>
        <p class="page-subtitle">基于当前后端的 interview run、board 和 question-rounds 接口，逐轮作答并查看结构化反馈。</p>
      </div>
      <div class="page-head__actions">
        <span v-if="runBadge" class="ui-badge" :class="runBadge.tone">{{ runBadge.label }}</span>
        <button class="ui-btn ui-btn--outline" :disabled="loadingSessions" @click="loadInterviewSessions">
          {{ loadingSessions ? '刷新中…' : '刷新会话' }}
        </button>
      </div>
    </header>

    <!-- 新建面试 -->
    <section class="ui-card setup">
      <header class="setup__head">
        <h2 class="setup__title">开始新的模拟面试</h2>
        <p class="setup__desc">选择简历并补充岗位 JD 后启动，题目会逐轮下发。</p>
      </header>
      <div class="setup__grid">
        <div class="field">
          <label class="ui-label" for="setup-resume">选择简历</label>
          <select id="setup-resume" v-model="createForm.resumeId" class="ui-input ui-select">
            <option value="">不关联简历</option>
            <option v-for="resume in resumes" :key="resume.id" :value="resume.id">{{ resume.title }}</option>
          </select>
        </div>
        <div class="field">
          <label class="ui-label" for="setup-title">会话标题</label>
          <input id="setup-title" v-model="createForm.sessionTitle" class="ui-input" type="text" placeholder="例如：Java 后端一面" />
        </div>
        <div class="field field--wide">
          <label class="ui-label" for="setup-jd">岗位 JD</label>
          <textarea id="setup-jd" v-model="createForm.jobDescription" class="ui-input" rows="3" placeholder="填写岗位要求，留空也可以启动"></textarea>
        </div>
      </div>
      <div class="setup__actions">
        <button class="ui-btn ui-btn--primary" :disabled="creating" @click="handleCreateAndStart">
          {{ creating ? '启动中…' : '新建并开始面试' }}
        </button>
      </div>
    </section>

    <p v-if="message" class="notice notice--success" role="status" aria-live="polite">{{ message }}</p>
    <p v-if="error" class="notice notice--danger" role="alert">{{ error }}</p>

    <div class="interview-layout">
      <!-- 面试会话列表 -->
      <aside class="ui-card session-panel">
        <header class="panel-head">
          <h2 class="panel-head__title">面试会话</h2>
          <span class="ui-badge">{{ sessions.length }}</span>
        </header>
        <div class="session-list ui-scroll">
          <p v-if="loadingSessions" class="state-block">加载中…</p>
          <p v-else-if="sessions.length === 0" class="state-block">暂无面试会话</p>
          <button
            v-for="session in sessions"
            :key="session.id"
            type="button"
            class="session-item"
            :class="{ 'is-active': selectedRunId === session.activeRunId }"
            @click="selectSession(session)"
          >
            <span class="session-item__title">{{ session.sessionTitle || `面试会话 #${session.id}` }}</span>
            <span class="session-item__meta">
              <span class="ui-badge" :class="sessionBadge(session).tone">{{ sessionBadge(session).label }}</span>
              <span class="session-item__time">{{ formatDateTime(session.updatedAt || session.lastMessageAt) }}</span>
            </span>
          </button>
        </div>
      </aside>

      <!-- 面试看板 -->
      <section class="board">
        <div v-if="!selectedRunId" class="ui-card board-empty">
          <div class="ui-empty">
            <p class="ui-empty__title">还没有进行中的面试</p>
            <p class="ui-empty__hint">在上方选择简历并填写岗位 JD，点击「新建并开始面试」即可开始第一轮问答。</p>
          </div>
        </div>

        <template v-else>
          <header class="ui-card board-head">
            <div class="board-head__text">
              <h2 class="board-head__title">Run #{{ selectedRunId }}</h2>
              <p class="board-head__meta">
                状态：{{ board?.status || '未加载' }}
                <template v-if="loadingBoard">· 加载中…</template>
              </p>
            </div>
            <div class="board-head__actions">
              <span v-if="runBadge" class="ui-badge" :class="runBadge.tone">{{ runBadge.label }}</span>
              <button class="ui-btn ui-btn--outline ui-btn--sm" :disabled="loadingBoard" @click="refreshBoard">
                {{ loadingBoard ? '刷新中…' : '刷新当前题' }}
              </button>
            </div>
          </header>

          <p v-if="board?.errorMessage" class="notice notice--danger">{{ board.errorMessage }}</p>

          <!-- 面试总结：总分 / 亮点 / 待提升 -->
          <section v-if="summaryView" class="ui-card summary">
            <header class="panel-head">
              <h2 class="panel-head__title">面试总结</h2>
              <span v-if="summaryView.score !== null" class="ui-badge ui-badge--accent">{{ summaryView.score }} 分</span>
            </header>
            <div class="summary__body">
              <p v-if="summaryView.text" class="summary__text">{{ summaryView.text }}</p>
              <div class="summary__columns">
                <div v-if="summaryView.highlight.length" class="summary__block">
                  <span class="summary__label">亮点</span>
                  <ul class="summary__list">
                    <li v-for="(item, index) in summaryView.highlight" :key="`h-${index}`">{{ item }}</li>
                  </ul>
                </div>
                <div v-if="summaryView.improvement.length" class="summary__block">
                  <span class="summary__label">待提升</span>
                  <ul class="summary__list summary__list--warn">
                    <li v-for="(item, index) in summaryView.improvement" :key="`i-${index}`">{{ item }}</li>
                  </ul>
                </div>
              </div>
            </div>
          </section>

          <!-- 当前题目 -->
          <section v-if="currentQuestion" class="ui-card question">
            <header class="question__head">
              <span class="ui-badge ui-badge--accent">第 {{ currentQuestion.roundNo }} 轮</span>
              <span class="ui-badge">{{ currentQuestion.status }}</span>
              <span class="question__hint">选择答案并补充说明后提交</span>
            </header>
            <h3 class="question__text">{{ currentQuestion.questionText }}</h3>

            <div class="option-list">
              <label
                v-for="option in currentQuestion.options || []"
                :key="option.key"
                class="option-item"
                :class="{ 'is-selected': answerForm.selectedOption === option.key }"
              >
                <input v-model="answerForm.selectedOption" type="radio" :value="option.key" />
                <span class="option-item__key">{{ option.key }}</span>
                <span class="option-item__text">{{ option.text }}</span>
              </label>
            </div>

            <div class="field">
              <label class="ui-label" for="answer-supplement">补充说明</label>
              <textarea
                id="answer-supplement"
                v-model="answerForm.supplementText"
                class="ui-input"
                rows="3"
                placeholder="可以补充你的选择理由或自由回答"
              ></textarea>
            </div>

            <!-- 底部固定输入区 -->
            <div class="question__actions">
              <button class="ui-btn ui-btn--primary" :disabled="submitting" @click="handleSubmitAnswer">
                {{ submitting ? '提交中…' : '提交答案' }}
              </button>
              <span class="question__tip">提交后自动进入下一题</span>
            </div>
          </section>
          <div v-else class="ui-card board-empty">
            <div class="ui-empty">
              <p class="ui-empty__title">当前没有待回答题目</p>
              <p class="ui-empty__hint">可能正在出题，稍等几秒后点「刷新当前题」即可。</p>
            </div>
          </div>

          <!-- 已回答轮次 -->
          <section class="ui-card rounds">
            <header class="panel-head">
              <h2 class="panel-head__title">已回答轮次</h2>
              <button class="ui-btn ui-btn--ghost ui-btn--sm" :disabled="loadingRounds" @click="loadRounds">
                {{ loadingRounds ? '加载中…' : '刷新历史' }}
              </button>
            </header>
            <div class="round-list ui-scroll">
              <p v-if="loadingRounds" class="state-block">加载中…</p>
              <p v-else-if="rounds.length === 0" class="state-block">暂无已回答记录</p>
              <article v-for="round in rounds" :key="round.roundId" class="round-item">
                <header class="round-item__head">
                  <span class="ui-badge ui-badge--accent">第 {{ round.roundNo }} 轮</span>
                  <span class="ui-badge" :class="roundBadge(round).tone">{{ roundBadge(round).label }}</span>
                  <span v-if="roundScore(round) !== null" class="round-item__score">
                    {{ starText(roundScore(round)) }} · {{ roundScore(round) }} 分
                  </span>
                </header>
                <h4 class="round-item__question">{{ round.questionText }}</h4>
                <div class="round-item__block">
                  <span class="round-item__label">我的回答</span>
                  <p class="round-item__text">{{ formatAnswer(round.userAnswer) }}</p>
                </div>
                <div v-if="roundAnalysis(round)" class="round-item__block">
                  <span class="round-item__label">评价</span>
                  <p class="round-item__text">{{ roundAnalysis(round).evaluation }}</p>
                  <p v-if="roundAnalysis(round).reason" class="round-item__reason">
                    判断依据：{{ roundAnalysis(round).reason }}
                  </p>
                </div>
                <div v-else-if="round.analysis" class="round-item__block">
                  <span class="round-item__label">评价</span>
                  <p class="round-item__text">{{ round.analysis }}</p>
                </div>
              </article>
            </div>
          </section>
        </template>
      </section>
    </div>
  </div>
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

// run / 会话 / 轮次的状态文案与徽标色调
const RUN_STATUS_VIEW = {
  QUEUED: { label: '排队中', tone: '' },
  RUNNING: { label: '进行中', tone: 'ui-badge--accent' },
  WAITING_USER: { label: '等待回答', tone: 'ui-badge--warning' },
  SUCCESS: { label: '已结束', tone: 'ui-badge--success' },
  FAILED: { label: '执行失败', tone: 'ui-badge--danger' },
  CANCELLED: { label: '已取消', tone: '' },
};

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

const runBadge = computed(() => {
  if (!board.value?.status) {
    return null;
  }
  return RUN_STATUS_VIEW[board.value.status] || { label: board.value.status, tone: '' };
});

// 面试总结在后端存的是 JSON 字符串（summary / highlight / improvement / score），
// 解析失败或内容为空时退回纯文本展示
const summaryView = computed(() => {
  const raw = board.value?.summary;
  if (!raw) {
    return null;
  }

  let parsed = null;
  try {
    parsed = JSON.parse(raw);
  } catch {
    // 不是 JSON，按纯文本处理
  }

  if (parsed && typeof parsed === 'object') {
    const view = {
      score: typeof parsed.score === 'number' ? parsed.score : null,
      text: typeof parsed.summary === 'string' ? parsed.summary : '',
      highlight: Array.isArray(parsed.highlight) ? parsed.highlight.filter((item) => typeof item === 'string') : [],
      improvement: Array.isArray(parsed.improvement) ? parsed.improvement.filter((item) => typeof item === 'string') : [],
    };
    return view.text || view.score !== null || view.highlight.length || view.improvement.length ? view : null;
  }

  if (typeof parsed === 'string' && parsed.trim()) {
    return { score: null, text: parsed, highlight: [], improvement: [] };
  }

  return { score: null, text: String(raw), highlight: [], improvement: [] };
});

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

// 会话徽标：有活跃 run 时看 run 状态，否则看会话状态
function sessionBadge(session) {
  const runStatus = session.activeRunStatus;
  if (runStatus && RUN_STATUS_VIEW[runStatus]) {
    return RUN_STATUS_VIEW[runStatus];
  }
  if (session.activeRunId) {
    return { label: '进行中', tone: 'ui-badge--accent' };
  }
  if (session.status === 'ARCHIVED') {
    return { label: '已归档', tone: '' };
  }
  return { label: '未开始', tone: '' };
}

function roundBadge(round) {
  if (round.status === 'ANSWERED') {
    return { label: '已作答', tone: 'ui-badge--success' };
  }
  return { label: round.status || '未知', tone: '' };
}

// 单轮分析在后端存的是 JSON 字符串：evaluation / score / reason
function roundAnalysis(round) {
  const raw = round?.analysis;
  if (!raw) {
    return null;
  }
  try {
    const parsed = JSON.parse(raw);
    if (parsed && typeof parsed === 'object') {
      return {
        evaluation: typeof parsed.evaluation === 'string' ? parsed.evaluation : '',
        score: typeof parsed.score === 'number' ? parsed.score : null,
        reason: typeof parsed.reason === 'string' ? parsed.reason : '',
      };
    }
  } catch {
    // 不是 JSON，模板里会退回原始文本
  }
  return null;
}

function roundScore(round) {
  const analysis = roundAnalysis(round);
  if (analysis && analysis.score !== null) {
    return analysis.score;
  }
  return null;
}

// 0-100 分映射成 5 星，用于徽标旁的直观展示
function starText(score) {
  const filled = Math.max(0, Math.min(5, Math.round((score / 100) * 5)));
  return '★'.repeat(filled) + '☆'.repeat(5 - filled);
}
</script>

<style scoped>
.interview-page {
  display: flex;
  flex-direction: column;
  gap: var(--ui-space-6);
  max-width: 1280px;
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
  align-items: center;
  flex-shrink: 0;
  gap: var(--ui-space-3);
}

/* ---------- 提示条 ---------- */
.notice {
  margin: 0;
  padding: var(--ui-space-3) var(--ui-space-4);
  border-radius: var(--ui-radius-md);
  font-size: var(--ui-text-sm);
  line-height: var(--ui-line-normal);
}

.notice--success {
  background: var(--ui-success-soft);
  border: 1px solid var(--ui-success-border);
  color: var(--ui-fg-primary);
}

.notice--danger {
  background: var(--ui-danger-soft);
  border: 1px solid var(--ui-danger-border);
  color: var(--ui-fg-primary);
}

/* ---------- 新建面试 ---------- */
.setup {
  padding: var(--ui-space-5);
}

.setup__head {
  margin-bottom: var(--ui-space-4);
}

.setup__title {
  margin: 0;
  font-size: var(--ui-text-md);
  font-weight: 600;
  color: var(--ui-fg-primary);
}

.setup__desc {
  margin: var(--ui-space-1) 0 0;
  font-size: var(--ui-text-sm);
  color: var(--ui-fg-secondary);
}

.setup__grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--ui-space-4);
}

.setup__actions {
  display: flex;
  justify-content: flex-end;
  margin-top: var(--ui-space-4);
}

/* ---------- 表单 ---------- */
.field {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.field--wide {
  grid-column: 1 / -1;
}

textarea.ui-input {
  resize: vertical;
  font-family: inherit;
}

.ui-select {
  appearance: none;
  padding-right: var(--ui-space-8);
  background-image: linear-gradient(45deg, transparent 50%, currentColor 50%),
    linear-gradient(135deg, currentColor 50%, transparent 50%);
  background-position: calc(100% - 16px) calc(50% + 1px), calc(100% - 11px) calc(50% + 1px);
  background-size: 5px 5px, 5px 5px;
  background-repeat: no-repeat;
}

/* ---------- 主布局 ---------- */
.interview-layout {
  display: grid;
  grid-template-columns: 268px minmax(0, 1fr);
  gap: var(--ui-space-5);
  align-items: start;
}

/* ---------- 会话列表 ---------- */
.session-panel {
  display: flex;
  flex-direction: column;
  gap: var(--ui-space-3);
  padding: var(--ui-space-4);
  max-height: calc(100vh - 220px);
}

.panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--ui-space-3);
  margin-bottom: var(--ui-space-3);
}

.panel-head__title {
  margin: 0;
  font-size: var(--ui-text-base);
  font-weight: 600;
  color: var(--ui-fg-primary);
}

.session-list {
  display: flex;
  flex-direction: column;
  gap: var(--ui-space-2);
  min-height: 0;
}

.session-item {
  display: flex;
  flex-direction: column;
  gap: var(--ui-space-2);
  width: 100%;
  padding: var(--ui-space-3);
  border: 1px solid var(--ui-border);
  border-radius: var(--ui-radius-md);
  background: var(--ui-bg-surface);
  color: var(--ui-fg-primary);
  text-align: left;
  cursor: pointer;
  transition: border-color var(--ui-duration-fast) var(--ui-ease-in-out),
    background var(--ui-duration-fast) var(--ui-ease-in-out);
}

.session-item:hover {
  background: var(--ui-bg-hover);
}

.session-item:focus-visible {
  outline: none;
  box-shadow: var(--ui-ring);
}

.session-item.is-active {
  border-color: var(--ui-accent-border);
  background: var(--ui-bg-selected);
}

.session-item__title {
  font-size: var(--ui-text-base);
  font-weight: 600;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.session-item__meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--ui-space-2);
}

.session-item__time {
  font-size: var(--ui-text-xs);
  color: var(--ui-fg-muted);
}

.state-block {
  margin: 0;
  padding: var(--ui-space-6) var(--ui-space-3);
  text-align: center;
  font-size: var(--ui-text-sm);
  color: var(--ui-fg-muted);
}

/* ---------- 看板 ---------- */
.board {
  display: flex;
  flex-direction: column;
  gap: var(--ui-space-4);
  min-width: 0;
}

.board-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--ui-space-4);
  padding: var(--ui-space-4) var(--ui-space-5);
}

.board-head__title {
  margin: 0;
  font-size: var(--ui-text-lg);
  font-weight: 600;
  color: var(--ui-fg-primary);
}

.board-head__meta {
  margin: var(--ui-space-1) 0 0;
  font-size: var(--ui-text-sm);
  color: var(--ui-fg-secondary);
}

.board-head__actions {
  display: flex;
  align-items: center;
  gap: var(--ui-space-3);
}

.board-empty {
  padding: 0;
}

/* ---------- 总结 ---------- */
.summary {
  padding: var(--ui-space-5);
}

.summary__body {
  display: flex;
  flex-direction: column;
  gap: var(--ui-space-4);
}

.summary__text {
  margin: 0;
  font-size: var(--ui-text-base);
  line-height: var(--ui-line-relaxed);
  color: var(--ui-fg-primary);
  white-space: pre-wrap;
  word-break: break-word;
}

.summary__columns {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: var(--ui-space-4);
}

.summary__block {
  padding: var(--ui-space-3) var(--ui-space-4);
  border: 1px solid var(--ui-border);
  border-radius: var(--ui-radius-md);
  background: var(--ui-bg-sunken);
}

.summary__label {
  display: block;
  margin-bottom: var(--ui-space-2);
  font-size: var(--ui-text-xs);
  font-weight: 600;
  color: var(--ui-fg-secondary);
}

.summary__list {
  margin: 0;
  padding-left: var(--ui-space-5);
  font-size: var(--ui-text-sm);
  line-height: var(--ui-line-relaxed);
  color: var(--ui-fg-primary);
}

.summary__list--warn {
  color: var(--ui-fg-secondary);
}

/* ---------- 题目 ---------- */
.question {
  display: flex;
  flex-direction: column;
  gap: var(--ui-space-4);
  padding: var(--ui-space-5);
}

.question__head {
  display: flex;
  align-items: center;
  gap: var(--ui-space-2);
}

.question__hint {
  font-size: var(--ui-text-xs);
  color: var(--ui-fg-muted);
}

.question__text {
  margin: 0;
  font-size: var(--ui-text-lg);
  font-weight: 600;
  line-height: var(--ui-line-tight);
  color: var(--ui-fg-primary);
  word-break: break-word;
}

.option-list {
  display: flex;
  flex-direction: column;
  gap: var(--ui-space-2);
}

.option-item {
  display: grid;
  grid-template-columns: 20px 28px minmax(0, 1fr);
  align-items: center;
  gap: var(--ui-space-3);
  padding: var(--ui-space-3);
  border: 1px solid var(--ui-border);
  border-radius: var(--ui-radius-md);
  background: var(--ui-bg-surface);
  cursor: pointer;
  transition: border-color var(--ui-duration-fast) var(--ui-ease-in-out),
    background var(--ui-duration-fast) var(--ui-ease-in-out);
}

.option-item:hover {
  background: var(--ui-bg-hover);
}

.option-item:focus-within {
  border-color: var(--ui-border-focus);
  box-shadow: var(--ui-ring);
}

.option-item.is-selected {
  border-color: var(--ui-accent-border);
  background: var(--ui-bg-selected);
}

.option-item__key {
  font-size: var(--ui-text-sm);
  font-weight: 700;
  color: var(--ui-fg-primary);
}

.option-item__text {
  font-size: var(--ui-text-base);
  color: var(--ui-fg-primary);
  word-break: break-word;
}

/* 底部固定输入区：作答区始终可见 */
.question__actions {
  position: sticky;
  bottom: 0;
  z-index: var(--ui-z-sticky);
  display: flex;
  align-items: center;
  gap: var(--ui-space-3);
  margin-top: var(--ui-space-2);
  padding: var(--ui-space-3) 0 0;
  border-top: 1px solid var(--ui-border);
  background: var(--ui-bg-surface);
}

.question__tip {
  font-size: var(--ui-text-xs);
  color: var(--ui-fg-muted);
}

/* ---------- 已回答轮次 ---------- */
.rounds {
  display: flex;
  flex-direction: column;
  padding: var(--ui-space-5);
}

.round-list {
  display: flex;
  flex-direction: column;
  gap: var(--ui-space-3);
  max-height: 420px;
  min-height: 0;
}

.round-item {
  display: flex;
  flex-direction: column;
  gap: var(--ui-space-3);
  padding: var(--ui-space-4);
  border: 1px solid var(--ui-border);
  border-radius: var(--ui-radius-md);
  background: var(--ui-bg-sunken);
}

.round-item__head {
  display: flex;
  align-items: center;
  gap: var(--ui-space-2);
}

.round-item__score {
  margin-left: auto;
  font-size: var(--ui-text-sm);
  font-weight: 600;
  color: var(--ui-fg-accent);
}

.round-item__question {
  margin: 0;
  font-size: var(--ui-text-base);
  font-weight: 600;
  color: var(--ui-fg-primary);
  word-break: break-word;
}

.round-item__block {
  display: flex;
  flex-direction: column;
  gap: var(--ui-space-1);
}

.round-item__label {
  font-size: var(--ui-text-xs);
  font-weight: 600;
  color: var(--ui-fg-muted);
}

.round-item__text {
  margin: 0;
  font-size: var(--ui-text-sm);
  line-height: var(--ui-line-relaxed);
  color: var(--ui-fg-secondary);
  white-space: pre-wrap;
  word-break: break-word;
}

.round-item__reason {
  margin: 0;
  font-size: var(--ui-text-xs);
  line-height: var(--ui-line-normal);
  color: var(--ui-fg-muted);
}

@media (max-width: 1100px) {
  .interview-layout,
  .setup__grid,
  .summary__columns {
    grid-template-columns: 1fr;
  }

  .session-panel {
    max-height: 320px;
  }

  .page-head {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
