<template>
  <div class="history-page">
    <header class="page-head">
      <div class="page-head__text">
        <h1 class="ui-page-title">历史记录</h1>
        <p class="ui-page-subtitle">查看与 AI 的全部会话记录，支持按类型筛选、关键词搜索与时间排序。</p>
      </div>
      <div class="page-head__actions">
        <button class="ui-btn ui-btn--outline" :disabled="loading" @click="loadSessions">
          <AppIcon name="arrows-clockwise" :size="16" />
          {{ loading ? '刷新中…' : '刷新' }}
        </button>
      </div>
    </header>

    <!-- 视图切换：历史总览 / 会话管理（保留原有的会话增删改与消息补录能力） -->
    <div class="ui-segmented view-switch" role="tablist" aria-label="历史记录视图">
      <button
        type="button"
        role="tab"
        class="ui-segmented__item"
        :class="{ 'is-active': view === 'overview' }"
        :aria-selected="view === 'overview' ? 'true' : 'false'"
        @click="view = 'overview'"
      >
        历史总览
      </button>
      <button
        type="button"
        role="tab"
        class="ui-segmented__item"
        :class="{ 'is-active': view === 'manage' }"
        :aria-selected="view === 'manage' ? 'true' : 'false'"
        @click="view = 'manage'"
      >
        会话管理
      </button>
    </div>

    <template v-if="view === 'overview'">
      <!-- 工具条：搜索 / 筛选 / 排序 / 密度 -->
      <section class="ui-panel toolbar">
        <div class="toolbar__row">
          <div class="field field--grow">
            <label class="ui-label" for="history-search">搜索</label>
            <input id="history-search" v-model="keyword" class="ui-input" type="search" placeholder="按会话标题或岗位 JD 搜索" />
          </div>
          <div class="field">
            <label class="ui-label" for="history-type">类型</label>
            <select id="history-type" v-model="typeFilter" class="ui-input ui-select">
              <option value="">全部类型</option>
              <option value="CHAT">AI 对话</option>
              <option value="INTERVIEW">模拟面试</option>
              <option value="RESUME">简历相关</option>
              <option value="OTHER">其他</option>
            </select>
          </div>
          <div class="field">
            <label class="ui-label" for="history-sort">排序</label>
            <select id="history-sort" v-model="sortOrder" class="ui-input ui-select">
              <option value="recent">最近活动</option>
              <option value="oldest">最早活动</option>
              <option value="created">最近创建</option>
              <option value="title">标题排序</option>
            </select>
          </div>
          <div class="field">
            <span class="ui-label">显示密度</span>
            <div class="ui-segmented" role="group" aria-label="显示密度">
              <button
                type="button"
                class="ui-segmented__item"
                :class="{ 'is-active': density === 'table' }"
                @click="density = 'table'"
              >
                表格
              </button>
              <button
                type="button"
                class="ui-segmented__item"
                :class="{ 'is-active': density === 'list' }"
                @click="density = 'list'"
              >
                列表
              </button>
            </div>
          </div>
        </div>
        <p class="toolbar__summary">
          共 {{ filteredSessions.length }} 条记录
          <span v-if="sessions.length !== filteredSessions.length">（已从 {{ sessions.length }} 条中筛选）</span>
        </p>
      </section>

      <div v-if="loadWarnings.length" class="ui-callout ui-callout--danger history-warning" role="alert">
        <AppIcon name="warning-circle" :size="16" />
        <span>{{ loadWarnings.join('；') }}</span>
        <button class="ui-btn ui-btn--sm ui-btn--outline" @click="loadSessions">重试</button>
      </div>

      <!-- 加载中 -->
      <div v-if="loading">
        <ul class="skeleton-list" aria-hidden="true">
          <li v-for="index in 5" :key="index" class="ui-skeleton skeleton-row"></li>
        </ul>
        <p class="state-text">正在加载历史记录…</p>
      </div>

      <!-- 加载失败 -->
      <div v-else-if="loadError" class="ui-callout ui-callout--danger">
        <AppIcon name="warning-circle" :size="16" />
        <div class="callout__body">
          <strong class="callout__title">历史记录加载失败</strong>
          <p class="callout__text">{{ loadError }}，请检查后端服务是否已启动后重试。</p>
        </div>
        <button class="ui-btn ui-btn--sm ui-btn--outline" @click="loadSessions">重试</button>
      </div>

      <!-- 没有任何记录 -->
      <div v-else-if="sessions.length === 0" class="ui-panel state-panel">
        <div class="ui-empty">
          <span class="ui-empty__icon" aria-hidden="true">
            <AppIcon name="clock-counter-clockwise" :size="24" />
          </span>
          <p class="ui-empty__title">还没有历史记录</p>
          <p class="ui-empty__hint">和 AI 对话一次，或者做一次模拟面试，记录就会出现在这里。</p>
          <div class="ui-empty__actions">
            <button class="ui-btn ui-btn--primary" @click="goChat">去和 AI 对话</button>
            <button class="ui-btn ui-btn--outline" @click="goInterview">开始模拟面试</button>
          </div>
        </div>
      </div>

      <!-- 有记录但筛选后为空 -->
      <div v-else-if="filteredSessions.length === 0" class="ui-panel state-panel">
        <div class="ui-empty">
          <span class="ui-empty__icon" aria-hidden="true">
            <AppIcon name="funnel" :size="24" />
          </span>
          <p class="ui-empty__title">没有匹配的记录</p>
          <p class="ui-empty__hint">换个关键词或筛选条件再试一次。</p>
          <button class="ui-btn ui-btn--outline" @click="resetFilters">清空筛选</button>
        </div>
      </div>

      <!-- 正常列表 -->
      <div v-else class="history-body">
        <section class="ui-panel list-panel">
          <div class="ui-scroll list-scroll">
            <table v-if="density === 'table'" class="history-table">
              <thead>
                <tr>
                  <th scope="col">会话标题</th>
                  <th scope="col">类型</th>
                  <th scope="col">状态</th>
                  <th scope="col">最近活动</th>
                </tr>
              </thead>
              <tbody>
                <tr
                  v-for="session in filteredSessions"
                  :key="session.historyKey"
                  class="history-row"
                  :class="{ 'is-active': selectedId === session.historyKey }"
                  tabindex="0"
                  @click="selectSession(session)"
                  @keydown.enter="selectSession(session)"
                  @keydown.space.prevent="selectSession(session)"
                >
                  <td class="cell-main">
                    <span class="cell-main__title">{{ sessionTitle(session) }}</span>
                    <span class="cell-main__sub">{{ sessionContext(session) }}</span>
                  </td>
                  <td><span class="ui-badge">{{ typeLabel(session.sceneCode) }}</span></td>
                  <td>
                    <span class="ui-badge" :class="statusBadge(session).tone">{{ statusBadge(session).label }}</span>
                  </td>
                  <td class="cell-time">{{ formatDateTime(sessionTime(session)) }}</td>
                </tr>
              </tbody>
            </table>

            <div v-else class="history-list">
              <button
                v-for="session in filteredSessions"
                :key="session.historyKey"
                type="button"
                class="history-item"
                :class="{ 'is-active': selectedId === session.historyKey }"
                @click="selectSession(session)"
              >
                <span class="history-item__head">
                  <span class="history-item__title">{{ sessionTitle(session) }}</span>
                  <span class="ui-badge">{{ typeLabel(session.sceneCode) }}</span>
                  <span class="ui-badge" :class="statusBadge(session).tone">{{ statusBadge(session).label }}</span>
                </span>
                <span class="history-item__body">
                  <span class="history-item__context">{{ sessionContext(session) }}</span>
                  <span class="history-item__time">{{ formatDateTime(sessionTime(session)) }}</span>
                </span>
              </button>
            </div>
          </div>
        </section>

        <!-- 选中会话的详情：摘要在这里展示（列表接口不返回摘要） -->
        <aside class="ui-panel detail-panel">
          <div class="ui-section-head">
            <h2 class="ui-section-head__title">会话详情</h2>
            <span v-if="sessionDetail" class="ui-badge">{{ typeLabel(sessionDetail.sceneCode) }}</span>
          </div>

          <p v-if="detailLoading" class="state-text">加载详情中…</p>

          <div v-else-if="detailError" class="ui-callout ui-callout--danger">
            <AppIcon name="warning-circle" :size="16" />
            <div class="callout__body">
              <strong class="callout__title">详情加载失败</strong>
              <p class="callout__text">{{ detailError }}，请点击重试。</p>
            </div>
            <button class="ui-btn ui-btn--sm ui-btn--outline" @click="reloadDetail">重试</button>
          </div>

          <div v-else-if="!sessionDetail" class="ui-empty">
            <span class="ui-empty__icon" aria-hidden="true">
              <AppIcon name="chat-circle-dots" :size="24" />
            </span>
            <p class="ui-empty__title">选择一条记录</p>
            <p class="ui-empty__hint">点击左侧任意会话，查看它的摘要、岗位 JD 与消息数量。</p>
          </div>

          <dl v-else class="detail-list">
            <div class="detail-item detail-item--wide">
              <dt class="detail-item__label">标题</dt>
              <dd class="detail-item__value">{{ sessionDetail.sessionTitle || '未命名会话' }}</dd>
            </div>
            <div class="detail-item">
              <dt class="detail-item__label">状态</dt>
              <dd class="detail-item__value">{{ statusBadge(sessionDetail).label }}</dd>
            </div>
            <div class="detail-item">
              <dt class="detail-item__label">消息数量</dt>
              <dd class="detail-item__value">{{ messageCount }} 条</dd>
            </div>
            <div class="detail-item">
              <dt class="detail-item__label">最近活动</dt>
              <dd class="detail-item__value">{{ formatDateTime(sessionTime(sessionDetail)) }}</dd>
            </div>
            <div class="detail-item">
              <dt class="detail-item__label">创建时间</dt>
              <dd class="detail-item__value">{{ formatDateTime(sessionDetail.createdAt) }}</dd>
            </div>
            <div class="detail-item detail-item--wide">
              <dt class="detail-item__label">摘要</dt>
              <dd class="detail-item__value detail-item__value--pre">{{ sessionDetail.summary || '这条会话还没有生成摘要。' }}</dd>
            </div>
            <div v-if="sessionDetail.jobDescription" class="detail-item detail-item--wide">
              <dt class="detail-item__label">岗位 JD</dt>
              <dd class="detail-item__value detail-item__value--pre">{{ sessionDetail.jobDescription }}</dd>
            </div>
          </dl>
          <div v-if="sessionDetail" class="detail-actions">
            <button v-if="sessionDetail.historySource === 'CHAT'" class="ui-btn ui-btn--primary" @click="openChatDetail">
              <AppIcon name="chat-circle" :size="15" />继续对话
            </button>
            <button v-else-if="sessionDetail.sceneCode === 'INTERVIEW'" class="ui-btn ui-btn--primary" @click="openInterviewDetail">
              <AppIcon name="microphone" :size="15" />打开面试记录
            </button>
            <button v-else-if="sessionDetail.resumeId" class="ui-btn ui-btn--primary" @click="openResumeTask">
              <AppIcon name="file-text" :size="15" />{{ sessionDetail.sceneCode === 'JD_CUSTOMIZE' ? '打开简历与 AI 任务' : '打开关联简历' }}
            </button>
          </div>
        </aside>
      </div>
    </template>

    <!-- 原有会话管理面板：新建 / 编辑 / 删除会话与补录消息 -->
    <section v-else class="legacy-panel">
      <AgentSessionManager />
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import AppIcon from '../components/common/AppIcon.vue';
import AgentSessionManager from '../components/AgentSessionManager.vue';
import { getAgentSessionDetail, listAgentSessions } from '../api/agentSessions.js';
import { listChatSessions } from '../api/chat.js';
import { getResumes } from '../api/resumes.js';
import { extractApiMessage } from '../api/request.js';
import { formatDateTime } from '../utils/resume.js';

