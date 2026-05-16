<template>
  <div class="profile-page">
    <header class="profile-header">
      <div>
        <h1 class="page-title">个人中心</h1>
        <p class="page-subtitle">统一查看账号资料、最近简历和 AI 使用情况。</p>
      </div>
      <div class="header-actions">
        <button class="ghost-btn" @click="refreshAll" :disabled="loading">
          {{ loading ? '刷新中...' : '刷新资料' }}
        </button>
        <button class="primary-btn" @click="goCreateResume">新建简历</button>
      </div>
    </header>

    <section class="hero-card">
      <div class="hero-left">
        <div class="avatar-panel">
          <div class="avatar-shell" @click="triggerAvatarUpload">
            <img v-if="authStore.user?.avatarUrl" :src="authStore.user.avatarUrl" alt="avatar" class="avatar-image" />
            <span v-else class="avatar-text">{{ userInitials }}</span>
          </div>
          <input ref="avatarInputRef" type="file" accept="image/*" class="hidden-input" @change="handleAvatarChange" />
          <div class="avatar-actions">
            <button class="ghost-btn small" @click="triggerAvatarUpload" :disabled="avatarBusy">
              {{ avatarBusy ? '处理中...' : '上传头像' }}
            </button>
          </div>
          <p class="avatar-tip">{{ avatarStatus || '头像会上传到后端并写入当前账号。' }}</p>
        </div>

        <div class="hero-info">
          <div class="hero-badge">{{ accountStatusText }}</div>
          <h2 class="hero-name">{{ displayName }}</h2>
          <p class="hero-email">{{ authStore.user?.email || '未获取到邮箱信息' }}</p>
          <div class="info-grid">
            <div class="info-block">
              <span class="info-label">用户 ID</span>
              <span class="info-value">{{ authStore.user?.userId || '-' }}</span>
            </div>
            <div class="info-block">
              <span class="info-label">注册时间</span>
              <span class="info-value">{{ formatDateTime(authStore.user?.createdAt) }}</span>
            </div>
            <div class="info-block">
              <span class="info-label">最后更新</span>
              <span class="info-value">{{ formatDateTime(authStore.user?.updatedAt) }}</span>
            </div>
            <div class="info-block">
              <span class="info-label">邮箱状态</span>
              <span class="info-value">{{ authStore.user?.email ? '已绑定' : '未绑定' }}</span>
            </div>
          </div>
        </div>
      </div>

      <div class="stats-grid">
        <article class="stat-card">
          <span class="stat-label">简历数量</span>
          <strong class="stat-value">{{ resumes.length }}</strong>
          <span class="stat-desc">当前账号下所有简历</span>
        </article>
        <article class="stat-card">
          <span class="stat-label">Agent 会话</span>
          <strong class="stat-value">{{ sessions.length }}</strong>
          <span class="stat-desc">已创建的 AI 对话历史</span>
        </article>
        <article class="stat-card">
          <span class="stat-label">最近简历更新</span>
          <strong class="stat-value">{{ lastResumeUpdateText }}</strong>
          <span class="stat-desc">按简历更新时间统计</span>
        </article>
        <article class="stat-card">
          <span class="stat-label">最近 AI 活跃</span>
          <strong class="stat-value">{{ lastSessionUpdateText }}</strong>
          <span class="stat-desc">按会话最后消息时间统计</span>
        </article>
      </div>
    </section>

    <section class="content-grid">
      <article class="panel-card">
        <div class="panel-head">
          <div>
            <h3>快捷操作</h3>
            <p>常用入口集中放在这里。</p>
          </div>
        </div>
        <div class="action-grid">
          <button class="action-tile" @click="goCreateResume">
            <span class="action-title">新建简历</span>
            <span class="action-desc">直接进入编辑器创建新简历</span>
          </button>
          <button class="action-tile" @click="goDashboard">
            <span class="action-title">查看全部简历</span>
            <span class="action-desc">进入简历列表管理已有内容</span>
          </button>
          <button class="action-tile" @click="goHistory">
            <span class="action-title">查看 Agent 会话</span>
            <span class="action-desc">进入 AI 历史与多轮对话页</span>
          </button>
          <button class="action-tile" @click="copyEmail" :disabled="!authStore.user?.email">
            <span class="action-title">复制邮箱</span>
            <span class="action-desc">{{ copyStatus || '快速复制当前绑定邮箱' }}</span>
          </button>
        </div>
      </article>

      <article class="panel-card">
        <div class="panel-head">
          <div>
            <h3>最近简历</h3>
            <p>最多展示 4 条最近更新的简历。</p>
          </div>
          <button class="text-btn" @click="goDashboard">全部简历</button>
        </div>
        <div v-if="recentResumes.length" class="list-block">
          <button v-for="resume in recentResumes" :key="resume.id" class="list-item" @click="openResume(resume.id)">
            <div>
              <strong>{{ resume.title || '未命名简历' }}</strong>
              <p>{{ resume.template || '未设置模板' }}</p>
            </div>
            <span>{{ formatDateTime(resume.updatedAt || resume.createdAt) }}</span>
          </button>
        </div>
        <div v-else class="empty-box">还没有简历记录。</div>
      </article>

      <article class="panel-card">
        <div class="panel-head">
          <div>
            <h3>最近 Agent 会话</h3>
            <p>最多展示 4 条最近活跃会话。</p>
          </div>
          <button class="text-btn" @click="goHistory">全部会话</button>
        </div>
        <div v-if="recentSessions.length" class="list-block">
          <button v-for="session in recentSessions" :key="session.id" class="list-item" @click="goHistory">
            <div>
              <strong>{{ session.sessionTitle || '未命名会话' }}</strong>
              <p>{{ session.sceneCode || '通用对话' }}</p>
            </div>
            <span>{{ formatDateTime(session.lastMessageAt || session.updatedAt || session.createdAt) }}</span>
          </button>
        </div>
        <div v-else class="empty-box">还没有 Agent 会话记录。</div>
      </article>

      <article class="panel-card">
        <div class="panel-head">
          <div>
            <h3>账号说明</h3>
            <p>当前这部分偏前端工作台能力。</p>
          </div>
        </div>
        <ul class="tips-list">
          <li>用户名、邮箱、注册时间来自后端当前用户接口。</li>
          <li>头像上传会调用后端接口并更新当前账号头像地址。</li>
          <li>个人中心会汇总简历与 Agent 会话，避免只剩一页静态资料。</li>
        </ul>
      </article>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../store/authStore.js'
