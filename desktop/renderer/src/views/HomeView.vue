<template>
  <div class="home-page">
    <header class="home-heading">
      <div>
        <p class="home-eyebrow">AI RESUME FORGE</p>
        <h1 class="ui-page-title">{{ greeting }}今天从哪里开始？</h1>
        <p class="ui-page-subtitle">简历、AI 对话和模拟面试都可以单独开始。这里汇总最近工作，方便你接着做。</p>
      </div>
      <button type="button" class="ui-btn ui-btn--primary" :disabled="creatingResume" @click="createResume">
        <AppIcon :name="creatingResume ? 'spinner-gap' : 'file-plus'" :size="16" :spin="creatingResume" />
        {{ creatingResume ? '创建中…' : '新建简历' }}
      </button>
    </header>

    <div v-if="creationNotice" class="ui-callout ui-callout--danger" role="alert">
      <AppIcon name="warning-circle" :size="16" />
      <span>{{ creationNotice }}</span>
      <button type="button" class="ui-btn ui-btn--ghost ui-btn--sm" @click="creationNotice = ''">关闭</button>
    </div>

    <section class="entry-grid ui-panel ui-panel--raised" aria-label="主要功能">
      <article class="entry-card entry-card--resume">
        <div class="entry-card__top">
          <span class="ui-icon-tile"><AppIcon name="file-text" :size="18" /></span>
          <span class="ui-badge">{{ resumeError ? '—' : resumes.length }}<span v-if="!resumeError"> 份</span></span>
        </div>
        <div class="entry-card__copy">
          <h2>简历编辑</h2>
          <p>维护经历、调整版式，查看预览并导出。</p>
        </div>
        <div v-if="loadingResumes && !resumes.length" class="entry-card__state" aria-live="polite">正在读取简历…</div>
        <div v-else-if="resumeError" class="entry-card__state entry-card__state--error" role="alert">
          <span>{{ resumeError }}</span>
          <button type="button" class="ui-btn ui-btn--ghost ui-btn--sm" @click="loadResumes">重试</button>
        </div>
        <div v-else-if="resumes.length" class="entry-card__state">已有 {{ resumes.length }} 份简历，可在下方列表中继续编辑。</div>
        <div v-else class="entry-card__state">还没有简历，创建一份空白简历开始整理经历。</div>
        <footer class="entry-card__actions">
          <button type="button" class="ui-btn ui-btn--outline ui-btn--sm" @click="router.push('/dashboard/templates')">浏览模板</button>
          <button v-if="resumes.length" type="button" class="ui-btn ui-btn--ghost ui-btn--sm" @click="openResumeList">查看简历</button>
        </footer>
      </article>

      <article class="entry-card entry-card--chat">
        <div class="entry-card__top">
          <span class="ui-icon-tile"><AppIcon name="chat-circle-dots" :size="18" /></span>
          <span class="ui-badge">{{ chatError ? '—' : chatSessions.length }}<span v-if="!chatError"> 条</span></span>
        </div>
        <div class="entry-card__copy">
          <h2>AI 对话</h2>
          <p>讨论简历内容，也可以让 AI 查看本地工作区文件。</p>
        </div>
        <div v-if="loadingChats && !chatSessions.length" class="entry-card__state" aria-live="polite">正在读取对话…</div>
        <div v-else-if="chatError" class="entry-card__state entry-card__state--error" role="alert">
          <span>{{ chatError }}</span>
          <button type="button" class="ui-btn ui-btn--ghost ui-btn--sm" @click="loadChats">重试</button>
        </div>
        <div v-else-if="chatSessions.length" class="entry-card__state">已保存 {{ chatSessions.length }} 条记录；最近对话见下方列表。</div>
        <div v-else class="entry-card__state">还没有对话记录，可以直接开启新对话。</div>
        <footer class="entry-card__actions">
          <button type="button" class="ui-btn ui-btn--outline ui-btn--sm" @click="openChat()">开始对话</button>
        </footer>
      </article>

      <article class="entry-card entry-card--interview">
        <div class="entry-card__top">
          <span class="ui-icon-tile"><AppIcon name="microphone" :size="18" /></span>
          <span class="ui-badge">{{ agentError ? '—' : interviewSessions.length }}<span v-if="!agentError"> 场</span></span>
        </div>
        <div class="entry-card__copy">
          <h2>模拟面试</h2>
          <p>选择一份简历和目标岗位，逐轮练习并查看反馈。</p>
        </div>
        <div v-if="loadingAgents && !interviewSessions.length" class="entry-card__state" aria-live="polite">正在读取面试…</div>
        <div v-else-if="agentError" class="entry-card__state entry-card__state--error" role="alert">
          <span>{{ agentError }}</span>
          <button type="button" class="ui-btn ui-btn--ghost ui-btn--sm" @click="loadAgentSessions">重试</button>
        </div>
        <div v-else-if="interviewSessions.length" class="entry-card__state">已保存 {{ interviewSessions.length }} 场面试；最近记录见下方列表。</div>
        <div v-else class="entry-card__state">还没有面试记录，准备好后可以从这里开始练习。</div>
        <footer class="entry-card__actions">
          <button type="button" class="ui-btn ui-btn--outline ui-btn--sm" @click="openInterview()">开始面试</button>
        </footer>
      </article>
    </section>

    <section class="overview-grid">
      <article class="ui-panel overview-panel">
        <header class="ui-section-head">
          <div>
            <h2 class="ui-section-head__title">最近工作</h2>
            <p class="ui-section-head__hint">按最近活动排序，点开后回到对应功能。</p>
          </div>
          <router-link class="ui-btn ui-btn--ghost ui-btn--sm" to="/dashboard/history">全部历史</router-link>
        </header>

        <div v-if="activityError" class="ui-callout ui-callout--danger" role="alert">
          <AppIcon name="warning-circle" :size="16" />
          <span>{{ activityError }}</span>
          <button type="button" class="ui-btn ui-btn--sm ui-btn--outline" @click="reloadActivity">重新加载</button>
        </div>
        <ul v-if="recentActivity.length" class="activity-list">
          <li v-for="item in recentActivity" :key="item.key">
            <button type="button" class="activity-item" @click="item.open()">
              <span class="activity-item__icon"><AppIcon :name="item.icon" :size="16" /></span>
              <span class="activity-item__body">
                <span class="activity-item__title">{{ item.title }}</span>
                <span class="activity-item__meta">{{ item.kind }} · {{ formatDateTime(item.time) }}</span>
              </span>
              <span v-if="item.status" class="ui-badge" :class="item.statusTone">{{ item.status }}</span>
              <AppIcon name="caret-right" :size="16" class="activity-item__arrow" />
            </button>
          </li>
        </ul>
        <div v-else-if="loadingAgents || loadingChats" class="activity-empty" aria-live="polite">正在整理最近工作…</div>
        <div v-else-if="!activityError" class="ui-empty activity-empty">
          <span class="ui-empty__icon"><AppIcon name="clock-counter-clockwise" :size="24" /></span>
          <p class="ui-empty__title">这里还没有近期记录</p>
          <p class="ui-empty__hint">开始一次简历编辑、AI 对话或模拟面试后，记录会出现在这里。</p>
        </div>
      </article>

    </section>

    <section class="ui-panel resume-library" id="resume-list">
      <header class="ui-section-head">
        <div>
          <h2 class="ui-section-head__title">我的简历</h2>
          <p class="ui-section-head__hint">每份简历可以独立编辑、保存版本和导出。</p>
        </div>
        <span class="ui-badge">{{ resumeError ? '—' : resumes.length }}<span v-if="!resumeError"> 份</span></span>
      </header>
      <div v-if="resumes.length" class="resume-library__grid">
        <ResumeCard v-for="resume in resumes" :key="resume.id" :resume="resume" />
      </div>
      <div v-else-if="!loadingResumes && !resumeError" class="ui-empty resume-library__empty">
        <span class="ui-empty__icon"><AppIcon name="files" :size="24" /></span>
        <p class="ui-empty__title">还没有简历</p>
        <p class="ui-empty__hint">新建空白简历后，可以按自己的经历逐步补充内容。</p>
        <button type="button" class="ui-btn ui-btn--primary" :disabled="creatingResume" @click="createResume">新建简历</button>
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import AppIcon from '../components/common/AppIcon.vue';
import { createResume as createResumeApi, getResumes } from '../api/resumes.js';
import { listChatSessions } from '../api/chat.js';
import { listAgentSessions } from '../api/agentSessions.js';
import { useAuthStore } from '../store/authStore.js';
import { formatDateTime } from '../utils/resume.js';
import { extractApiMessage } from '../api/request.js';
import ResumeCard from '../components/ResumeCard.vue';