// 简历类场景码统一归到「简历相关」
const RESUME_SCENE_CODES = new Set(['JD_CUSTOMIZE', 'RESUME_OPTIMIZE', 'RESUME_REWRITE', 'OPTIMIZE', 'MATCH']);

const TYPE_LABELS = {
  CHAT: 'AI 对话',
  INTERVIEW: '模拟面试',
  RESUME: '简历相关',
  OTHER: '其他',
};

const router = useRouter();
const route = useRoute();

const view = ref('overview');
const loading = ref(false);
const loadError = ref('');
const loadWarnings = ref([]);
const sessions = ref([]);
const resumeTitles = ref({});
const selectedId = ref(null);
const sessionDetail = ref(null);
const detailLoading = ref(false);
const detailError = ref('');

const keyword = ref('');
const typeFilter = ref('');
const sortOrder = ref('recent');
const density = ref('table');

onMounted(() => {
  loadSessions();
});

watch(() => [route.query.sessionId, route.query.source], async () => {
  if (!sessions.value.length) {
    await loadSessions();
    return;
  }
  const requestedSession = findRequestedSession();
  if (requestedSession) {
    await selectSession(requestedSession);
  }
});

async function loadSessions() {
  loading.value = true;
  loadError.value = '';
  loadWarnings.value = [];
  const [agentResult, chatResult, resumeResult] = await Promise.allSettled([
    listAgentSessions(),
    listChatSessions(),
    getResumes(),
  ]);
  const agentSessions = agentResult.status === 'fulfilled' && Array.isArray(agentResult.value)
    ? agentResult.value.map((session) => ({ ...session, historySource: 'AGENT', historyKey: `agent-${session.id}` }))
    : [];
  const chatSessions = chatResult.status === 'fulfilled' && Array.isArray(chatResult.value)
    ? chatResult.value.map((session) => ({
        ...session,
        sessionTitle: session.title || `对话 #${session.id}`,
        sceneCode: 'CHAT',
        status: 'ACTIVE',
        historySource: 'CHAT',
        historyKey: `chat-${session.id}`,
      }))
    : [];
  sessions.value = [...agentSessions, ...chatSessions];
  if (resumeResult.status === 'fulfilled' && Array.isArray(resumeResult.value)) {
    resumeTitles.value = resumeResult.value.reduce((acc, resume) => ({
      ...acc,
      [resume.id]: resume.title || '未命名简历',
    }), {});
  } else {
    resumeTitles.value = {};
    loadWarnings.value.push('简历名称读取失败，部分历史记录无法显示关联简历。');
  }
  if (agentResult.status === 'rejected') {
    loadWarnings.value.push(extractApiMessage(agentResult.reason, 'AI 任务记录读取失败。'));
  }
  if (chatResult.status === 'rejected') {
    loadWarnings.value.push(extractApiMessage(chatResult.reason, 'AI 对话记录读取失败。'));
  }
  if (agentResult.status === 'rejected' && chatResult.status === 'rejected') {
    loadError.value = '会话服务暂不可用。';
  }

  if (selectedId.value && !sessions.value.some((item) => item.historyKey === selectedId.value)) {
    selectedId.value = null;
    sessionDetail.value = null;
  }
  if (route.query.sessionId) {
    const requestedSession = findRequestedSession();
    if (requestedSession) {
      await selectSession(requestedSession);
    } else {
      selectedId.value = null;
      sessionDetail.value = null;
      detailError.value = '找不到这条会话记录。它可能已删除，请返回工作台重新选择。';
    }
  } else if (!selectedId.value && sessions.value.length) {
    const latestSession = [...sessions.value].sort((left, right) => sessionTime(right) - sessionTime(left))[0];
    await selectSession(latestSession);
  }
  loading.value = false;
}