import { getResumes } from '../api/resumes.js'
import { listAgentSessions } from '../api/agentSessions.js'
import { extractApiMessage } from '../api/request.js'

const authStore = useAuthStore()
const router = useRouter()
const avatarInputRef = ref(null)
const loading = ref(false)
const avatarBusy = ref(false)
const avatarStatus = ref('')
const copyStatus = ref('')
const resumes = ref([])
const sessions = ref([])

const userInitials = computed(() => {
  const name = authStore.user?.username || 'U'
  return name.charAt(0).toUpperCase()
})

const displayName = computed(() => authStore.user?.username || '未命名用户')
const accountStatusText = computed(() => (authStore.user?.enabled === false ? '账号停用' : '账号正常'))

const recentResumes = computed(() =>
  [...resumes.value]
    .sort((a, b) => new Date(b.updatedAt || b.createdAt || 0) - new Date(a.updatedAt || a.createdAt || 0))
    .slice(0, 4)
)

const recentSessions = computed(() =>
  [...sessions.value]
    .sort((a, b) => new Date(b.lastMessageAt || b.updatedAt || b.createdAt || 0) - new Date(a.lastMessageAt || a.updatedAt || a.createdAt || 0))
    .slice(0, 4)
)

const lastResumeUpdateText = computed(() => {
  const latest = recentResumes.value[0]
  return latest ? formatDateTime(latest.updatedAt || latest.createdAt) : '暂无'
})

const lastSessionUpdateText = computed(() => {
  const latest = recentSessions.value[0]
  return latest ? formatDateTime(latest.lastMessageAt || latest.updatedAt || latest.createdAt) : '暂无'
})