const router = useRouter();
const route = useRoute();
const authStore = useAuthStore();
const resumes = ref([]);
const chatSessions = ref([]);
const agentSessions = ref([]);
const loadingResumes = ref(false);
const loadingChats = ref(false);
const loadingAgents = ref(false);
const creatingResume = ref(false);
const resumeError = ref('');
const chatError = ref('');
const agentError = ref('');
const creationNotice = ref('');

watch(() => route.query.resumeCreateError, (code) => {
  if (!code) {
    return;
  }
  creationNotice.value = code === 'missing-id'
    ? '创建简历失败：后端没有返回简历编号。'
    : '创建简历失败，请检查后端服务后重试。';
  router.replace({ query: { ...route.query, resumeCreateError: undefined } });
}, { immediate: true });

const greeting = computed(() => {
  const name = authStore.currentUser?.username;
  return name ? `${name}，` : '';
});

const interviewSessions = computed(() =>
  agentSessions.value.filter((session) => String(session.sceneCode || '').toUpperCase() === 'INTERVIEW')
);

const recentActivity = computed(() => {
  const chats = chatSessions.value.slice(0, 4).map((session) => ({
    key: `chat-${session.id}`,
    title: session.title || `对话 #${session.id}`,
    kind: 'AI 对话',
    time: session.lastMessageAt || session.updatedAt,
    icon: 'chat-circle',
    open: () => openChat(session.id),
  }));
  const agents = agentSessions.value.slice(0, 6).map((session) => {
    const isInterview = String(session.sceneCode || '').toUpperCase() === 'INTERVIEW';
    const runStatus = session.activeRunStatus || session.latestRunStatus;
    return {
      key: `agent-${session.id}`,
      title: session.sessionTitle || (isInterview ? `面试 #${session.id}` : `简历任务 #${session.id}`),
      kind: isInterview ? '模拟面试' : '简历 AI',
      time: session.lastMessageAt || session.updatedAt || session.createdAt,
      icon: isInterview ? 'microphone' : 'sparkle',
      status: runStatus ? runStatusLabel(runStatus) : '',
      statusTone: runStatusTone(runStatus),
      open: () => isInterview ? openInterview(session.id) : openAgentSession(session),
    };
  });
  return [...chats, ...agents]
    .sort((left, right) => new Date(right.time || 0).getTime() - new Date(left.time || 0).getTime())
    .slice(0, 6);
});