async function selectSession(session) {
  selectedId.value = session.historyKey;
  detailLoading.value = true;
  detailError.value = '';
  try {
    if (session.historySource === 'CHAT') {
      sessionDetail.value = { ...session, summary: 'AI 对话内容可在对话工作区继续查看。' };
    } else {
      sessionDetail.value = {
        ...await getAgentSessionDetail(session.id),
        historySource: 'AGENT',
        historyKey: session.historyKey,
      };
    }
  } catch (err) {
    sessionDetail.value = null;
    detailError.value = extractApiMessage(err, '加载会话详情失败');
  } finally {
    detailLoading.value = false;
  }
}

async function reloadDetail() {
  const current = sessions.value.find((item) => item.historyKey === selectedId.value);
  if (current) {
    await selectSession(current);
  }
}

function resetFilters() {
  keyword.value = '';
  typeFilter.value = '';
  sortOrder.value = 'recent';
}

const filteredSessions = computed(() => {
  const word = keyword.value.trim().toLowerCase();
  const list = sessions.value.filter((session) => {
    if (typeFilter.value && sessionType(session.sceneCode) !== typeFilter.value) {
      return false;
    }
    if (!word) {
      return true;
    }
    return [session.sessionTitle, session.jobDescription]
      .some((value) => String(value || '').toLowerCase().includes(word));
  });

  return list.sort((a, b) => {
    if (sortOrder.value === 'oldest') {
      return sessionTime(a) - sessionTime(b);
    }
    if (sortOrder.value === 'created') {
      return toTime(b.createdAt) - toTime(a.createdAt);
    }
    if (sortOrder.value === 'title') {
      return sessionTitle(a).localeCompare(sessionTitle(b), 'zh-CN');
    }
    return sessionTime(b) - sessionTime(a);
  });
});

