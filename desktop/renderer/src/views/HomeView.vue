<template>
  <div class="home-page">
    <header class="home-heading">
      <div class="home-heading__copy">
        <p class="home-eyebrow">工作台</p>
        <h1 class="home-title">专注准备，走向更好的机会</h1>
        <p class="home-intro">简历、工作区对话和模拟面试都可单独开始；这里汇总真实工作状态和最近记录，方便继续。</p>
      </div>
    </header>

    <div v-if="creationNotice" class="home-notice" role="alert">
      <AppIcon name="warning-circle" :size="18" />
      <span>{{ creationNotice }}</span>
      <button type="button" class="home-notice__close" aria-label="关闭提示" @click="creationNotice = ''">
        <AppIcon name="x" :size="16" />
      </button>
    </div>
    <div v-if="resumeActionNotice" class="home-notice" role="alert">
      <AppIcon name="warning-circle" :size="18" />
      <span>{{ resumeActionNotice }}</span>
      <button type="button" class="home-notice__close" aria-label="关闭提示" @click="resumeActionNotice = ''">
        <AppIcon name="x" :size="16" />
      </button>
    </div>

    <section class="work-ledger" aria-label="独立工作入口">
      <article class="work-row">
        <div class="work-row__status" aria-live="polite">
          <template v-if="!authStore.isAuthenticated">
            <span class="entry-status__label">{{ authStore.guestSessionState === 'error' ? '未连接' : '连接中' }}</span>
            <button v-if="authStore.guestSessionState === 'error'" type="button" class="entry-status__retry" @click="retryGuestLogin">重试</button>
          </template>
          <template v-else-if="loadingResumes && !resumes.length">
            <span class="entry-status__label">读取中</span>
          </template>
          <template v-else-if="resumeError && !resumes.length">
            <span class="entry-status__label entry-status__label--error">读取失败</span>
            <button type="button" class="entry-status__retry" @click="loadResumes">重试</button>
          </template>
          <template v-else-if="resumes.length">
            <strong>{{ resumes.length }}</strong>
            <span class="entry-status__label">份简历</span>
          </template>
          <span v-else class="entry-status__label">暂无简历</span>
        </div>
        <div class="work-row__copy">
          <h2>简历编辑与优化</h2>
          <p>按目标岗位整理经历，逐模块优化内容，并可预览和导出简历。</p>
        </div>
        <div class="work-row__icon" aria-hidden="true">
          <span class="work-row__icon-tile"><AppIcon name="file-text" :size="50" /></span>
        </div>
        <div class="work-row__detail">
          <span>模块编辑与版本保存</span>
          <span>模板预览和文件导出</span>
          <span>AI 修改由你审阅确认</span>
        </div>
        <div class="work-row__action">
          <button
            type="button"
            class="ui-btn ui-btn--primary ledger-action"
            :disabled="creatingResume || (loadingResumes && !resumes.length)"
            @click="startResumeWorkflow"
          >
            <span>{{ creatingResume ? '创建中…' : '进入简历' }}</span>
            <AppIcon :name="creatingResume ? 'spinner-gap' : 'arrow-right'" :size="18" :spin="creatingResume" />
          </button>
        </div>
      </article>

      <article class="work-row">
        <div class="work-row__status" aria-live="polite">
          <template v-if="!authStore.isAuthenticated">
            <span class="entry-status__label">{{ authStore.guestSessionState === 'error' ? '未连接' : '连接中' }}</span>
            <button v-if="authStore.guestSessionState === 'error'" type="button" class="entry-status__retry" @click="retryGuestLogin">重试</button>
          </template>
          <template v-else-if="loadingChats && !chatSessions.length">
            <span class="entry-status__label">读取中</span>
          </template>
          <template v-else-if="chatError && !chatSessions.length">
            <span class="entry-status__label entry-status__label--error">读取失败</span>
            <button type="button" class="entry-status__retry" @click="loadChats">重试</button>
          </template>
          <template v-else-if="chatSessions.length">
            <strong>{{ chatSessions.length }}</strong>
            <span class="entry-status__label">条对话</span>
          </template>
          <span v-else class="entry-status__label">暂无对话</span>
        </div>
        <div class="work-row__copy">
          <h2>工作区对话</h2>
          <p>用于通用求职问题和本地文件交流。简历分区优化请在编辑器中打开模块 AI 助手。</p>
        </div>
        <div class="work-row__icon" aria-hidden="true">
          <span class="work-row__icon-tile"><AppIcon name="chat-centered-dots" :size="50" /></span>
        </div>
        <div class="work-row__detail">
          <span>岗位分析与差距分析</span>
          <span>围绕简历内容交流</span>
          <span>建议由你审阅后再使用</span>
        </div>
        <div class="work-row__action">
          <button type="button" class="ui-btn ui-btn--outline ledger-action" @click="startChat">
            <span>进入对话</span>
            <AppIcon name="arrow-right" :size="18" />
          </button>
        </div>
      </article>

      <article class="work-row">
        <div class="work-row__status" aria-live="polite">
          <template v-if="!authStore.isAuthenticated">
            <span class="entry-status__label">{{ authStore.guestSessionState === 'error' ? '未连接' : '连接中' }}</span>
            <button v-if="authStore.guestSessionState === 'error'" type="button" class="entry-status__retry" @click="retryGuestLogin">重试</button>
          </template>
          <template v-else-if="loadingAgents && !interviewSessions.length">
            <span class="entry-status__label">读取中</span>
          </template>
          <template v-else-if="agentError && !interviewSessions.length">
            <span class="entry-status__label entry-status__label--error">读取失败</span>
            <button type="button" class="entry-status__retry" @click="loadAgentSessions">重试</button>
          </template>
          <template v-else-if="interviewSessions.length">
            <strong>{{ interviewSessions.length }}</strong>
            <span class="entry-status__label">场面试</span>
          </template>
          <span v-else class="entry-status__label">暂无面试</span>
        </div>
        <div class="work-row__copy">
          <h2>模拟面试</h2>
          <p>选择简历与目标岗位，逐轮练习常见问题并查看面试反馈。</p>
        </div>
        <div class="work-row__icon" aria-hidden="true">
          <span class="work-row__icon-tile"><AppIcon name="user" :size="50" /></span>
        </div>
        <div class="work-row__detail">
          <span>按岗位和场景开始练习</span>
          <span>逐轮完成模拟问答</span>
          <span>查看并恢复历史记录</span>
        </div>
        <div class="work-row__action">
          <button type="button" class="ui-btn ui-btn--outline ledger-action" @click="startInterview">
            <span>进入面试</span>
            <AppIcon name="arrow-right" :size="18" />
          </button>
        </div>
      </article>
    </section>

    <section class="home-lower-grid" aria-label="近期内容">
      <article class="home-column home-column--activity">
        <header class="home-column__head">
          <h2>最近工作</h2>
          <router-link class="home-text-link" to="/dashboard/history">
            历史记录 <AppIcon name="caret-right" :size="14" />
          </router-link>
        </header>

        <p v-if="!authStore.isAuthenticated && authStore.guestSessionState === 'loading'" class="home-empty" aria-live="polite">正在进入游客模式…</p>
        <div v-else-if="!authStore.isAuthenticated" class="home-inline-error" role="alert">
          <span>游客模式暂时无法连接。</span>
          <button type="button" @click="retryGuestLogin">重试</button>
        </div>
        <div v-if="authStore.isAuthenticated && activityError" class="home-inline-error" role="alert">
          <span>{{ activityError }}</span>
          <button type="button" @click="reloadActivity">重试</button>
        </div>
        <ul v-if="authStore.isAuthenticated && recentActivity.length" class="activity-list">
          <li v-for="item in recentActivity.slice(0, 3)" :key="item.key">
            <button type="button" class="activity-item" @click="item.open()">
              <span class="activity-item__icon"><AppIcon :name="item.icon" :size="17" /></span>
              <span class="activity-item__body">
                <span class="activity-item__title">{{ item.title }}</span>
                <span class="activity-item__meta">{{ item.kind }} · {{ formatDateTime(item.time) }}</span>
              </span>
              <span v-if="item.status" class="ui-badge" :class="item.statusTone">{{ item.status }}</span>
              <AppIcon name="caret-right" :size="16" class="activity-item__arrow" />
            </button>
          </li>
        </ul>
        <p v-else-if="authStore.isAuthenticated && (loadingAgents || loadingChats)" class="home-empty" aria-live="polite">正在整理最近工作…</p>
        <p v-else-if="authStore.isAuthenticated && !activityError" class="home-empty">这里还没有近期记录。开始一次工作后，记录会显示在这里。</p>
      </article>

      <article id="resume-list" class="home-column home-column--resumes">
        <header class="home-column__head">
          <h2>我的简历</h2>
          <div class="home-column__actions">
            <router-link class="home-text-link" to="/dashboard/templates">
              <AppIcon name="layout" :size="15" />
              模板
            </router-link>
            <button type="button" class="home-text-link" :disabled="creatingResume" @click="createResume">
              <AppIcon :name="creatingResume ? 'spinner-gap' : 'file-plus'" :size="15" :spin="creatingResume" />
              新建
            </button>
            <button
              v-if="resumes.length > 3"
              type="button"
              class="home-text-link"
              :aria-expanded="showAllResumes"
              @click="showAllResumes = !showAllResumes"
            >
              {{ showAllResumes ? '收起' : '全部' }}
              <AppIcon name="caret-right" :size="14" />
            </button>
          </div>
        </header>

        <p v-if="!authStore.isAuthenticated && authStore.guestSessionState === 'loading'" class="home-empty" aria-live="polite">正在进入游客模式…</p>
        <div v-else-if="!authStore.isAuthenticated" class="home-inline-error" role="alert">
          <span>游客模式暂时无法连接。</span>
          <button type="button" @click="retryGuestLogin">重试</button>
        </div>
        <p v-else-if="loadingResumes && !resumes.length" class="home-empty" aria-live="polite">正在读取简历…</p>
        <div v-else-if="resumeError && !resumes.length" class="home-inline-error" role="alert">
          <span>{{ resumeError }}</span>
          <button type="button" @click="loadResumes">重试</button>
        </div>
        <div v-else-if="resumes.length" class="resume-list">
          <ResumeCard
            v-for="resume in visibleResumes"
            :key="resume.id"
            :resume="resume"
            @deleted="handleResumeDeleted"
            @delete-error="handleResumeDeleteError"
          />
        </div>
        <div v-else class="home-empty">
          <span>还没有简历</span>
          <button type="button" class="home-empty__action" :disabled="creatingResume" @click="createResume">创建一份简历</button>
        </div>
      </article>
    </section>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, ref, watch } from 'vue';
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
const resumeActionNotice = ref('');
const showAllResumes = ref(false);

