<template>
  <section class="session-page">
    <header class="page-header">
      <div class="page-header__text">
        <h2 class="page-title">Agent 会话管理</h2>
        <p class="page-subtitle">新建、筛选、编辑和删除 Agent 会话，也可以补录历史消息。</p>
      </div>
      <button class="ui-btn ui-btn--outline" :disabled="loading" @click="loadSessions">
        {{ loading ? '刷新中…' : '刷新列表' }}
      </button>
    </header>

    <!-- 筛选 + 新建 -->
    <div class="ui-card toolbar">
      <div class="toolbar__row">
        <div class="field field--grow">
          <label class="ui-label" for="filter-resume">筛选简历</label>
          <select id="filter-resume" v-model="filters.resumeId" class="ui-input ui-select" @change="loadSessions">
            <option value="">全部简历</option>
            <option v-for="resume in resumes" :key="resume.id" :value="resume.id">{{ resume.title }}</option>
          </select>
        </div>
        <div class="field field--grow">
          <label class="ui-label" for="filter-scene">筛选场景</label>
          <select id="filter-scene" v-model="filters.sceneCode" class="ui-input ui-select" @change="loadSessions">
            <option value="">全部场景</option>
            <option v-for="scene in sceneOptions" :key="scene" :value="scene">{{ sceneLabel(scene) }}</option>
          </select>
        </div>
      </div>

      <div class="toolbar__row toolbar__row--create">
        <div class="field field--grow">
          <label class="ui-label" for="create-resume">关联简历</label>
          <select id="create-resume" v-model="createForm.resumeId" class="ui-input ui-select">
            <option value="">不关联简历</option>
            <option v-for="resume in resumes" :key="resume.id" :value="resume.id">{{ resume.title }}</option>
          </select>
        </div>
        <div class="field">
          <label class="ui-label" for="create-scene">场景</label>
          <select id="create-scene" v-model="createForm.sceneCode" class="ui-input ui-select">
            <option v-for="scene in sceneOptions" :key="scene" :value="scene">{{ sceneLabel(scene) }}</option>
          </select>
        </div>
        <div class="field field--grow">
          <label class="ui-label" for="create-title">会话标题</label>
          <input id="create-title" v-model="createForm.sessionTitle" class="ui-input" type="text" placeholder="可选" />
        </div>
        <button class="ui-btn ui-btn--primary create-btn" :disabled="creating" @click="handleCreateSession">
          {{ creating ? '创建中…' : '新建会话' }}
        </button>
      </div>
    </div>

    <p v-if="message" class="message success" role="status">{{ message }}</p>
    <p v-if="error" class="message error" role="alert">{{ error }}</p>

    <div class="session-layout">
      <!-- 会话列表 -->
      <aside class="ui-card session-list-card">
        <header class="card-head">
          <h3 class="card-head__title">会话列表</h3>
          <span class="ui-badge">{{ sessions.length }}</span>
        </header>

        <!-- 加载中 -->
        <div v-if="loading" class="state-block">
          <ul class="skeleton-list" aria-hidden="true">
            <li v-for="index in 4" :key="index" class="skeleton-row"></li>
          </ul>
          <p class="state-text">正在加载会话列表…</p>
        </div>

        <!-- 加载失败 -->
        <div v-else-if="listError" class="state-block">
          <div class="ui-empty">
            <p class="ui-empty__title">会话列表加载失败</p>
            <p class="ui-empty__hint">{{ listError }}</p>
            <button class="ui-btn ui-btn--primary" @click="loadSessions">重新加载</button>
          </div>
        </div>

        <!-- 空列表 -->
        <div v-else-if="sessions.length === 0" class="state-block">
          <div class="ui-empty">
            <p class="ui-empty__title">还没有会话记录</p>
            <p class="ui-empty__hint">用上面的「新建会话」创建一个，或先去 AI 对话一次。</p>
          </div>
        </div>

        <!-- 列表 -->
        <div v-else class="session-items">
          <button
            v-for="session in sessions"
            :key="session.id"
            type="button"
            class="session-item"
            :class="{ 'is-active': selectedSessionId === session.id }"
            @click="loadSessionDetail(session.id)"
          >
            <span class="session-item__head">
              <span class="session-item__title">{{ session.sessionTitle || '未命名会话' }}</span>
              <span class="ui-badge">{{ sceneLabel(session.sceneCode) }}</span>
            </span>
            <span class="session-item__foot">
              <span class="ui-badge" :class="statusBadge(session).tone">{{ statusBadge(session).label }}</span>
              <span class="session-item__time">{{ formatDateTime(session.updatedAt || session.lastMessageAt) }}</span>
            </span>
          </button>
        </div>
      </aside>

      <!-- 会话详情 -->
      <section class="ui-card session-detail-card">
        <div v-if="detailLoading" class="state-block">
          <p class="state-text">正在加载会话详情…</p>
        </div>

        <div v-else-if="!sessionDetail" class="state-block">
          <div class="ui-empty">
            <p class="ui-empty__title">选择一条会话</p>
            <p class="ui-empty__hint">点击左侧任意会话，查看它的消息并编辑标题与状态。</p>
          </div>
        </div>

        <template v-else>
          <div class="detail-top">
            <div class="detail-meta">
              <h3 class="detail-meta__title">{{ sessionDetail.sessionTitle || '未命名会话' }}</h3>
              <p class="detail-meta__badges">
                <span class="ui-badge">{{ sceneLabel(sessionDetail.sceneCode) }}</span>
                <span class="ui-badge" :class="statusBadge(sessionDetail).tone">{{ statusBadge(sessionDetail).label }}</span>
                <span class="detail-meta__time">{{ formatDateTime(sessionDetail.updatedAt) }}</span>
              </p>
            </div>
            <button class="ui-btn ui-btn--danger ui-btn--sm" @click="handleDeleteSession">删除会话</button>
          </div>

          <!-- 编辑会话 -->
          <div class="update-grid">
            <div class="field field--grow">
              <label class="ui-label" for="update-title">会话标题</label>
              <input id="update-title" v-model="updateForm.sessionTitle" class="ui-input" type="text" placeholder="修改会话标题" />
            </div>
            <div class="field">
              <label class="ui-label" for="update-status">状态</label>
              <select id="update-status" v-model="updateForm.status" class="ui-input ui-select">
                <option value="ACTIVE">活跃</option>
                <option value="ARCHIVED">已归档</option>
                <option value="DELETED">已删除</option>
              </select>
            </div>
            <button class="ui-btn ui-btn--outline action-btn" @click="handleUpdateSession">更新会话</button>
          </div>

          <!-- 消息列表 -->
          <div class="message-header">
            <strong class="message-header__title">消息列表</strong>
            <button class="ui-btn ui-btn--ghost ui-btn--sm" @click="refreshMessages">刷新消息</button>
          </div>

          <div class="message-list ui-scroll">
            <p v-if="messages.length === 0" class="state-text state-text--left">这条会话还没有消息。</p>
            <div
              v-for="messageItem in messages"
              :key="messageItem.id"
              class="message-item"
              :class="messageItem.role.toLowerCase()"
            >
              <div class="message-item-top">
                <span>{{ messageItem.role }}</span>
                <span>{{ formatDateTime(messageItem.createdAt) }}</span>
              </div>
              <pre class="message-content">{{ messageItem.content }}</pre>
            </div>
          </div>

          <!-- 补录消息 -->
          <div class="composer-card">
            <p class="composer-card__title">补录消息</p>
            <div class="update-grid">
              <div class="field">
                <label class="ui-label" for="message-role">消息角色</label>
                <select id="message-role" v-model="messageForm.role" class="ui-input ui-select">
                  <option value="USER">USER</option>
                  <option value="ASSISTANT">ASSISTANT</option>
                  <option value="SYSTEM">SYSTEM</option>
                  <option value="TOOL">TOOL</option>
                </select>
              </div>
              <div class="field">
                <label class="ui-label" for="message-type">内容类型</label>
                <select id="message-type" v-model="messageForm.contentType" class="ui-input ui-select">
                  <option value="TEXT">TEXT</option>
                  <option value="JSON">JSON</option>
                </select>
              </div>
            </div>
            <div class="field">
              <label class="ui-label" for="message-content">消息内容</label>
              <textarea id="message-content" v-model="messageForm.content" class="ui-input field-textarea" rows="5" placeholder="补录一条消息"></textarea>
            </div>
            <button class="ui-btn ui-btn--primary" :disabled="sendingMessage" @click="handleCreateMessage">
              {{ sendingMessage ? '发送中…' : '新增消息' }}
            </button>
          </div>
        </template>
      </section>
    </div>
  </section>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue';