const messageCount = computed(() => {
  if (sessionDetail.value?.historySource === 'CHAT') {
    return Number(sessionDetail.value.messageCount) || 0;
  }
  const messages = sessionDetail.value?.messages;
  return Array.isArray(messages) ? messages.length : 0;
});

function sessionType(sceneCode) {
  const code = String(sceneCode || '').toUpperCase();
  if (code === 'CHAT') {
    return 'CHAT';
  }
  if (code === 'INTERVIEW') {
    return 'INTERVIEW';
  }
  if (RESUME_SCENE_CODES.has(code)) {
    return 'RESUME';
  }
  return 'OTHER';
}

function typeLabel(sceneCode) {
  return TYPE_LABELS[sessionType(sceneCode)];
}

function sessionTitle(session) {
  return session?.sessionTitle || session?.title || `会话 #${session?.id}`;
}

function sessionTime(session) {
  return toTime(session?.lastMessageAt || session?.updatedAt || session?.createdAt);
}

function toTime(value) {
  if (!value) {
    return 0;
  }
  const time = new Date(value).getTime();
  return Number.isNaN(time) ? 0 : time;
}

// 行内副标题：优先展示岗位 JD，其次展示关联简历
function sessionContext(session) {
  const jobDescription = String(session?.jobDescription || '').trim();
  if (jobDescription) {
    const firstLine = jobDescription.split('\n').find((line) => line.trim());
    return `岗位 JD：${firstLine ? firstLine.trim() : jobDescription}`;
  }
  const resumeTitle = resumeTitles.value[session?.resumeId];
  if (resumeTitle) {
    return `关联简历：${resumeTitle}`;
  }
  if (session?.historySource === 'CHAT') {
    return '独立 AI 对话';
  }
  return '未关联岗位 JD 与简历';
}

