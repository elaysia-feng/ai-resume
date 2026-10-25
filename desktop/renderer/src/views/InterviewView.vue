<template>
  <div class="interview-page">
    <header class="page-head">
      <div class="page-head__text">
        <h1 class="ui-page-title">模拟面试</h1>
        <p class="ui-page-subtitle">基于当前后端的 interview run、board 和 question-rounds 接口，逐轮作答并查看结构化反馈。</p>
      </div>
      <div class="page-head__actions">
        <span v-if="runBadge" class="ui-badge" :class="runBadge.tone">
          <AppIcon :name="runBadge.icon" :size="12" :spin="runBadge.spin" />
          {{ runBadge.label }}
        </span>
        <button v-if="selectedRunId" type="button" class="ui-btn ui-btn--primary ui-btn--sm" @click="showSetup = !showSetup">
          <AppIcon :name="showSetup ? 'x' : 'plus'" :size="15" />
          {{ showSetup ? '收起新面试' : '开始新面试' }}
        </button>
        <button class="ui-btn ui-btn--outline" :disabled="loadingSessions" @click="loadInterviewSessions">
          <AppIcon name="arrows-clockwise" :size="16" :spin="loadingSessions" />
          {{ loadingSessions ? '刷新中…' : '刷新会话' }}
        </button>
      </div>
    </header>

    <!-- 新建面试 -->
    <section v-if="showSetup || !selectedRunId" class="ui-panel setup">
      <header class="ui-section-head setup__head">
        <div>
          <h2 class="ui-section-head__title">开始新的模拟面试</h2>
          <p class="ui-section-head__hint">选择一份简历开始。岗位 JD 可选，用来让题目更贴近目标方向。</p>
        </div>
      </header>
      <div class="setup__grid">
        <div class="field">
          <label class="ui-label" for="setup-resume">选择简历</label>
          <select id="setup-resume" v-model="createForm.resumeId" class="ui-input ui-select" required>
            <option value="">请选择简历</option>
            <option v-for="resume in resumes" :key="resume.id" :value="resume.id">{{ resume.title }}</option>
          </select>
        </div>
        <div class="field">
          <label class="ui-label" for="setup-title">会话标题</label>
          <input
            id="setup-title"
            ref="setupTitleRef"
            v-model="createForm.sessionTitle"
            class="ui-input"
            type="text"
            placeholder="例如：Java 后端一面"
          />
        </div>
        <div class="field field--wide">
          <label class="ui-label" for="setup-jd">岗位 JD</label>
          <textarea
            id="setup-jd"
            v-model="createForm.jobDescription"
            class="ui-input ui-textarea"
            rows="3"
            placeholder="可粘贴目标岗位要求；不填写时会按简历内容出题"
          ></textarea>
        </div>
      </div>
      <div class="setup__actions">
        <p v-if="!loadingSessions && !resumes.length" class="ui-callout ui-callout--accent" role="status">
          先创建一份简历，面试才能结合你的经历生成问题。
          <router-link class="ui-btn ui-btn--outline ui-btn--sm" to="/resume/create">新建简历</router-link>
        </p>
        <button class="ui-btn ui-btn--primary" :disabled="creating || !createForm.resumeId" @click="handleCreateAndStart">
          <AppIcon name="play" :size="16" />
          {{ creating ? '启动中…' : '新建并开始面试' }}
        </button>
      </div>
    </section>

    <p v-if="message" class="ui-callout ui-callout--accent notice" role="status" aria-live="polite">
      <AppIcon name="check-circle" :size="16" />
      <span>{{ message }}</span>
    </p>
    <p v-if="error" class="ui-callout ui-callout--danger notice" role="alert">
      <AppIcon name="warning-circle" :size="16" />
      <span>{{ error }}</span>
    </p>

    <div class="interview-layout">
      <!-- 面试会话列表 -->
      <aside class="ui-panel session-panel">
        <header class="ui-section-head session-panel__head">
          <h2 class="ui-section-head__title">面试会话</h2>
          <span class="ui-badge">{{ sessions.length }}</span>
        </header>
        <div class="session-list ui-scroll">
          <div v-if="loadingSessions" class="session-skeletons">
            <div v-for="index in 3" :key="index" class="ui-skeleton session-skeleton"></div>
          </div>

          <div v-else-if="sessions.length === 0" class="ui-empty session-empty">
            <span class="ui-empty__icon">
              <AppIcon name="microphone" :size="24" />
            </span>
            <p class="ui-empty__title">暂无面试会话</p>
            <p class="ui-empty__hint">新建并开始面试后，这里会列出全部面试记录。</p>
          </div>

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
              <span class="ui-badge" :class="sessionBadge(session).tone">
                <AppIcon :name="sessionBadge(session).icon" :size="12" :spin="sessionBadge(session).spin" />
                {{ sessionBadge(session).label }}
              </span>
              <span class="session-item__time">{{ formatDateTime(session.updatedAt || session.lastMessageAt) }}</span>
            </span>
          </button>
        </div>
      </aside>

      <!-- 面试看板 -->
      <section class="board">
        <div v-if="!selectedRunId" class="ui-panel board-empty">
          <div class="ui-empty">
            <span class="ui-empty__icon">
              <AppIcon name="microphone" :size="24" />
            </span>
            <p class="ui-empty__title">还没有进行中的面试</p>
            <p class="ui-empty__hint">在上方选择简历并填写岗位 JD，点击「新建并开始面试」即可开始第一轮问答。</p>
            <button class="ui-btn ui-btn--primary" type="button" @click="focusSetup">去填写面试信息</button>
          </div>
        </div>

        <template v-else>
          <header class="ui-panel board-head">
            <div class="board-head__text">
              <h2 class="board-head__title">{{ selectedSessionTitle || '模拟面试记录' }}</h2>
              <p class="board-head__meta">状态：{{ board?.status || '未加载' }}</p>
              <span v-if="loadingBoard" class="board-head__loading">
                <AppIcon name="spinner-gap" :size="12" spin />
                加载中…
              </span>
            </div>
            <div class="board-head__actions">
              <button class="ui-btn ui-btn--outline ui-btn--sm" :disabled="loadingBoard" @click="refreshBoard">
                <AppIcon name="arrows-clockwise" :size="14" :spin="loadingBoard" />
                {{ loadingBoard ? '刷新中…' : '刷新当前题' }}
              </button>
            </div>
          </header>

          <!-- 面试进行状态：让「排队中 / 进行中 / 等待回答」有存在感 -->
          <div v-if="runBadge" class="run-strip" :class="runStripTone">
            <span class="run-strip__icon">
              <AppIcon :name="runBadge.icon" :size="16" :spin="runBadge.spin" />
            </span>
            <span class="run-strip__label">{{ runBadge.label }}</span>
            <span class="run-strip__hint">{{ runHint }}</span>
          </div>

          <p v-if="board?.errorMessage" class="ui-callout ui-callout--danger notice">
            <AppIcon name="warning-circle" :size="16" />
            <span>{{ board.errorMessage }}</span>
          </p>

          <!-- 面试总结：总分 / 亮点 / 待提升 -->
          <section v-if="summaryView" class="ui-panel summary">
            <header class="ui-section-head">
              <h2 class="ui-section-head__title">面试总结</h2>
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
          <section v-if="currentQuestion" class="ui-panel question">
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
                class="ui-input ui-textarea question__textarea"
                rows="4"
                placeholder="可以补充你的选择理由或自由回答"
              ></textarea>
            </div>

            <!-- 底部固定输入区 -->
            <div class="question__actions">
              <button class="ui-btn ui-btn--primary" :disabled="submitting" @click="handleSubmitAnswer">
                <AppIcon name="paper-plane-tilt" :size="16" />
                {{ submitting ? '提交中…' : '提交答案' }}
              </button>
              <span class="question__tip">提交后自动进入下一题</span>
            </div>
          </section>
          <div v-else class="ui-panel board-empty">
            <div class="ui-empty">
              <span class="ui-empty__icon">
                <AppIcon name="question" :size="24" />
              </span>
              <p class="ui-empty__title">当前没有待回答题目</p>
              <p class="ui-empty__hint">可能正在出题，稍等几秒后点「刷新当前题」即可。</p>
              <button class="ui-btn ui-btn--outline" type="button" :disabled="loadingBoard" @click="refreshBoard">
                <AppIcon name="arrows-clockwise" :size="16" :spin="loadingBoard" />
                刷新当前题
              </button>
            </div>
          </div>

          <!-- 已回答轮次 -->
          <section class="ui-panel rounds">
            <header class="ui-section-head">
              <h2 class="ui-section-head__title">已回答轮次</h2>
              <button class="ui-btn ui-btn--ghost ui-btn--sm" :disabled="loadingRounds" @click="loadRounds">
                <AppIcon name="arrows-clockwise" :size="14" :spin="loadingRounds" />
                {{ loadingRounds ? '加载中…' : '刷新历史' }}
              </button>
            </header>
            <div class="round-list ui-scroll">
              <div v-if="loadingRounds" class="round-skeletons">
                <div v-for="index in 2" :key="index" class="ui-skeleton round-skeleton"></div>
              </div>

              <div v-else-if="rounds.length === 0" class="ui-empty rounds-empty">
                <span class="ui-empty__icon">
                  <AppIcon name="clock-counter-clockwise" :size="24" />
                </span>
                <p class="ui-empty__title">暂无已回答记录</p>
                <p class="ui-empty__hint">提交第一题后，这里会保留每一轮的作答与评价。</p>
              </div>

              <article v-for="round in rounds" :key="round.roundId" class="round-item">
                <header class="round-item__head">
                  <span class="ui-badge ui-badge--accent">第 {{ round.roundNo }} 轮</span>
                  <span class="ui-badge" :class="roundBadge(round).tone">
                    <AppIcon :name="roundBadge(round).icon" :size="12" />
                    {{ roundBadge(round).label }}
                  </span>
                  <span v-if="roundScore(round) !== null" class="round-item__score">
                    <span class="round-item__stars">{{ starText(roundScore(round)) }}</span>
                    <span>{{ roundScore(round) }} 分</span>
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
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue';
import { useRoute } from 'vue-router';
import {
  createAgentSession,
  extractApiMessage,
  getInterviewBoard,
  getInterviewQuestionRounds,
  getResumes,
  listAgentSessionRuns,
  listAgentSessions,
  startInterviewRun,
  submitInterviewAnswer,
} from '../api/index.js';
import { formatDateTime } from '../utils/resume.js';
import AppIcon from '../components/common/AppIcon.vue';