watch(() => route.query.resumeCreateError, (code) => {
  if (!code) {
    return;
  }
  creationNotice.value = code === 'missing-id'
    ? '创建简历失败：后端没有返回简历编号。'
    : '创建简历失败，请检查后端服务后重试。';
  router.replace({ query: { ...route.query, resumeCreateError: undefined } });
}, { immediate: true });

const interviewSessions = computed(() =>
  agentSessions.value.filter((session) => String(session.sceneCode || '').toUpperCase() === 'INTERVIEW')
);

const recentActivity = computed(() => {
  const chats = chatSessions.value.slice(0, 4).map((session) => ({
    key: `chat-${session.id}`,
    title: session.title || `对话 #${session.id}`,
    kind: '工作区对话',
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
const visibleResumes = computed(() => showAllResumes.value ? resumes.value : resumes.value.slice(0, 3));

watch(() => route.query.section, (section) => {
  if (section === 'resumes') {
    showAllResumes.value = true;
    nextTick(openResumeList);
  }
});

onMounted(() => {
  if (authStore.isAuthenticated) {
    loadOverview();
  } else {
    authStore.loginAsGuest().catch(() => {});
  }
  if (route.query.section === 'resumes') {
    showAllResumes.value = true;
    nextTick(openResumeList);
  }
});

watch(() => authStore.token, (token) => {
  resumes.value = [];
  chatSessions.value = [];
  agentSessions.value = [];
  resumeError.value = '';
  chatError.value = '';
  agentError.value = '';
  if (token) {
    loadOverview();
  }
});

function loadOverview() {
  loadResumes();
  loadChats();
  loadAgentSessions();
}

async function retryGuestLogin() {
  try {
    await authStore.loginAsGuest();
  } catch {
    return;
  }
}

async function ensureGuestSession() {
  if (authStore.isAuthenticated) {
    return true;
  }
  try {
    await authStore.loginAsGuest();
    return authStore.isAuthenticated;
  } catch (error) {
    creationNotice.value = extractApiMessage(error, '游客模式暂时无法连接，请稍后重试。');
    return false;
  }
}

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

function handleResumeDeleted(id) {
  resumes.value = resumes.value.filter((resume) => String(resume.id) !== String(id));
  resumeActionNotice.value = '';
}

function handleResumeDeleteError(error) {
  resumeActionNotice.value = extractApiMessage(error, '删除简历失败，请重试。');
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
  if (!await ensureGuestSession()) {
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
    creationNotice.value = extractApiMessage(error, '简历创建失败，请重试。');
  } finally {
    creatingResume.value = false;
  }
}

async function createResumeRequest() {
  return createResumeApi({ title: '新建简历' });
}

function openResumeList() {
  showAllResumes.value = true;
  document.querySelector('#resume-list')?.scrollIntoView({ behavior: 'smooth', block: 'start' });
}

async function startResumeWorkflow() {
  if (!await ensureGuestSession()) {
    return;
  }
  if (!resumes.value.length && !resumeError.value) {
    await loadResumes();
  }
  if (resumeError.value) {
    creationNotice.value = resumeError.value;
    return;
  }
  if (resumes.value.length) {
    openResumeList();
    return;
  }
  createResume();
}

function openChat(sessionId) {
  const query = sessionId ? { sessionId: String(sessionId) } : {};
  router.push({ name: 'Chat', query });
}

async function startChat() {
  if (await ensureGuestSession()) {
    openChat();
  }
}

function openInterview(sessionId) {
  const query = sessionId ? { sessionId: String(sessionId) } : {};
  router.push({ name: 'Interview', query });
}

async function startInterview() {
  if (await ensureGuestSession()) {
    openInterview();
  }
}

function openAgentSession(session) {
  if (session.resumeId && String(session.sceneCode || '').toUpperCase() === 'JD_CUSTOMIZE') {
    const query = { sessionId: String(session.id) };
    if (session.targetSectionId) {
      query.sectionId = String(session.targetSectionId);
    }
    router.push({ name: 'ResumeEditor', params: { id: session.resumeId }, query });
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
  min-height: 100%;
  background: var(--ui-bg-app);
  color: var(--ui-fg-primary);
}

.home-heading {
  display: flex;
  min-height: 197px;
  align-items: center;
  padding: 24px 32px;
  border-bottom: 1px solid var(--ui-grid-line);
}

.home-heading__copy {
  min-width: 0;
}

.home-eyebrow {
  margin: 0 0 12px;
  color: var(--ui-fg-secondary);
  font-size: 14px;
  line-height: 1.4;
}

.home-title {
  margin: 0;
  color: var(--ui-fg-primary);
  font-family: var(--ui-font-display);
  font-size: 36px;
  font-weight: 600;
  letter-spacing: -0.025em;
  line-height: 1.08;
}

.home-intro {
  max-width: 75ch;
  margin: 10px 0 0;
  color: var(--ui-fg-secondary);
  font-size: 17px;
  line-height: 1.5;
}

.home-notice {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 32px;
  border-bottom: 1px solid var(--ui-danger-border);
  background: var(--ui-danger-soft);
  color: var(--ui-danger-fg);
  font-size: 14px;
}

.home-notice__close {
  display: grid;
  width: 30px;
  height: 30px;
  flex: 0 0 auto;
  place-items: center;
  margin-left: auto;
  border: 1px solid transparent;
  background: transparent;
  color: inherit;
  cursor: pointer;
}

.home-notice__close:hover {
  border-color: currentColor;
}

.work-ledger {
  border-bottom: 1px solid var(--ui-grid-line);
}

.work-row {
  display: grid;
  min-height: 166px;
  grid-template-columns: 100px minmax(0, 1.4fr) 142px minmax(190px, 1fr) 240px;
  border-bottom: 1px solid var(--ui-grid-line);
}

.work-row:last-child {
  border-bottom: 0;
}

.work-row__status,
.work-row__copy,
.work-row__icon,
.work-row__detail {
  border-right: 1px solid var(--ui-grid-line);
}

.work-row__status {
  display: flex;
  min-width: 0;
  flex-direction: column;
  justify-content: center;
  gap: 4px;
  padding: 0 10px 0 30px;
  color: var(--ui-fg-muted);
}

.work-row__status strong {
  color: var(--ui-fg-muted);
  font-size: 25px;
  font-weight: 500;
  line-height: 1.1;
  font-variant-numeric: tabular-nums;
}

.entry-status__label {
  color: var(--ui-fg-secondary);
  font-size: 14px;
  line-height: 1.4;
}

.entry-status__label--error {
  color: var(--ui-danger);
}

.entry-status__retry {
  width: fit-content;
  padding: 0;
  border: 0;
  background: transparent;
  color: var(--ui-accent);
  font-size: 13px;
  text-decoration: underline;
  text-underline-offset: 3px;
  cursor: pointer;
}

.work-row__copy {
  display: flex;
  min-width: 0;
  flex-direction: column;
  justify-content: center;
  padding: 20px 32px;
}

.work-row__copy h2 {
  margin: 0;
  color: var(--ui-fg-primary);
  font-size: 29px;
  font-weight: 650;
  letter-spacing: -0.02em;
  line-height: 1.2;
}

.work-row__copy p {
  max-width: 42ch;
  margin: 9px 0 0;
  color: var(--ui-fg-secondary);
  font-size: 16px;
  line-height: 1.55;
}

.work-row__icon {
  display: grid;
  place-items: center;
}

.work-row__icon-tile {
  display: grid;
  width: 84px;
  height: 84px;
  place-items: center;
  border: 1px solid var(--ui-grid-line);
  background: var(--ui-bg-canvas);
  color: var(--ui-fg-primary);
}

.work-row__detail {
  display: flex;
  min-width: 0;
  flex-direction: column;
  justify-content: center;
  gap: 8px;
  padding: 20px 32px;
  color: var(--ui-fg-secondary);
  font-size: 16px;
  line-height: 1.4;
}

.work-row__action {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px 18px;
}

.ledger-action {
  display: flex;
  width: 100%;
  height: 70px;
  justify-content: space-between;
  padding: 0 22px;
  font-size: 17px;
}

.home-lower-grid {
  display: grid;
  min-height: 214px;
  grid-template-columns: minmax(0, 0.75fr) minmax(0, 1fr);
  margin: 18px 36px 30px 17px;
  border: 1px solid var(--ui-grid-line);
  background: var(--ui-bg-canvas);
}

.home-column {
  display: flex;
  min-width: 0;
  flex-direction: column;
  padding: 14px 24px 12px;
}

.home-column--activity {
  border-right: 1px solid var(--ui-grid-line-strong);
}

.home-column__head {
  display: flex;
  min-height: 34px;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 8px;
}

.home-column__head h2 {
  margin: 0;
  color: var(--ui-fg-primary);
  font-size: 18px;
  font-weight: 650;
  line-height: 1.3;
}

.home-column__actions {
  display: flex;
  align-items: center;
  gap: 16px;
}

.home-text-link {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 0;
  border: 0;
  background: transparent;
  color: var(--ui-accent);
  font: inherit;
  font-size: 13px;
  white-space: nowrap;
  cursor: pointer;
}

.home-text-link:hover {
  color: var(--ui-accent-hover);
  text-decoration: underline;
  text-underline-offset: 3px;
}

.home-text-link:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.home-text-link:focus-visible,
.entry-status__retry:focus-visible,
.home-empty__action:focus-visible {
  outline: 2px solid var(--ui-accent);
  outline-offset: 3px;
}

.activity-list,
.resume-list {
  min-height: 0;
  margin: 0;
  padding: 0;
  overflow: auto;
  list-style: none;
}

.activity-list li + li {
  border-top: 1px solid var(--ui-grid-line);
}

.activity-item {
  display: flex;
  width: 100%;
  min-height: 43px;
  align-items: center;
  gap: 10px;
  padding: 5px 0;
  border: 0;
  background: transparent;
  color: var(--ui-fg-primary);
  font: inherit;
  text-align: left;
  cursor: pointer;
}

.activity-item:hover .activity-item__title {
  color: var(--ui-accent);
}

.activity-item:focus-visible {
  outline: 2px solid var(--ui-accent);
  outline-offset: -2px;
}

.activity-item__icon {
  display: grid;
  width: 28px;
  height: 28px;
  flex: 0 0 auto;
  place-items: center;
  border: 1px solid var(--ui-grid-line);
  color: var(--ui-fg-secondary);
}

.activity-item__body {
  display: flex;
  min-width: 0;
  flex: 1;
  flex-direction: column;
  gap: 1px;
}

.activity-item__title {
  overflow: hidden;
  font-size: 14px;
  font-weight: 550;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.activity-item__meta {
  color: var(--ui-fg-muted);
  font-size: 12px;
  line-height: 1.35;
}

.activity-item__arrow {
  flex: 0 0 auto;
  color: var(--ui-fg-muted);
}

.home-empty {
  display: flex;
  min-height: 105px;
  align-items: center;
  justify-content: center;
  gap: 10px;
  margin: 0;
  color: var(--ui-fg-muted);
  font-size: 14px;
  text-align: center;
}

.home-empty__action {
  padding: 5px 8px;
  border: 1px solid var(--ui-accent-border);
  background: transparent;
  color: var(--ui-accent);
  font-size: 13px;
  cursor: pointer;
}

.home-inline-error {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 6px 0;
  color: var(--ui-danger-fg);
  font-size: 13px;
}

.home-inline-error button {
  padding: 4px 0;
  border: 0;
  background: transparent;
  color: var(--ui-accent);
  text-decoration: underline;
  text-underline-offset: 3px;
  cursor: pointer;
}

@media (max-width: 1300px) {
  .work-row {
    grid-template-columns: 82px minmax(0, 1fr) 190px;
    grid-template-areas:
      "status copy action"
      "status detail action";
    min-height: 170px;
  }

  .work-row__status { grid-area: status; }
  .work-row__copy { grid-area: copy; min-height: 82px; padding: 18px 24px 2px; border-right: 0; }
  .work-row__icon { display: none; }
  .work-row__detail { grid-area: detail; padding: 2px 24px 16px; border-right: 1px solid var(--ui-grid-line); }
  .work-row__action { grid-area: action; }
  .work-row__copy h2 { font-size: 25px; }
  .work-row__detail { gap: 3px; font-size: 13px; }
  .home-heading { min-height: 164px; }
  .home-title { font-size: clamp(30px, 4vw, 42px); }
  .home-lower-grid { grid-template-columns: minmax(0, 1fr) minmax(0, 1fr); }
  .home-column { padding-right: 16px; padding-left: 16px; }
}

@media (max-width: 900px) {
  .home-heading { padding: 20px 24px; }
  .home-title { font-size: 32px; }
  .work-row {
    grid-template-columns: 82px minmax(0, 1fr);
    grid-template-areas:
      "status copy"
      "status detail"
      "action action";
    padding-bottom: 12px;
  }
  .work-row__status { padding-left: 18px; }
  .work-row__action { padding: 10px 18px 4px; }
  .ledger-action { height: 52px; }
  .home-lower-grid { grid-template-columns: minmax(0, 1fr); margin: 16px; }
  .home-column--activity { border-right: 0; border-bottom: 1px solid var(--ui-grid-line-strong); }
}
</style>