function statusBadge(session) {
  if (session?.historySource === 'CHAT') {
    return { label: '可继续', tone: 'ui-badge--accent' };
  }
  const runStatus = session?.activeRunStatus || session?.latestRunStatus;
  if (runStatus) {
    if (runStatus === 'QUEUED' || runStatus === 'RUNNING') {
      return { label: '进行中', tone: 'ui-badge--accent' };
    }
    if (runStatus === 'SUCCESS') {
      return { label: '已完成', tone: 'ui-badge--success' };
    }
    if (runStatus === 'FAILED') {
      return { label: '执行失败', tone: 'ui-badge--danger' };
    }
    if (runStatus === 'WAITING_USER') {
      return { label: '等待输入', tone: 'ui-badge--warning' };
    }
    if (runStatus === 'WAITING_CONFIRM') {
      return { label: '等待确认', tone: 'ui-badge--warning' };
    }
    if (runStatus === 'CANCELLED') {
      return { label: '已取消', tone: '' };
    }
    return { label: runStatus, tone: '' };
  }
  if (session?.status === 'ARCHIVED') {
    return { label: '已归档', tone: '' };
  }
  if (session?.status === 'DELETED') {
    return { label: '已删除', tone: 'ui-badge--danger' };
  }
  return { label: '可继续', tone: 'ui-badge--accent' };
}