import {
  createAgentMessage,
  createAgentSession,
  deleteAgentSession,
  extractApiMessage,
  getAgentSessionDetail,
  getResumes,
  listAgentMessages,
  listAgentSessions,
  updateAgentSession,
} from '../api/index.js';
import { formatDateTime } from '../utils/resume.js';

const sceneOptions = ['CHAT', 'OPTIMIZE', 'MATCH', 'SUMMARY'];

// 场景码 → 中文名（与「历史记录」页保持同一套口径）
const SCENE_LABELS = {
  CHAT: 'AI 对话',
  OPTIMIZE: '简历优化',
  MATCH: '岗位匹配',
  SUMMARY: '简历摘要',
};

const loading = ref(false);
const detailLoading = ref(false);
const creating = ref(false);
const sendingMessage = ref(false);
const message = ref('');
const error = ref('');
const listError = ref('');
const resumes = ref([]);
const sessions = ref([]);
const sessionDetail = ref(null);
const messages = ref([]);
const selectedSessionId = ref(null);

const filters = reactive({
  resumeId: '',
  sceneCode: '',
});

const createForm = reactive({
  resumeId: '',
  sceneCode: 'CHAT',
  sessionTitle: '',
});

const updateForm = reactive({
  sessionTitle: '',
  status: 'ACTIVE',
});