const activityError = computed(() => [chatError.value, agentError.value].filter(Boolean).join('；'));

onMounted(() => {
  loadResumes();
  loadChats();
  loadAgentSessions();
});

async function loadResumes() {
  loadingResumes.value = true;
  resumeError.value = '';
  try {
    resumes.value = await getResumes();
  } catch (error) {
    resumeError.value = extractApiMessage(error, '简历读取失败，请检查后端服务后重试。');
  } finally {
    loadingResumes.value = false;
  }
}

async function loadChats() {
  loadingChats.value = true;
  chatError.value = '';
  try {
    const list = await listChatSessions();
    chatSessions.value = Array.isArray(list) ? list : [];
  } catch (error) {
    chatError.value = extractApiMessage(error, '对话记录读取失败。');
  } finally {
    loadingChats.value = false;
  }
}

async function loadAgentSessions() {
  loadingAgents.value = true;
  agentError.value = '';
  try {
    const list = await listAgentSessions();
    agentSessions.value = Array.isArray(list) ? list : [];
  } catch (error) {
    agentError.value = extractApiMessage(error, 'AI 任务和面试记录读取失败。');
  } finally {
    loadingAgents.value = false;
  }
}

async function createResume() {
  if (creatingResume.value) {
    return;
  }
  creatingResume.value = true;
  try {
    const created = await createResumeRequest();
    const id = created?.resumeId ?? created?.id ?? created?._id;
    if (id) {
      await router.push(`/editor/${id}`);
    } else {
      creationNotice.value = '简历已创建，但没有拿到简历编号。请到「我的简历」中重新打开。';
    }
  } catch (error) {
    resumeError.value = extractApiMessage(error, '简历创建失败，请重试。');
  } finally {
    creatingResume.value = false;
  }
}