function goChat() {
  router.push('/chat');
}

function goInterview() {
  router.push('/dashboard/interview');
}

function findRequestedSession() {
  const requestedId = String(route.query.sessionId || '');
  const requestedSource = String(route.query.source || 'AGENT').toUpperCase();
  return sessions.value.find((session) =>
    String(session.id) === requestedId && session.historySource === requestedSource
  );
}

function openChatDetail() {
  if (sessionDetail.value?.id) {
    router.push({ name: 'Chat', query: { sessionId: String(sessionDetail.value.id) } });
  }
}

function openInterviewDetail() {
  if (sessionDetail.value?.id) {
    router.push({ name: 'Interview', query: { sessionId: String(sessionDetail.value.id) } });
  }
}

function openResumeTask() {
  if (sessionDetail.value?.resumeId) {
    const query = sessionDetail.value.sceneCode === 'JD_CUSTOMIZE'
      ? { sessionId: String(sessionDetail.value.id) }
      : {};
    router.push({
      name: 'ResumeEditor',
      params: { id: sessionDetail.value.resumeId },
      query,
    });
  }
}
</script>

<style scoped>
.history-page {
  display: flex;
  flex-direction: column;
  gap: var(--ui-space-5);
  max-width: 1280px;
  margin: 0 auto;
  padding: 0;
}

.history-warning {
  align-items: flex-start;
}

/* ---------- 页头 ---------- */
.page-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--ui-space-4);
}

.page-head__actions {
  display: flex;
  flex-shrink: 0;
  gap: var(--ui-space-2);
}

/* ---------- 视图切换 ---------- */
.view-switch {
  align-self: flex-start;
}

/* ---------- 工具条 ---------- */
.toolbar {
  padding: var(--ui-space-4) var(--ui-space-5);
}

.toolbar__row {
  display: flex;
  flex-wrap: wrap;
  gap: var(--ui-space-4);
  align-items: flex-end;
}