const messageForm = reactive({
  role: 'USER',
  contentType: 'TEXT',
  content: '',
});

onMounted(async () => {
  await Promise.all([loadResumes(), loadSessions()]);
});

function sceneLabel(sceneCode) {
  return SCENE_LABELS[sceneCode] || sceneCode || '未知场景';
}

// 状态展示：优先看单次运行状态，其次看会话自身的归档 / 删除状态
function statusBadge(session) {
  const runStatus = session?.activeRunStatus;
  if (runStatus) {
    if (runStatus === 'SUCCESS') {
      return { label: '已完成', tone: 'ui-badge--success' };
    }
    if (runStatus === 'FAILED') {
      return { label: '执行失败', tone: 'ui-badge--danger' };
    }
    if (runStatus === 'WAITING_USER') {
      return { label: '等待输入', tone: 'ui-badge--warning' };
    }
    return { label: runStatus, tone: 'ui-badge--accent' };
  }
  if (session?.status === 'ARCHIVED') {
    return { label: '已归档', tone: '' };
  }
  if (session?.status === 'DELETED') {
    return { label: '已删除', tone: 'ui-badge--danger' };
  }
  return { label: '活跃', tone: 'ui-badge--accent' };
}

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

async function loadSessions() {
  loading.value = true;
  listError.value = '';
  setStatus();
  try {
    sessions.value = await listAgentSessions({
      resumeId: filters.resumeId || undefined,
      sceneCode: filters.sceneCode || undefined,
    });
  } catch (err) {
    sessions.value = [];
    listError.value = extractApiMessage(err, '加载会话列表失败');
  } finally {
    loading.value = false;
  }
}

async function loadSessionDetail(sessionId) {
  detailLoading.value = true;
  selectedSessionId.value = sessionId;
  setStatus();
  try {
    sessionDetail.value = await getAgentSessionDetail(sessionId);
    messages.value = await listAgentMessages(sessionId);
    updateForm.sessionTitle = sessionDetail.value.sessionTitle || '';
    updateForm.status = sessionDetail.value.status || 'ACTIVE';
  } catch (err) {
    setStatus('', extractApiMessage(err, '加载会话详情失败'));
  } finally {
    detailLoading.value = false;
  }
}

async function refreshMessages() {
  if (!selectedSessionId.value) {
    return;
  }
  try {
    messages.value = await listAgentMessages(selectedSessionId.value);
  } catch (err) {
    setStatus('', extractApiMessage(err, '刷新消息失败'));
  }
}

async function handleCreateSession() {
  creating.value = true;
  setStatus();
  try {
    const session = await createAgentSession({
      resumeId: createForm.resumeId || null,
      sceneCode: createForm.sceneCode,
      sessionTitle: createForm.sessionTitle || null,
    });
    createForm.resumeId = '';
    createForm.sceneCode = 'CHAT';
    createForm.sessionTitle = '';
    await loadSessions();
    await loadSessionDetail(session.id);
    setStatus('会话创建成功');
  } catch (err) {
    setStatus('', extractApiMessage(err, '创建会话失败'));
  } finally {
    creating.value = false;
  }
}