onMounted(() => {
  refreshAll()
})

async function refreshAll() {
  loading.value = true
  try {
    await authStore.refreshCurrentUser()
  } catch {
    // 资料获取失败时保持本地兜底
  }

  try {
    const [resumeList, sessionList] = await Promise.all([
      getResumes(),
      listAgentSessions(),
    ])
    resumes.value = Array.isArray(resumeList) ? resumeList : []
    sessions.value = Array.isArray(sessionList) ? sessionList : []
  } catch {
    resumes.value = []
    sessions.value = []
  } finally {
    loading.value = false
  }
}

function formatDateTime(value) {
  if (!value) {
    return '暂无'
  }

  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return String(value)
  }

  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  }).format(date)
}

function triggerAvatarUpload() {
  avatarInputRef.value?.click()
}

async function handleAvatarChange(event) {
  const file = event.target.files?.[0]
  event.target.value = ''

  if (!file) {
    return
  }

  if (!file.type.startsWith('image/')) {
    avatarStatus.value = '请选择图片文件'
    return
  }

  avatarBusy.value = true
  try {
    await authStore.uploadAvatar(file)
    avatarStatus.value = '头像上传成功'
  } catch (error) {
    avatarStatus.value = extractApiMessage(error, '头像上传失败')
  } finally {
    avatarBusy.value = false
  }
}

async function copyEmail() {
  if (!authStore.user?.email) {
    return
  }

  try {
    await navigator.clipboard.writeText(authStore.user.email)
    copyStatus.value = '邮箱已复制'
    window.setTimeout(() => {
      copyStatus.value = ''
    }, 2000)
  } catch {
    copyStatus.value = '复制失败'
  }
}

function goCreateResume() {
  router.push('/resume/create')
}

function goDashboard() {
  router.push('/dashboard')
}

function goHistory() {
  router.push('/dashboard/history')
}

function openResume(resumeId) {
  router.push(`/editor/${resumeId}`)
}
</script>

<style scoped>
.profile-page {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.profile-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.page-subtitle {
  margin: 8px 0 0;
  font-size: 14px;
  color: #6b7280;
}

.header-actions {
  display: flex;
  gap: 12px;
}

.page-title {
  font-size: 24px;
  font-weight: 700;
  color: #1a1a2e;
  margin: 0;
}

.hero-card {
  background: #fff;
  border-radius: 20px;
  padding: 28px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.08);
  display: flex;
  gap: 24px;
  justify-content: space-between;
}

.hero-left {
  flex: 1;
  display: flex;
  gap: 24px;
}

.avatar-panel {
  width: 220px;
  flex-shrink: 0;
}