.field {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.field--grow {
  flex: 1 1 260px;
}

.toolbar__summary {
  margin: var(--ui-space-3) 0 0;
  font-size: var(--ui-text-xs);
  color: var(--ui-fg-muted);
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

/* ---------- 状态块 ---------- */
.state-panel {
  padding: var(--ui-space-2);
}

.state-text {
  margin: var(--ui-space-4) 0 0;
  text-align: center;
  font-size: var(--ui-text-sm);
  color: var(--ui-fg-muted);
}

.ui-empty__actions {
  display: flex;
  flex-wrap: wrap;
  gap: var(--ui-space-2);
  justify-content: center;
}

/* 提示条里的文字块：标题 + 说明 */
.callout__body {
  display: flex;
  flex-direction: column;
  gap: 2px;
  flex: 1;
  min-width: 0;
}

.callout__title {
  font-weight: 600;
}

.callout__text {
  margin: 0;
  line-height: var(--ui-line-normal);
  word-break: break-word;
}

.skeleton-list {
  display: flex;
  flex-direction: column;
  gap: var(--ui-space-2);
  margin: 0;
  padding: 0;
  list-style: none;
}

.skeleton-row {
  height: 44px;
  border-radius: var(--ui-radius-md);
}

/* ---------- 列表 + 详情 ---------- */
.history-body {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 300px;
  gap: var(--ui-space-4);
  align-items: start;
}

.list-panel {
  padding: 0;
  overflow: hidden;
}

.list-scroll {
  max-height: calc(100vh - 300px);
  overflow-x: auto;
}

/* 表格密度 */
.history-table {
  width: 100%;
  border-collapse: collapse;
  font-size: var(--ui-text-sm);
}

.history-table th {
  position: sticky;
  top: 0;
  z-index: var(--ui-z-sticky);
  padding: var(--ui-space-3) var(--ui-space-4);
  border-bottom: 1px solid var(--ui-border);
  background: var(--ui-bg-surface);
  color: var(--ui-fg-muted);
  font-size: var(--ui-text-xs);
  font-weight: 600;
  text-align: left;
  white-space: nowrap;
}

.history-row {
  border-bottom: 1px solid var(--ui-border-subtle);
  cursor: pointer;
  transition: background var(--ui-duration-fast) var(--ui-ease-in-out);
}

.history-row:hover {
  background: var(--ui-bg-hover);
}

.history-row:focus-visible {
  outline: none;
  box-shadow: var(--ui-ring);
}

.history-row.is-active {
  background: var(--ui-bg-selected);
}

.history-row td {
  padding: var(--ui-space-3) var(--ui-space-4);
  vertical-align: top;
  color: var(--ui-fg-primary);
}

.cell-main {
  min-width: 200px;
}

.cell-main__title {
  display: block;
  font-weight: 600;
  word-break: break-word;
}

.cell-main__sub {
  display: block;
  margin-top: 2px;
  font-size: var(--ui-text-xs);
  color: var(--ui-fg-muted);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 420px;
}

.cell-time {
  white-space: nowrap;
  color: var(--ui-fg-secondary);
}

/* 列表密度 */
.history-list {
  display: flex;
  flex-direction: column;
}

.history-item {
  display: flex;
  flex-direction: column;
  gap: var(--ui-space-2);
  width: 100%;
  padding: var(--ui-space-3) var(--ui-space-4);
  border: none;
  border-bottom: 1px solid var(--ui-border-subtle);
  background: var(--ui-bg-surface);
  color: var(--ui-fg-primary);
  text-align: left;
  cursor: pointer;
  transition: background var(--ui-duration-fast) var(--ui-ease-in-out);
}

.history-item:hover {
  background: var(--ui-bg-hover);
}

.history-item:focus-visible {
  outline: none;
  box-shadow: var(--ui-ring);
}

.history-item.is-active {
  background: var(--ui-bg-selected);
}

.history-item__head {
  display: flex;
  align-items: center;
  gap: var(--ui-space-2);
}

.history-item__title {
  font-weight: 600;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.history-item__body {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--ui-space-3);
  font-size: var(--ui-text-xs);
  color: var(--ui-fg-muted);
}

.history-item__context {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.history-item__time {
  flex-shrink: 0;
}

/* ---------- 详情面板 ---------- */
.detail-panel {
  padding: var(--ui-space-5);
  position: sticky;
  top: 0;
}

.detail-actions {
  display: flex;
  flex-wrap: wrap;
  gap: var(--ui-space-2);
  margin-top: var(--ui-space-4);
}

.detail-list {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(130px, 1fr));
  gap: var(--ui-space-3);
  margin: 0;
}

.detail-item {
  display: flex;
  flex-direction: column;
  gap: var(--ui-space-1);
  min-width: 0;
  padding: var(--ui-space-3);
  border: 1px solid var(--ui-border);
  border-radius: var(--ui-radius-md);
  background: var(--ui-bg-sunken);
}

.detail-item--wide {
  grid-column: 1 / -1;
}

.detail-item__label {
  font-size: var(--ui-text-xs);
  color: var(--ui-fg-muted);
}

.detail-item__value {
  margin: 0;
  font-size: var(--ui-text-sm);
  font-weight: 600;
  color: var(--ui-fg-primary);
  word-break: break-word;
}

.detail-item__value--pre {
  font-weight: 400;
  line-height: var(--ui-line-relaxed);
  color: var(--ui-fg-secondary);
  white-space: pre-wrap;
}

/* ---------- 原有会话管理面板 ---------- */
.legacy-panel {
  padding: 0;
}

@media (max-width: 1100px) {
  .history-body {
    grid-template-columns: 1fr;
  }

  .list-scroll {
    max-height: 460px;
  }

  .detail-panel {
    position: static;
  }

  .page-head {
    flex-direction: column;
    align-items: stretch;
  }
}

@media (max-width: 720px) {
  .detail-list {
    grid-template-columns: 1fr;
  }

  .cell-main__sub {
    max-width: 220px;
  }
}
</style>