async function createResumeRequest() {
  return createResumeApi({ title: '新建简历' });
}

function openResumeList() {
  document.querySelector('#resume-list')?.scrollIntoView({ behavior: 'smooth', block: 'start' });
}

function openChat(sessionId) {
  const query = sessionId ? { sessionId: String(sessionId) } : {};
  router.push({ name: 'Chat', query });
}

function openInterview(sessionId) {
  const query = sessionId ? { sessionId: String(sessionId) } : {};
  router.push({ name: 'Interview', query });
}

function openAgentSession(session) {
  if (session.resumeId && String(session.sceneCode || '').toUpperCase() === 'JD_CUSTOMIZE') {
    router.push({ name: 'ResumeEditor', params: { id: session.resumeId }, query: { sessionId: String(session.id) } });
    return;
  }
  router.push({ name: 'History', query: { sessionId: String(session.id), source: 'AGENT' } });
}

function runStatusLabel(status) {
  const labels = {
    QUEUED: '排队中',
    RUNNING: '进行中',
    WAITING_USER: '待补充',
    WAITING_CONFIRM: '待确认',
    FAILED: '执行失败',
    CANCELLED: '已取消',
    SUCCESS: '已完成',
  };
  return labels[status] || status;
}

function runStatusTone(status) {
  if (status === 'FAILED') {
    return 'ui-badge--danger';
  }
  if (status === 'WAITING_USER' || status === 'WAITING_CONFIRM') {
    return 'ui-badge--warning';
  }
  if (status === 'SUCCESS') {
    return 'ui-badge--success';
  }
  return 'ui-badge--accent';
}

function reloadActivity() {
  loadChats();
  loadAgentSessions();
}
</script>

<style scoped>
.home-page {
  display: flex;
  flex-direction: column;
  gap: var(--ui-space-6);
  max-width: var(--ui-content-max);
  margin: 0 auto;
}

.home-heading {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: var(--ui-space-5);
}

.home-eyebrow {
  margin: 0 0 var(--ui-space-2);
  color: var(--ui-fg-accent);
  font-size: var(--ui-text-xs);
  font-weight: 700;
  letter-spacing: 0.08em;
}

.home-heading .ui-page-subtitle {
  margin-top: var(--ui-space-2);
}

.entry-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 0;
  overflow: hidden;
  padding: var(--ui-space-2);
}

.entry-card {
  display: flex;
  min-width: 0;
  min-height: 176px;
  flex-direction: column;
  gap: var(--ui-space-3);
  padding: var(--ui-space-5);
}