const POLL_INTERVAL_MS = 2500;
const route = useRoute();

// run / 会话 / 轮次的状态文案与徽标色调（icon 用于界面上的状态标识）
const RUN_STATUS_VIEW = {
  QUEUED: { label: '排队中', tone: '', icon: 'hourglass-medium', spin: false },
  RUNNING: { label: '进行中', tone: 'ui-badge--accent', icon: 'spinner-gap', spin: true },
  WAITING_USER: { label: '等待回答', tone: 'ui-badge--warning', icon: 'microphone', spin: false },
  SUCCESS: { label: '已结束', tone: 'ui-badge--success', icon: 'check-circle', spin: false },
  FAILED: { label: '执行失败', tone: 'ui-badge--danger', icon: 'x-circle', spin: false },
  CANCELLED: { label: '已取消', tone: '', icon: 'minus', spin: false },
};

// 状态条上的一句提示：说清「现在在做什么」
const RUN_STATUS_HINT = {
  QUEUED: '题目准备中，稍等几秒',
  RUNNING: '正在生成题目，稍等几秒',
  WAITING_USER: '请在下方选择答案并提交',
  SUCCESS: '本场面试已结束，可回看每轮评价',
  FAILED: '本次面试执行失败，可新建一场重试',
  CANCELLED: '本次面试已取消',
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
const selectedSessionTitle = ref('');
const showSetup = ref(true);
const board = ref(null);
const rounds = ref([]);
const activeRoundId = ref(null);
const setupTitleRef = ref(null);
let pollTimer = null;

const createForm = reactive({
  resumeId: '',
  sessionTitle: '模拟面试',
  jobDescription: '',
});

const answerForm = reactive({
  selectedOption: '',
  supplementText: '',
});

watch(() => route.query.sessionId, async (sessionId) => {
  if (!sessionId) {
    return;
  }
  const loaded = await loadInterviewSessions();
  if (!loaded) {
    return;
  }
  const requestedSession = sessions.value.find((session) => String(session.id) === String(sessionId));
  if (requestedSession) {
    await selectSession(requestedSession);
    return;
  }
  clearSelectedSession();
  setStatus('', '找不到这条面试记录。它可能已删除，请从会话列表重新选择。');
});

const currentQuestion = computed(() => board.value?.currentQuestion || null);

const runBadge = computed(() => {
  if (!board.value?.status) {
    return null;
  }
  return RUN_STATUS_VIEW[board.value.status] || { label: board.value.status, tone: '', icon: 'info', spin: false };
});

const runHint = computed(() => RUN_STATUS_HINT[board.value?.status] || '');

// 状态条配色：排队 / 进行中 / 等待回答 / 已结束 / 失败
const runStripTone = computed(() => {
  const status = board.value?.status || '';
  if (status === 'WAITING_USER') {
    return 'is-waiting';
  }
  if (status === 'SUCCESS') {
    return 'is-success';
  }
  if (status === 'FAILED') {
    return 'is-danger';
  }
  if (status === 'QUEUED' || status === 'RUNNING') {
    return 'is-live';
  }
  return '';
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
  const [, sessionsLoaded] = await Promise.all([loadResumes(), loadInterviewSessions()]);
  const requestedSessionId = String(route.query.sessionId || '');
  const requestedSession = sessions.value.find((session) => String(session.id) === requestedSessionId);
  if (requestedSession) {
    createForm.resumeId = requestedSession.resumeId ? String(requestedSession.resumeId) : createForm.resumeId;
    createForm.sessionTitle = requestedSession.sessionTitle ? `${requestedSession.sessionTitle} · 新一轮练习` : createForm.sessionTitle;
    createForm.jobDescription = requestedSession.jobDescription || createForm.jobDescription;
    await selectSession(requestedSession);
  } else if (requestedSessionId && sessionsLoaded) {
    clearSelectedSession();
    setStatus('', '找不到这条面试记录。它可能已删除，请从会话列表重新选择。');
  } else {
    if (route.query.resumeId) {
      createForm.resumeId = String(route.query.resumeId);
    }
    if (route.query.sessionTitle) {
      createForm.sessionTitle = String(route.query.sessionTitle);
    }
    if (route.query.jobDescription) {
      createForm.jobDescription = String(route.query.jobDescription);
    }
  }
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
    if (route.query.resumeId) {
      createForm.resumeId = String(route.query.resumeId);
    } else if (!createForm.resumeId && resumes.value.length) {
      createForm.resumeId = String(resumes.value[0].id);
    }
  } catch (err) {
    setStatus('', extractApiMessage(err, '加载简历列表失败'));
  }
}

async function loadInterviewSessions() {
  loadingSessions.value = true;
  try {
    sessions.value = await listAgentSessions({ sceneCode: 'INTERVIEW' });
    if (!route.query.sessionId && !selectedRunId.value && sessions.value.length) {
      const firstSession = sessions.value.find((session) => session.activeRunId) || sessions.value[0];
      await selectSession(firstSession);
    }
    return true;
  } catch (err) {
    setStatus('', extractApiMessage(err, '加载面试会话失败'));
    return false;
  } finally {
    loadingSessions.value = false;
  }
}

function clearSelectedSession() {
  stopPolling();
  selectedRunId.value = null;
  selectedSessionTitle.value = '';
  board.value = null;
  rounds.value = [];
  activeRoundId.value = null;
  showSetup.value = true;
}

async function handleCreateAndStart() {
  if (!createForm.resumeId) {
    setStatus('', '请选择一份简历后再开始面试。');
    return;
  }
  creating.value = true;
  setStatus();
  try {
    const session = await createAgentSession({
      resumeId: Number(createForm.resumeId),
      sceneCode: 'INTERVIEW',
      sessionTitle: createForm.sessionTitle || '模拟面试',
      jobDescription: createForm.jobDescription || null,
    });
    const runId = await startInterviewRun({ sessionId: session.id });
    selectedRunId.value = runId;
    selectedSessionTitle.value = createForm.sessionTitle || '模拟面试';
    showSetup.value = false;
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
  stopPolling();
  setStatus();
  selectedSessionTitle.value = session.sessionTitle || `面试会话 #${session.id}`;
  try {
    // 活跃 run 已由会话列表批量返回；否则只取最新一条面试历史。
    let runId = session.activeRunId;
    if (!runId) {
      const runs = await listAgentSessionRuns(session.id, { sceneCode: 'INTERVIEW', limit: 1 });
      runId = Array.isArray(runs) ? runs[0]?.runId : null;
    }
    if (!runId) {
      selectedRunId.value = null;
      board.value = null;
      rounds.value = [];
      showSetup.value = true;
      setStatus('', '这条会话还没有面试记录，可以使用上方表单开始练习。');
      return;
    }
    selectedRunId.value = runId;
    showSetup.value = false;
    await refreshBoard();
    await loadRounds();
    if (shouldKeepPolling(board.value?.status)) {
      startPolling();
    }
  } catch (err) {
    selectedRunId.value = null;
    board.value = null;
    rounds.value = [];
    showSetup.value = true;
    setStatus('', extractApiMessage(err, '加载面试历史失败'));
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

// 空态引导：把焦点送回「新建面试」的表单
function focusSetup() {
  setupTitleRef.value?.focus();
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
    return { label: '进行中', tone: 'ui-badge--accent', icon: 'spinner-gap', spin: true };
  }
  if (session.status === 'ARCHIVED') {
    return { label: '已归档', tone: '', icon: 'note-blank', spin: false };
  }
  return { label: '打开记录', tone: '', icon: 'clock', spin: false };
}

function roundBadge(round) {
  if (round.status === 'ANSWERED') {
    return { label: '已作答', tone: 'ui-badge--success', icon: 'check-circle' };
  }
  return { label: round.status || '未知', tone: '', icon: 'info' };
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

.page-head__text {
  min-width: 0;
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
}

/* ---------- 新建面试 ---------- */
.setup {
  padding: var(--ui-space-5);
}

.setup__head {
  margin-bottom: var(--ui-space-5);
}

.setup__grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--ui-space-4);
}

.setup__actions {
  display: flex;
  justify-content: flex-end;
  margin-top: var(--ui-space-5);
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

.session-panel__head {
  margin-bottom: 0;
}

.session-list {
  display: flex;
  flex-direction: column;
  gap: var(--ui-space-2);
  min-height: 0;
}

/* 会话列表加载态 */
.session-skeletons {
  display: flex;
  flex-direction: column;
  gap: var(--ui-space-2);
}

.session-skeleton {
  height: 58px;
  border-radius: var(--ui-radius-md);
}

.session-empty {
  gap: var(--ui-space-2);
  padding: var(--ui-space-6) var(--ui-space-3);
}

.session-empty :deep(.ui-empty__title) {
  font-size: var(--ui-text-sm);
}

.session-empty :deep(.ui-empty__hint) {
  font-size: var(--ui-text-xs);
  line-height: var(--ui-line-normal);
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

.board-head__text {
  display: flex;
  flex-wrap: wrap;
  align-items: baseline;
  gap: var(--ui-space-2) var(--ui-space-3);
  min-width: 0;
}

.board-head__title {
  margin: 0;
  font-size: var(--ui-text-lg);
  font-weight: 600;
  letter-spacing: var(--ui-tracking-snug);
  color: var(--ui-fg-primary);
}

.board-head__meta {
  margin: 0;
  font-size: var(--ui-text-sm);
  color: var(--ui-fg-secondary);
}

.board-head__loading {
  display: inline-flex;
  align-items: center;
  gap: var(--ui-space-1);
  font-size: var(--ui-text-xs);
  color: var(--ui-fg-muted);
}

.board-head__actions {
  display: flex;
  align-items: center;
  gap: var(--ui-space-3);
  flex: none;
}

.board-empty {
  padding: 0;
}

/* ---------- 面试状态条 ---------- */
.run-strip {
  display: flex;
  align-items: center;
  gap: var(--ui-space-3);
  padding: var(--ui-space-3) var(--ui-space-4);
  border: 1px solid var(--ui-border);
  border-radius: var(--ui-radius-lg);
  background: var(--ui-bg-sunken);
  color: var(--ui-fg-secondary);
}

.run-strip__icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  width: 32px;
  height: 32px;
  border-radius: var(--ui-radius-md);
  background: var(--ui-bg-surface);
  border: 1px solid var(--ui-border);
  color: var(--ui-fg-secondary);
}

.run-strip__label {
  font-size: var(--ui-text-base);
  font-weight: 600;
  color: var(--ui-fg-primary);
  white-space: nowrap;
}

.run-strip__hint {
  font-size: var(--ui-text-sm);
  color: var(--ui-fg-secondary);
  word-break: break-word;
}

/* 进行中：底色 + 品牌色图标，让状态有存在感 */
.run-strip.is-live {
  background: var(--ui-accent-soft);
  border-color: var(--ui-accent-border);
}

.run-strip.is-live .run-strip__icon {
  background: var(--ui-bg-surface);
  border-color: var(--ui-accent-border);
  color: var(--ui-fg-accent);
}

.run-strip.is-live .run-strip__label {
  color: var(--ui-fg-accent);
}

/* 等待回答：状态最强，等用户动作 */
.run-strip.is-waiting {
  background: var(--ui-accent-soft);
  border-color: var(--ui-accent);
  box-shadow: var(--ui-shadow-accent);
}

.run-strip.is-waiting .run-strip__icon {
  background: var(--ui-accent);
  border-color: var(--ui-accent);
  color: var(--ui-accent-fg);
}

.run-strip.is-waiting .run-strip__label {
  color: var(--ui-fg-accent);
}

.run-strip.is-success {
  background: var(--ui-success-soft);
  border-color: var(--ui-success-border);
}

.run-strip.is-success .run-strip__icon {
  color: var(--ui-success);
}

.run-strip.is-danger {
  background: var(--ui-danger-soft);
  border-color: var(--ui-danger-border);
}

.run-strip.is-danger .run-strip__icon {
  color: var(--ui-danger);
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
  letter-spacing: var(--ui-tracking-snug);
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
  grid-template-columns: 16px 26px minmax(0, 1fr);
  align-items: center;
  gap: var(--ui-space-3);
  padding: var(--ui-space-3) var(--ui-space-4);
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

.option-item input[type='radio'] {
  accent-color: var(--ui-accent);
}

.option-item__key {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 26px;
  height: 26px;
  border: 1px solid var(--ui-border);
  border-radius: var(--ui-radius-sm);
  background: var(--ui-bg-sunken);
  font-size: var(--ui-text-sm);
  font-weight: 700;
  color: var(--ui-fg-primary);
}

.option-item.is-selected .option-item__key {
  border-color: var(--ui-accent-border);
  background: var(--ui-accent-soft);
  color: var(--ui-fg-accent);
}

.option-item__text {
  font-size: var(--ui-text-base);
  color: var(--ui-fg-primary);
  word-break: break-word;
}

/* 作答区留出宽敞的书写空间 */
.question__textarea {
  min-height: 140px;
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

/* 历史记录加载态 */
.round-skeletons {
  display: flex;
  flex-direction: column;
  gap: var(--ui-space-3);
}

.round-skeleton {
  height: 132px;
  border-radius: var(--ui-radius-md);
}

.rounds-empty {
  gap: var(--ui-space-2);
  padding: var(--ui-space-6) var(--ui-space-3);
}

.rounds-empty :deep(.ui-empty__title) {
  font-size: var(--ui-text-sm);
}

.rounds-empty :deep(.ui-empty__hint) {
  font-size: var(--ui-text-xs);
  line-height: var(--ui-line-normal);
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
  display: inline-flex;
  align-items: center;
  gap: var(--ui-space-2);
  margin-left: auto;
  font-size: var(--ui-text-sm);
  font-weight: 600;
  color: var(--ui-fg-accent);
  font-variant-numeric: tabular-nums;
}

.round-item__stars {
  letter-spacing: var(--ui-tracking-normal);
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
