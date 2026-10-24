<template>
  <section class="session-page">
    <header class="page-header">
      <div>
        <h1 class="page-title">Agent 会话管理</h1>
        <p class="page-subtitle">查看、筛选、编辑和补录会话消息</p>
      </div>
      <button class="refresh-btn" :disabled="loading" @click="loadSessions">刷新列表</button>
    </header>

    <div class="toolbar-card">
      <div class="toolbar-grid">
        <div class="field">
          <label>筛选简历</label>
          <select v-model="filters.resumeId" class="field-input" @change="loadSessions">
            <option value="">全部</option>
            <option v-for="resume in resumes" :key="resume.id" :value="resume.id">{{ resume.title }}</option>
          </select>
        </div>
        <div class="field">
          <label>筛选场景</label>
          <select v-model="filters.sceneCode" class="field-input" @change="loadSessions">
            <option value="">全部</option>
            <option v-for="scene in sceneOptions" :key="scene" :value="scene">{{ scene }}</option>
          </select>
        </div>
      </div>

      <div class="create-grid">
        <div class="field">
          <label>新建会话简历</label>
          <select v-model="createForm.resumeId" class="field-input">
            <option value="">不关联简历</option>
            <option v-for="resume in resumes" :key="resume.id" :value="resume.id">{{ resume.title }}</option>
          </select>
        </div>
        <div class="field">
          <label>场景</label>
          <select v-model="createForm.sceneCode" class="field-input">
            <option v-for="scene in sceneOptions" :key="scene" :value="scene">{{ scene }}</option>
          </select>
        </div>
        <div class="field">
          <label>会话标题</label>
          <input v-model="createForm.sessionTitle" class="field-input" type="text" placeholder="可选" />
        </div>
        <button class="primary-btn create-btn" :disabled="creating" @click="handleCreateSession">
          {{ creating ? '创建中...' : '新建会话' }}
        </button>
      </div>
    </div>

    <p v-if="message" class="message success">{{ message }}</p>
    <p v-if="error" class="message error">{{ error }}</p>

    <div class="session-layout">
      <aside class="session-list-card">
        <div v-if="loading" class="empty-block">加载中...</div>
        <div v-else-if="sessions.length === 0" class="empty-block">暂无会话记录</div>
        <button
          v-for="session in sessions"
          :key="session.id"
          class="session-item"
          :class="{ active: selectedSessionId === session.id }"
          @click="loadSessionDetail(session.id)"
        >
          <div class="session-item-top">
            <span class="session-title">{{ session.sessionTitle }}</span>
            <span class="session-scene">{{ session.sceneCode }}</span>
          </div>
          <div class="session-item-bottom">
            <span>{{ session.status }}</span>
            <span>{{ formatDateTime(session.updatedAt || session.lastMessageAt) }}</span>
          </div>
        </button>
      </aside>

      <section class="session-detail-card">
        <div v-if="detailLoading" class="empty-block">加载详情中...</div>
        <div v-else-if="!sessionDetail" class="empty-block">选择一个会话查看详情</div>
        <template v-else>
          <div class="detail-top">
            <div class="detail-meta">
              <h2>{{ sessionDetail.sessionTitle }}</h2>
              <p>
                <span>{{ sessionDetail.sceneCode }}</span>
                <span>{{ sessionDetail.status }}</span>
                <span>{{ formatDateTime(sessionDetail.updatedAt) }}</span>
              </p>
            </div>
            <button class="danger-btn" @click="handleDeleteSession">删除会话</button>
          </div>

          <div class="update-grid">
            <div class="field">
              <label>会话标题</label>
              <input v-model="updateForm.sessionTitle" class="field-input" type="text" placeholder="修改会话标题" />
            </div>
            <div class="field">
              <label>状态</label>
              <select v-model="updateForm.status" class="field-input">
                <option value="ACTIVE">ACTIVE</option>
                <option value="ARCHIVED">ARCHIVED</option>
                <option value="DELETED">DELETED</option>
              </select>
            </div>
            <button class="ghost-btn action-btn" @click="handleUpdateSession">更新会话</button>
          </div>

          <div class="message-header">
            <strong>消息列表</strong>
            <button class="ghost-btn small" @click="refreshMessages">刷新消息</button>
          </div>

          <div class="message-list">
            <div v-if="messages.length === 0" class="empty-block small">暂无消息</div>
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

          <div class="composer-card">
            <div class="update-grid">
              <div class="field">
                <label>消息角色</label>
                <select v-model="messageForm.role" class="field-input">
                  <option value="USER">USER</option>
                  <option value="ASSISTANT">ASSISTANT</option>
                  <option value="SYSTEM">SYSTEM</option>
                  <option value="TOOL">TOOL</option>
                </select>
              </div>
              <div class="field">
                <label>内容类型</label>
                <select v-model="messageForm.contentType" class="field-input">
                  <option value="TEXT">TEXT</option>
                  <option value="JSON">JSON</option>
                </select>
              </div>
            </div>
            <div class="field">
              <label>消息内容</label>
              <textarea v-model="messageForm.content" class="field-textarea" rows="5" placeholder="补录一条消息"></textarea>
            </div>
            <button class="primary-btn" :disabled="sendingMessage" @click="handleCreateMessage">
              {{ sendingMessage ? '发送中...' : '新增消息' }}
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