.entry-card + .entry-card {
  border-left: 1px solid var(--ui-border-subtle);
}

.entry-card__top,
.entry-card__actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--ui-space-2);
}

.entry-card__copy h2 {
  margin: 0;
  font-size: var(--ui-text-lg);
  letter-spacing: var(--ui-tracking-snug);
}

.entry-card__copy p {
  margin: var(--ui-space-2) 0 0;
  color: var(--ui-fg-secondary);
  font-size: var(--ui-text-sm);
  line-height: var(--ui-line-normal);
}

.entry-card__actions button:focus-visible,
.activity-item:focus-visible {
  outline: none;
  box-shadow: var(--ui-ring);
}

.entry-card__state {
  display: flex;
  flex: 1;
  align-items: center;
  gap: var(--ui-space-3);
  color: var(--ui-fg-muted);
  font-size: var(--ui-text-sm);
  line-height: var(--ui-line-normal);
}

.entry-card__state--error {
  color: var(--ui-danger);
}

.entry-card__actions {
  min-height: 32px;
  justify-content: flex-start;
  margin-top: auto;
}

.overview-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  align-items: start;
}

.overview-panel {
  min-width: 0;
  padding: var(--ui-space-5);
}

.resume-library {
  display: flex;
  flex-direction: column;
  gap: var(--ui-space-4);
  padding: var(--ui-space-5);
  scroll-margin-top: var(--ui-space-5);
}

.resume-library__grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--ui-space-3);
}

.resume-library__empty {
  min-height: 190px;
}

.overview-panel {
  display: flex;
  flex-direction: column;
  gap: var(--ui-space-4);
}

.activity-list {
  display: flex;
  flex-direction: column;
  gap: var(--ui-space-1);
  margin: 0;
  padding: 0;
  list-style: none;
}

.activity-item {
  display: flex;
  width: 100%;
  align-items: center;
  gap: var(--ui-space-3);
  padding: var(--ui-space-3);
  border: 1px solid transparent;
  border-radius: var(--ui-radius-md);
  background: transparent;
  color: var(--ui-fg-primary);
  font: inherit;
  text-align: left;
  cursor: pointer;
}

.activity-item:hover {
  border-color: var(--ui-border-subtle);
  background: var(--ui-bg-hover);
}

.activity-item__icon {
  display: grid;
  width: 34px;
  height: 34px;
  flex: 0 0 auto;
  place-items: center;
  border: 1px solid var(--ui-border-subtle);
  border-radius: var(--ui-radius-md);
  background: var(--ui-bg-surface);
  color: var(--ui-fg-accent);
}

.activity-item__body {
  display: flex;
  min-width: 0;
  flex: 1;
  flex-direction: column;
  gap: 3px;
}

.activity-item__title {
  overflow: hidden;
  font-size: var(--ui-text-sm);
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.activity-item__meta {
  color: var(--ui-fg-muted);
  font-size: var(--ui-text-xs);
}

.activity-item__arrow {
  flex: 0 0 auto;
  color: var(--ui-fg-muted);
}

.activity-empty {
  min-height: 166px;
}

@media (max-width: 1200px) {
  .entry-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .entry-card--interview {
    grid-column: span 2;
    border-top: 1px solid var(--ui-border-subtle);
    border-left: 0;
  }
}

@media (max-width: 860px) {
  .resume-library__grid {
    grid-template-columns: minmax(0, 1fr);
  }
}

@media (max-width: 640px) {
  .home-heading {
    align-items: flex-start;
    flex-direction: column;
  }

  .entry-grid {
    grid-template-columns: minmax(0, 1fr);
  }

  .entry-card--interview {
    grid-column: auto;
    border-top: 1px solid var(--ui-border-subtle);
  }

  .entry-card + .entry-card {
    border-left: 0;
  }

  .entry-card--chat {
    border-top: 1px solid var(--ui-border-subtle);
  }
}
</style>