.avatar-shell {
  width: 132px;
  height: 132px;
  border-radius: 50%;
  background: linear-gradient(135deg, #22c55e, #14b8a6);
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  cursor: pointer;
  color: #fff;
  font-size: 48px;
  font-weight: 700;
  box-shadow: 0 18px 40px rgba(34, 197, 94, 0.22);
}

.avatar-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.hidden-input {
  display: none;
}

.avatar-actions {
  display: flex;
  gap: 10px;
  margin-top: 18px;
  flex-wrap: wrap;
}

.avatar-tip {
  margin: 12px 0 0;
  font-size: 12px;
  line-height: 1.6;
  color: #6b7280;
}

.hero-info {
  flex: 1;
  min-width: 0;
}

.hero-badge {
  display: inline-flex;
  align-items: center;
  padding: 6px 12px;
  border-radius: 999px;
  background: #dcfce7;
  color: #166534;
  font-size: 12px;
  font-weight: 700;
}

.hero-name {
  margin: 16px 0 8px;
  font-size: 30px;
  color: #111827;
}

.hero-email {
  margin: 0 0 20px;
  color: #4b5563;
  font-size: 15px;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.info-block {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 14px 16px;
  border-radius: 14px;
  background: #f8fafc;
}

.info-label {
  font-size: 12px;
  color: #6b7280;
}

.info-value {
  font-size: 14px;
  font-weight: 600;
  color: #111827;
}

.stats-grid {
  width: 320px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.stat-card {
  display: flex;
  flex-direction: column;
  gap: 6px;
  border-radius: 16px;
  padding: 18px;
  background: linear-gradient(180deg, #f8fafc, #ffffff);
  border: 1px solid #e5e7eb;
}

.stat-label {
  font-size: 12px;
  color: #6b7280;
  text-transform: uppercase;
  letter-spacing: 0.04em;
}

.stat-value {
  font-size: 20px;
  color: #111827;
}

.stat-desc {
  font-size: 13px;
  color: #6b7280;
}

.content-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 20px;
}

.panel-card {
  background: #fff;
  border-radius: 18px;
  padding: 22px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.06);
}

.panel-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 18px;
}

.panel-head h3 {
  margin: 0;
  font-size: 18px;
  color: #111827;
}

.panel-head p {
  margin: 6px 0 0;
  font-size: 13px;
  color: #6b7280;
}

.action-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.action-tile {
  text-align: left;
  padding: 16px;
  border-radius: 14px;
  border: 1px solid #e5e7eb;
  background: #f8fafc;
  cursor: pointer;
  transition: transform 0.15s, border-color 0.15s, box-shadow 0.15s;
}

.action-tile:hover {
  transform: translateY(-1px);
  border-color: #86efac;
  box-shadow: 0 10px 22px rgba(34, 197, 94, 0.08);
}

.action-title {
  display: block;
  font-size: 15px;
  font-weight: 700;
  color: #111827;
}

.action-desc {
  display: block;
  margin-top: 6px;
  font-size: 13px;
  color: #6b7280;
  line-height: 1.6;
}

.list-block {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.list-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  text-align: left;
  padding: 14px 16px;
  border: 1px solid #e5e7eb;
  border-radius: 14px;
  background: #fff;
  cursor: pointer;
}

.list-item strong {
  display: block;
  color: #111827;
  font-size: 14px;
}

.list-item p {
  margin: 6px 0 0;
  color: #6b7280;
  font-size: 12px;
}

.list-item span {
  flex-shrink: 0;
  color: #6b7280;
  font-size: 12px;
}

.empty-box {
  border-radius: 14px;
  padding: 28px 16px;
  background: #f8fafc;
  color: #6b7280;
  text-align: center;
  font-size: 14px;
}

.tips-list {
  margin: 0;
  padding-left: 18px;
  color: #4b5563;
  line-height: 1.8;
  font-size: 14px;
}

.primary-btn,
.ghost-btn,
.danger-btn,
.text-btn {
  border: none;
  cursor: pointer;
  transition: all 0.2s;
}

.primary-btn {
  padding: 10px 18px;
  border-radius: 10px;
  background: #22c55e;
  color: #fff;
  font-size: 14px;
  font-weight: 600;
}

.primary-btn:hover {
  background: #16a34a;
}

.ghost-btn,
.danger-btn {
  padding: 10px 16px;
  border-radius: 10px;
  background: #fff;
  border: 1px solid #d1d5db;
  font-size: 14px;
  font-weight: 600;
  color: #374151;
}

.ghost-btn.small,
.danger-btn.small {
  padding: 8px 12px;
  font-size: 13px;
}

.ghost-btn:hover {
  border-color: #86efac;
  color: #15803d;
}

.danger-btn {
  color: #b91c1c;
  border-color: #fecaca;
}

.danger-btn:hover {
  background: #fef2f2;
}

.text-btn {
  background: transparent;
  color: #16a34a;
  font-size: 13px;
  font-weight: 600;
}

.text-btn:hover {
  color: #15803d;
}

@media (max-width: 1100px) {
  .hero-card,
  .hero-left {
    flex-direction: column;
  }

  .stats-grid {
    width: 100%;
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 900px) {
  .content-grid,
  .action-grid,
  .info-grid,
  .stats-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .profile-header,
  .panel-head,
  .header-actions {
    flex-direction: column;
    align-items: stretch;
  }

  .list-item {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