const loading = ref(false);
const detailLoading = ref(false);
const creating = ref(false);
const sendingMessage = ref(false);
const message = ref('');
const error = ref('');
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
  setStatus();
  try {
    sessions.value = await listAgentSessions({
      resumeId: filters.resumeId || undefined,
      sceneCode: filters.sceneCode || undefined,
    });
  } catch (err) {
    setStatus('', extractApiMessage(err, '加载会话列表失败'));
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
  if (!window.confirm('确定删除这个会话吗？')) {
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
.session-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.page-header,
.toolbar-grid,
.create-grid,
.detail-top,
.update-grid,
.message-header,
.session-item-top,
.session-item-bottom,
.message-item-top {
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

.toolbar-card,
.session-list-card,
.session-detail-card {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 14px;
  padding: 16px;
}

.toolbar-card,
.session-detail-card {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.toolbar-grid,
.create-grid,
.update-grid {
  align-items: end;
}

.field {
  display: flex;
  flex-direction: column;
  gap: 6px;
  flex: 1;
}

.field label {
  font-size: 13px;
  font-weight: 600;
  color: #374151;
}

.field-input,
.field-textarea {
  width: 100%;
  border: 1px solid #d1d5db;
  border-radius: 8px;
  padding: 10px 12px;
  font-size: 14px;
  box-sizing: border-box;
}

.field-textarea {
  resize: vertical;
  min-height: 120px;
  font-family: inherit;
}

.refresh-btn,
.ghost-btn,
.primary-btn,
.danger-btn {
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
  border: none;
  background: #22c55e;
  color: #fff;
}

.danger-btn {
  border: none;
  background: #ef4444;
  color: #fff;
}

.create-btn,
.action-btn {
  min-width: 120px;
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

.session-layout {
  display: grid;
  grid-template-columns: 320px 1fr;
  gap: 16px;
  min-height: 560px;
}

.session-list-card {
  display: flex;
  flex-direction: column;
  gap: 10px;
  overflow-y: auto;
}

.session-item {
  width: 100%;
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

.session-scene,
.session-item-bottom,
.detail-meta p,
.message-item-top {
  font-size: 12px;
  color: #6b7280;
}

.detail-top {
  align-items: flex-start;
}

.detail-meta h2 {
  margin: 0;
  font-size: 20px;
  color: #111827;
}

.detail-meta p {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin: 8px 0 0;
}

.message-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  max-height: 360px;
  overflow-y: auto;
}

.message-item {
  border-radius: 12px;
  padding: 12px;
}

.message-item.user {
  background: #eff6ff;
}

.message-item.assistant {
  background: #f0fdf4;
}

.message-item.system,
.message-item.tool {
  background: #f9fafb;
}

.message-content {
  margin: 8px 0 0;
  white-space: pre-wrap;
  word-break: break-word;
  font-size: 13px;
  line-height: 1.6;
  color: #111827;
  font-family: inherit;
}

.composer-card {
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  padding: 14px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.empty-block {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 180px;
  color: #9ca3af;
  font-size: 13px;
}

.empty-block.small {
  min-height: 120px;
}

@media (max-width: 1100px) {
  .session-layout,
  .toolbar-grid,
  .create-grid,
  .update-grid,
  .detail-top,
  .page-header {
    grid-template-columns: 1fr;
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