async function handleUpdateSession() {
  if (!selectedSessionId.value) {
    return;
  }
  try {
    await updateAgentSession(selectedSessionId.value, {
      sessionTitle: updateForm.sessionTitle || null,
      status: updateForm.status || null,
    });
    await loadSessions();
    await loadSessionDetail(selectedSessionId.value);
    setStatus('会话更新成功');
  } catch (err) {
    setStatus('', extractApiMessage(err, '更新会话失败'));
  }
}

async function handleDeleteSession() {
  if (!selectedSessionId.value) {
    return;
  }
  const title = sessionDetail.value?.sessionTitle || '未命名会话';
  if (!window.confirm(`确定删除会话「${title}」吗？该操作不可恢复。`)) {
    return;
  }
  try {
    await deleteAgentSession(selectedSessionId.value);
    selectedSessionId.value = null;
    sessionDetail.value = null;
    messages.value = [];
    await loadSessions();
    setStatus('会话已删除');
  } catch (err) {
    setStatus('', extractApiMessage(err, '删除会话失败'));
  }
}

async function handleCreateMessage() {
  if (!selectedSessionId.value || !messageForm.content.trim()) {
    setStatus('', '请先选择会话并填写消息内容');
    return;
  }
  sendingMessage.value = true;
  setStatus();
  try {
    await createAgentMessage(selectedSessionId.value, {
      role: messageForm.role,
      contentType: messageForm.contentType,
      content: messageForm.content,
    });
    messageForm.content = '';
    await loadSessionDetail(selectedSessionId.value);
    setStatus('消息新增成功');
  } catch (err) {
    setStatus('', extractApiMessage(err, '新增消息失败'));
  } finally {
    sendingMessage.value = false;
  }
}
</script>

<style scoped>
/* ========================================
   Agent 会话管理台
   ======================================== */
.session-page {
  display: flex;
  flex-direction: column;
  gap: var(--ui-space-5);
}

/* ---------- 页头 ---------- */
.page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--ui-space-4);
}

.page-title {
  margin: 0;
  font-size: var(--ui-text-xl);
  font-weight: 700;
  line-height: var(--ui-line-tight);
  color: var(--ui-fg-primary);
}

.page-subtitle {
  margin: var(--ui-space-2) 0 0;
  font-size: var(--ui-text-base);
  color: var(--ui-fg-secondary);
}

/* ---------- 工具条 ---------- */
.toolbar {
  padding: var(--ui-space-5);
  display: flex;
  flex-direction: column;
  gap: var(--ui-space-4);
}

.toolbar__row {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-end;
  gap: var(--ui-space-4);
}

.toolbar__row--create {
  padding-top: var(--ui-space-4);
  border-top: 1px solid var(--ui-border-subtle);
}

.field {
  display: flex;
  flex-direction: column;
  gap: var(--ui-space-2);
  min-width: 0;
}

.field--grow {
  flex: 1 1 200px;
}

.field-textarea {
  min-height: 120px;
  resize: vertical;
  font-family: inherit;
}

.create-btn,
.action-btn {
  flex-shrink: 0;
}

/* ---------- 提示条 ---------- */
.message {
  margin: 0;
  padding: var(--ui-space-3) var(--ui-space-4);
  border-radius: var(--ui-radius-md);
  border: 1px solid transparent;
  font-size: var(--ui-text-sm);
  color: var(--ui-fg-primary);
}

.message.success {
  background: var(--ui-success-soft);
  border-color: var(--ui-success-border);
}

.message.error {
  background: var(--ui-danger-soft);
  border-color: var(--ui-danger-border);
}

/* ---------- 列表 + 详情 ---------- */
.session-layout {
  display: grid;
  grid-template-columns: 320px minmax(0, 1fr);
  gap: var(--ui-space-4);
  align-items: start;
}

.session-list-card,
.session-detail-card {
  padding: var(--ui-space-5);
  display: flex;
  flex-direction: column;
  gap: var(--ui-space-4);
  min-width: 0;
}

.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--ui-space-3);
  padding-bottom: var(--ui-space-3);
  border-bottom: 1px solid var(--ui-border);
}

.card-head__title {
  margin: 0;
  font-size: var(--ui-text-base);
  font-weight: 600;
  color: var(--ui-fg-primary);
}

/* ---------- 会话列表项 ---------- */
.session-items {
  display: flex;
  flex-direction: column;
  gap: var(--ui-space-2);
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
  transition: background var(--ui-duration-fast) var(--ui-ease-in-out),
    border-color var(--ui-duration-fast) var(--ui-ease-in-out);
}

.session-item:hover {
  background: var(--ui-bg-hover);
}

.session-item:focus-visible {
  outline: none;
  box-shadow: var(--ui-ring);
}

.session-item.is-active {
  border-color: var(--ui-border-focus);
  background: var(--ui-bg-selected);
}

.session-item__head,
.session-item__foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--ui-space-2);
  min-width: 0;
}

.session-item__title {
  font-size: var(--ui-text-sm);
  font-weight: 600;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.session-item__time {
  font-size: var(--ui-text-xs);
  color: var(--ui-fg-muted);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* ---------- 详情 ---------- */
.detail-top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--ui-space-4);
}

.detail-meta {
  min-width: 0;
}

.detail-meta__title {
  margin: 0;
  font-size: var(--ui-text-lg);
  font-weight: 600;
  color: var(--ui-fg-primary);
  word-break: break-word;
}

.detail-meta__badges {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--ui-space-2);
  margin: var(--ui-space-2) 0 0;
}

.detail-meta__time {
  font-size: var(--ui-text-xs);
  color: var(--ui-fg-muted);
}

.update-grid {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-end;
  gap: var(--ui-space-4);
}

/* ---------- 消息 ---------- */
.message-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--ui-space-3);
  padding-top: var(--ui-space-3);
  border-top: 1px solid var(--ui-border);
}

.message-header__title {
  font-size: var(--ui-text-base);
  color: var(--ui-fg-primary);
}

.message-list {
  display: flex;
  flex-direction: column;
  gap: var(--ui-space-3);
  max-height: 360px;
  overflow-y: auto;
}

.message-item {
  border: 1px solid var(--ui-border-subtle);
  border-radius: var(--ui-radius-md);
  padding: var(--ui-space-3);
}

.message-item.user {
  background: var(--ui-info-soft);
  border-color: var(--ui-info-border);
}

.message-item.assistant {
  background: var(--ui-accent-soft);
  border-color: var(--ui-accent-border);
}

.message-item.system,
.message-item.tool {
  background: var(--ui-bg-sunken);
}

.message-item-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--ui-space-3);
  font-size: var(--ui-text-xs);
  color: var(--ui-fg-secondary);
}

.message-content {
  margin: var(--ui-space-2) 0 0;
  white-space: pre-wrap;
  word-break: break-word;
  font-family: inherit;
  font-size: var(--ui-text-sm);
  line-height: var(--ui-line-relaxed);
  color: var(--ui-fg-primary);
}

/* ---------- 补录消息 ---------- */
.composer-card {
  display: flex;
  flex-direction: column;
  gap: var(--ui-space-3);
  padding: var(--ui-space-4);
  border: 1px solid var(--ui-border);
  border-radius: var(--ui-radius-md);
  background: var(--ui-bg-sunken);
}

.composer-card__title {
  margin: 0;
  font-size: var(--ui-text-base);
  font-weight: 600;
  color: var(--ui-fg-primary);
}

/* ---------- 状态块 ---------- */
.state-block {
  padding: var(--ui-space-4) 0;
}

.state-text {
  margin: var(--ui-space-3) 0 0;
  text-align: center;
  font-size: var(--ui-text-sm);
  color: var(--ui-fg-muted);
}

.state-text--left {
  margin: 0;
  text-align: left;
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
  height: 56px;
  border-radius: var(--ui-radius-md);
  background: var(--ui-bg-sunken);
  animation: skeleton-pulse 1.4s var(--ui-ease-in-out) infinite;
}

@keyframes skeleton-pulse {
  0%,
  100% {
    opacity: 1;
  }

  50% {
    opacity: 0.55;
  }
}

/* ---------- 窄窗口 ---------- */
@media (max-width: 1100px) {
  .session-layout {
    grid-template-columns: 1fr;
  }

  .page-header {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
