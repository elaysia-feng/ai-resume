<template>
  <div class="profile-page">
    <header class="page-head">
      <div class="page-head__text">
        <h1 class="ui-page-title">个人档案</h1>
        <p class="ui-page-subtitle">查看账号资料、简历与 AI 使用情况，这些信息会作为简历生成与面试的默认上下文。</p>
      </div>
      <div class="page-head__actions">
        <button class="ui-btn ui-btn--outline" :disabled="loading" @click="refreshAll">
          <AppIcon name="arrows-clockwise" :size="16" />
          {{ loading ? '刷新中…' : '刷新资料' }}
        </button>
        <button class="ui-btn ui-btn--primary" @click="goCreateResume">
          <AppIcon name="file-plus" :size="16" />
          新建简历
        </button>
      </div>
    </header>

    <div class="profile-layout">
      <!-- 左侧分区目录 -->
      <nav class="section-nav" aria-label="档案分区目录">
        <p class="section-nav__caption">档案分区</p>
        <button
          v-for="section in SECTIONS"
          :key="section.id"
          type="button"
          class="section-nav__item"
          :class="{ 'is-active': activeSection === section.id }"
          :aria-current="activeSection === section.id ? 'true' : undefined"
          @click="activeSection = section.id"
        >
          <span class="section-nav__icon" aria-hidden="true">
            <AppIcon :name="section.icon" :size="16" />
          </span>
          <span class="section-nav__body">
            <span class="section-nav__title">{{ section.title }}</span>
            <span class="section-nav__meta">{{ sectionMeta(section.id) }}</span>
          </span>
        </button>
      </nav>

      <div class="section-body">
        <!-- 分区一：档案概览 -->
        <section v-show="activeSection === 'overview'" class="section-stack">
          <!-- 账号身份卡片 -->
          <article class="ui-panel avatar-card">
            <div class="avatar-preview">
              <button type="button" class="avatar-shell" :disabled="avatarBusy" @click="triggerAvatarUpload">
                <img v-if="authStore.user?.avatarUrl" :src="authStore.user.avatarUrl" alt="头像预览" class="avatar-image" />
                <span v-else class="avatar-text">{{ userInitials }}</span>
              </button>
              <input ref="avatarInputRef" type="file" accept="image/*" class="hidden-input" @change="handleAvatarChange" />
            </div>
            <div class="avatar-info">
              <div class="avatar-info__head">
                <h2 class="avatar-info__name">{{ displayName }}</h2>
                <span class="ui-badge" :class="authStore.user?.enabled === false ? 'ui-badge--danger' : 'ui-badge--success'">
                  {{ accountStatusText }}
                </span>
              </div>
              <p class="avatar-info__email">{{ accountEmailText }}</p>
              <div class="avatar-actions">
                <button class="ui-btn ui-btn--outline ui-btn--sm" :disabled="avatarBusy" @click="triggerAvatarUpload">
                  <AppIcon name="upload-simple" :size="14" />
                  {{ avatarBusy ? '上传中…' : authStore.user?.avatarUrl ? '替换账号头像' : '上传账号头像' }}
                </button>
                <span class="avatar-tip">{{ avatarStatus || '账号头像会上传到本机后端并写入当前账号。' }}</span>
              </div>
              <p class="avatar-note">这是账号身份的头像（显示在侧边栏），与简历里的照片无关；后端暂未提供删除头像的接口，需要恢复默认头像时可上传一张新图片覆盖。</p>
            </div>
          </article>

          <!-- 使用统计 -->
          <article class="ui-panel stat-card">
            <div class="ui-section-head">
              <div>
                <h2 class="ui-section-head__title">使用统计</h2>
                <p class="ui-section-head__hint">按当前账号统计</p>
              </div>
            </div>
            <div class="stat-grid">
              <div class="ui-stat stat-item">
                <span class="ui-stat__value">{{ resumes.length }}</span>
                <span class="ui-stat__label">简历数量</span>
                <span class="stat-item__desc">当前账号下所有简历</span>
              </div>
              <div class="ui-stat stat-item">
                <span class="ui-stat__value">{{ sessions.length }}</span>
                <span class="ui-stat__label">Agent 会话</span>
                <span class="stat-item__desc">已创建的 AI 会话历史</span>
              </div>
              <div class="ui-stat stat-item">
                <span class="ui-stat__value stat-item__value--text">{{ lastResumeUpdateText }}</span>
                <span class="ui-stat__label">最近简历更新</span>
                <span class="stat-item__desc">按简历更新时间统计</span>
              </div>
              <div class="ui-stat stat-item">
                <span class="ui-stat__value stat-item__value--text">{{ lastSessionUpdateText }}</span>
                <span class="ui-stat__label">最近 AI 活跃</span>
                <span class="stat-item__desc">按会话最后消息时间统计</span>
              </div>
            </div>
          </article>
        </section>

        <!-- 分区二：账号资料 -->
        <section v-show="activeSection === 'account'" class="ui-panel block">
          <header class="card-head">
            <div>
              <h2 class="card-head__title">账号资料</h2>
              <p class="card-head__desc">这里是账号身份（「我是谁」），只影响应用内的展示。</p>
            </div>
            <button class="ui-btn ui-btn--outline ui-btn--sm" :disabled="loading" @click="refreshAll">
              {{ loading ? '刷新中…' : '刷新' }}
            </button>
          </header>

          <!-- 账号身份 -->
          <div class="ui-section-head">
            <div>
              <h3 class="ui-section-head__title">账号身份</h3>
              <p class="ui-section-head__hint">昵称、账号头像与账号类型</p>
            </div>
          </div>
          <dl class="info-grid">
            <div class="info-item">
              <dt class="info-item__label">账号昵称</dt>
              <dd class="info-item__value">{{ authStore.user?.username || '未命名用户' }}</dd>
            </div>
            <div class="info-item">
              <dt class="info-item__label">账号头像</dt>
              <dd class="info-item__value">
                <span class="account-avatar">
                  <img v-if="authStore.user?.avatarUrl" :src="authStore.user.avatarUrl" alt="账号头像" />
                  <span v-else>{{ userInitials }}</span>
                </span>
              </dd>
            </div>
            <div class="info-item">
              <dt class="info-item__label">账号类型</dt>
              <dd class="info-item__value">{{ accountTypeText }}</dd>
            </div>
            <div class="info-item">
              <dt class="info-item__label">邮箱</dt>
              <dd class="info-item__value">{{ accountEmailText }}</dd>
            </div>
            <div class="info-item">
              <dt class="info-item__label">用户 ID</dt>
              <dd class="info-item__value info-item__value--mono">{{ authStore.user?.userId || '-' }}</dd>
            </div>
            <div class="info-item">
              <dt class="info-item__label">账号状态</dt>
              <dd class="info-item__value">{{ accountStatusText }}</dd>
            </div>
            <div class="info-item">
              <dt class="info-item__label">邮箱状态</dt>
              <dd class="info-item__value">{{ accountEmailStatusText }}</dd>
            </div>
            <div class="info-item">
              <dt class="info-item__label">创建时间</dt>
              <dd class="info-item__value">{{ formatDateTime(authStore.user?.createdAt) }}</dd>
            </div>
            <div class="info-item">
              <dt class="info-item__label">最后更新</dt>
              <dd class="info-item__value">{{ formatDateTime(authStore.user?.updatedAt) }}</dd>
            </div>
          </dl>

          <!-- 简历个人信息：与账号身份分开，避免概念混淆 -->
          <div class="ui-section-head section-head--spaced">
            <div>
              <h3 class="ui-section-head__title">简历个人信息</h3>
              <p class="ui-section-head__hint">姓名与照片</p>
            </div>
          </div>
          <dl class="info-grid">
            <div class="info-item">
              <dt class="info-item__label">简历数量</dt>
              <dd class="info-item__value">{{ resumes.length }} 份</dd>
            </div>
          </dl>
          <div class="ui-callout ui-callout--accent identity-note">
            <AppIcon name="info" :size="16" />
            <span>简历里的姓名和照片要在每份简历里单独填写，两者互不影响。</span>
          </div>
        </section>

        <!-- 分区三：我的简历 -->
        <section v-show="activeSection === 'resumes'" class="ui-panel block">
          <div class="ui-section-head">
            <div>
              <h2 class="ui-section-head__title">最近简历</h2>
              <p class="ui-section-head__hint">最多展示 4 条最近更新的简历。</p>
            </div>
            <button class="ui-btn ui-btn--ghost ui-btn--sm" @click="goDashboard">全部简历</button>
          </div>
          <div v-if="recentResumes.length" class="list-block">
            <button v-for="resume in recentResumes" :key="resume.id" type="button" class="list-item" @click="openResume(resume.id)">
              <span class="list-item__main">
                <strong class="list-item__title">{{ resume.title || '未命名简历' }}</strong>
                <span class="list-item__desc">{{ resume.template || '未设置模板' }}</span>
              </span>
              <span class="list-item__time">{{ formatDateTime(resume.updatedAt || resume.createdAt) }}</span>
            </button>
          </div>
          <div v-else class="ui-empty">
            <span class="ui-empty__icon" aria-hidden="true">
              <AppIcon name="files" :size="24" />
            </span>
            <p class="ui-empty__title">还没有简历记录</p>
            <p class="ui-empty__hint">新建一份简历后，AI 对话与模拟面试都会以它作为上下文。</p>
            <button class="ui-btn ui-btn--primary" @click="goCreateResume">新建简历</button>
          </div>
        </section>

        <!-- 分区四：AI 会话 -->
        <section v-show="activeSection === 'sessions'" class="ui-panel block">
          <div class="ui-section-head">
            <div>
              <h2 class="ui-section-head__title">最近 AI 会话</h2>
              <p class="ui-section-head__hint">最多展示 4 条最近活跃会话。</p>
            </div>
            <button class="ui-btn ui-btn--ghost ui-btn--sm" @click="goHistory">全部会话</button>
          </div>
          <div v-if="recentSessions.length" class="list-block">
            <button v-for="session in recentSessions" :key="session.id" type="button" class="list-item" @click="goHistory">
              <span class="list-item__main">
                <strong class="list-item__title">{{ session.sessionTitle || '未命名会话' }}</strong>
                <span class="list-item__desc">{{ sceneLabel(session.sceneCode) }}</span>
              </span>
              <span class="list-item__time">{{ formatDateTime(session.lastMessageAt || session.updatedAt || session.createdAt) }}</span>
            </button>
          </div>
          <div v-else class="ui-empty">
            <span class="ui-empty__icon" aria-hidden="true">
              <AppIcon name="chat-circle-dots" :size="24" />
            </span>
            <p class="ui-empty__title">还没有 AI 会话记录</p>
            <p class="ui-empty__hint">和 AI 对话一次，这里就会出现你的会话历史。</p>
            <button class="ui-btn ui-btn--primary" @click="goChat">去和 AI 对话</button>
          </div>
        </section>

        <!-- 分区五：快捷操作 -->
        <section v-show="activeSection === 'actions'" class="ui-panel block">
          <div class="ui-section-head">
            <div>
              <h2 class="ui-section-head__title">快捷操作</h2>
              <p class="ui-section-head__hint">常用入口集中放在这里。</p>
            </div>
          </div>
          <div class="action-grid">
            <button type="button" class="action-tile" @click="goCreateResume">
              <span class="ui-icon-tile ui-icon-tile--sm action-tile__icon" aria-hidden="true">
                <AppIcon name="file-plus" :size="16" />
              </span>
              <span class="action-title">新建简历</span>
              <span class="action-desc">直接进入编辑器创建新简历</span>
            </button>
            <button type="button" class="action-tile" @click="goDashboard">
              <span class="ui-icon-tile ui-icon-tile--sm action-tile__icon" aria-hidden="true">
                <AppIcon name="files" :size="16" />
              </span>
              <span class="action-title">查看全部简历</span>
              <span class="action-desc">进入简历列表管理已有内容</span>
            </button>
            <button type="button" class="action-tile" @click="goHistory">
              <span class="ui-icon-tile ui-icon-tile--sm action-tile__icon" aria-hidden="true">
                <AppIcon name="clock-counter-clockwise" :size="16" />
              </span>
              <span class="action-title">查看 Agent 会话</span>
              <span class="action-desc">进入 AI 历史与多轮对话页</span>
            </button>
            <button type="button" class="action-tile" :disabled="!authStore.user?.email" @click="copyEmail">
              <span class="ui-icon-tile ui-icon-tile--sm action-tile__icon" aria-hidden="true">
                <AppIcon name="copy" :size="16" />
              </span>
              <span class="action-title">复制邮箱</span>
              <span class="action-desc">{{ copyStatus || '快速复制当前绑定邮箱' }}</span>
            </button>
          </div>
        </section>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import AppIcon from '../components/common/AppIcon.vue'
import { useAuthStore } from '../store/authStore.js'
import { getResumes } from '../api/resumes.js'
import { listAgentSessions } from '../api/agentSessions.js'
import { extractApiMessage } from '../api/request.js'
import { isLocalIdentity } from '../api/localProfile.js'

// 左侧目录的分区，语义与原来的四块面板一致
const SECTIONS = [
  { id: 'overview', title: '档案概览', icon: 'user-circle' },
  { id: 'account', title: '账号资料', icon: 'identification-card' },
  { id: 'resumes', title: '我的简历', icon: 'files' },
  { id: 'sessions', title: 'AI 会话', icon: 'chat-circle-dots' },
  { id: 'actions', title: '快捷操作', icon: 'list-checks' },
]

// 会话场景码 -> 中文名称
const SCENE_LABELS = {
  CHAT: 'AI 对话',
  INTERVIEW: '模拟面试',
  JD_CUSTOMIZE: '简历定制',
  RESUME_OPTIMIZE: '简历优化',
  RESUME_REWRITE: '简历改写',
  OPTIMIZE: '简历优化',
  MATCH: '岗位匹配',
  SUMMARY: '内容总结',
}

const authStore = useAuthStore()
const router = useRouter()
const avatarInputRef = ref(null)
const loading = ref(false)
const avatarBusy = ref(false)
const avatarStatus = ref('')
const copyStatus = ref('')
const resumes = ref([])
const sessions = ref([])
const activeSection = ref('overview')

const userInitials = computed(() => {
  const name = authStore.user?.username || 'U'
  return name.charAt(0).toUpperCase()
})

const displayName = computed(() => authStore.user?.username || '未命名用户')
const accountStatusText = computed(() => (authStore.user?.enabled === false ? '账号停用' : '账号正常'))

// 本地身份（桌面端首次启动创建的那一个）没有真实邮箱，展示占位邮箱没意义
const isLocalIdentityUser = computed(() => isLocalIdentity(authStore.user))
const accountTypeText = computed(() => (isLocalIdentityUser.value ? '本地身份（桌面端）' : '邮箱账号'))
const accountEmailText = computed(() => {
  if (isLocalIdentityUser.value) {
    return '本地身份，无需邮箱'
  }
  return authStore.user?.email || '未获取到邮箱信息'
})
const accountEmailStatusText = computed(() => {
  if (isLocalIdentityUser.value) {
    return '不适用（本地身份）'
  }
  return authStore.user?.email ? '已绑定' : '未绑定'
})

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

// 目录右侧的一行摘要
function sectionMeta(sectionId) {
  if (sectionId === 'overview') {
    return displayName.value
  }
  if (sectionId === 'account') {
    return accountTypeText.value
  }
  if (sectionId === 'resumes') {
    return `${resumes.value.length} 份简历`
  }
  if (sectionId === 'sessions') {
    return `${sessions.value.length} 条会话`
  }
  return '常用入口'
}

function sceneLabel(sceneCode) {
  if (!sceneCode) {
    return '通用对话'
  }
  return SCENE_LABELS[sceneCode] || sceneCode
}

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

function goChat() {
  router.push('/chat')
}

function openResume(resumeId) {
  router.push(`/editor/${resumeId}`)
}
</script>

<style scoped>
.profile-page {
  display: flex;
  flex-direction: column;
  gap: var(--ui-space-6);
  max-width: 1080px;
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

.page-head__actions {
  display: flex;
  flex-shrink: 0;
  gap: var(--ui-space-2);
}

/* ---------- 布局：左侧目录 + 右侧分区 ---------- */
.profile-layout {
  display: grid;
  grid-template-columns: 208px minmax(0, 1fr);
  gap: var(--ui-space-6);
  align-items: start;
}

.section-nav {
  display: flex;
  flex-direction: column;
  gap: var(--ui-space-1);
  position: sticky;
  top: 0;
}

.section-nav__caption {
  margin: 0 0 var(--ui-space-2);
  padding-left: var(--ui-space-3);
  font-size: var(--ui-text-xs);
  color: var(--ui-fg-muted);
}

.section-nav__item {
  display: flex;
  align-items: center;
  gap: var(--ui-space-2);
  width: 100%;
  padding: var(--ui-space-2) var(--ui-space-3);
  border: 1px solid transparent;
  border-radius: var(--ui-radius-md);
  background: transparent;
  color: var(--ui-fg-secondary);
  font-family: inherit;
  text-align: left;
  cursor: pointer;
  transition: background var(--ui-duration-fast) var(--ui-ease-in-out),
    color var(--ui-duration-fast) var(--ui-ease-in-out),
    border-color var(--ui-duration-fast) var(--ui-ease-in-out);
}

.section-nav__item:hover {
  background: var(--ui-bg-hover);
  color: var(--ui-fg-primary);
}

.section-nav__item:focus-visible {
  outline: none;
  box-shadow: var(--ui-ring);
}

.section-nav__item.is-active {
  background: var(--ui-bg-selected);
  border-color: var(--ui-accent-border);
  color: var(--ui-fg-primary);
}

.section-nav__icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  width: 26px;
  height: 26px;
  border-radius: var(--ui-radius-sm);
  background: var(--ui-bg-sunken);
  color: var(--ui-fg-secondary);
}

.section-nav__item.is-active .section-nav__icon {
  background: var(--ui-accent-soft);
  color: var(--ui-fg-accent);
}

.section-nav__body {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.section-nav__title {
  font-size: var(--ui-text-base);
  font-weight: 600;
}

.section-nav__meta {
  font-size: var(--ui-text-xs);
  color: var(--ui-fg-muted);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.section-nav__item.is-active .section-nav__meta {
  color: var(--ui-fg-secondary);
}

.section-body {
  min-width: 0;
}

.section-stack {
  display: flex;
  flex-direction: column;
  gap: var(--ui-space-4);
}

/* ---------- 卡片 ---------- */
.block,
.stat-card,
.avatar-card {
  padding: var(--ui-space-6);
}

.card-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--ui-space-4);
  padding-bottom: var(--ui-space-4);
  margin-bottom: var(--ui-space-5);
  border-bottom: 1px solid var(--ui-border);
}

.card-head__title {
  margin: 0;
  font-size: var(--ui-text-lg);
  font-weight: 600;
  letter-spacing: var(--ui-tracking-snug);
  color: var(--ui-fg-primary);
}

.card-head__desc {
  margin: var(--ui-space-1) 0 0;
  font-size: var(--ui-text-sm);
  line-height: var(--ui-line-normal);
  color: var(--ui-fg-secondary);
}

/* 账号身份与简历个人信息之间的分隔 */
.section-head--spaced {
  margin-top: var(--ui-space-6);
}

.identity-note {
  margin-top: var(--ui-space-3);
}

/* ---------- 头像卡片 ---------- */
.avatar-card {
  display: flex;
  align-items: flex-start;
  gap: var(--ui-space-6);
}

.avatar-preview {
  flex-shrink: 0;
}

.avatar-shell {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 104px;
  height: 104px;
  border-radius: var(--ui-radius-full);
  border: 1px solid var(--ui-border);
  background: var(--ui-accent-soft);
  color: var(--ui-fg-accent);
  font-size: var(--ui-text-3xl);
  font-weight: 700;
  cursor: pointer;
  overflow: hidden;
  transition: border-color var(--ui-duration-fast) var(--ui-ease-in-out),
    box-shadow var(--ui-duration-fast) var(--ui-ease-in-out);
}

.avatar-shell:hover:not(:disabled) {
  border-color: var(--ui-accent-border);
  box-shadow: var(--ui-shadow-sm);
}

.avatar-shell:focus-visible {
  outline: none;
  box-shadow: var(--ui-ring);
}

.avatar-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.hidden-input {
  display: none;
}

.avatar-info {
  display: flex;
  flex-direction: column;
  gap: var(--ui-space-2);
  min-width: 0;
  flex: 1;
}

.avatar-info__head {
  display: flex;
  align-items: center;
  gap: var(--ui-space-3);
}

.avatar-info__name {
  margin: 0;
  font-size: var(--ui-text-xl);
  font-weight: 600;
  letter-spacing: var(--ui-tracking-snug);
  color: var(--ui-fg-primary);
}

.avatar-info__email {
  margin: 0;
  font-size: var(--ui-text-base);
  color: var(--ui-fg-secondary);
  word-break: break-word;
}

.avatar-actions {
  display: flex;
  align-items: center;
  gap: var(--ui-space-3);
  margin-top: var(--ui-space-2);
}

.avatar-tip {
  font-size: var(--ui-text-sm);
  color: var(--ui-fg-muted);
}

.avatar-note {
  margin: 0;
  font-size: var(--ui-text-xs);
  line-height: var(--ui-line-normal);
  color: var(--ui-fg-muted);
}

/* ---------- 统计 ---------- */
.stat-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(170px, 1fr));
  gap: var(--ui-space-4);
}

.stat-item {
  gap: var(--ui-space-1);
  padding: var(--ui-space-4);
  border: 1px solid var(--ui-border);
  border-radius: var(--ui-radius-md);
  background: var(--ui-bg-sunken);
  min-width: 0;
}

.stat-item__value--text {
  font-size: var(--ui-text-base);
  font-weight: 600;
}

.stat-item__desc {
  font-size: var(--ui-text-xs);
  color: var(--ui-fg-muted);
}

/* ---------- 账号资料 ---------- */
.info-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: var(--ui-space-4);
  margin: 0;
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: var(--ui-space-1);
  min-width: 0;
  padding: var(--ui-space-3) var(--ui-space-4);
  border: 1px solid var(--ui-border);
  border-radius: var(--ui-radius-md);
  background: var(--ui-bg-sunken);
}

.info-item__label {
  font-size: var(--ui-text-xs);
  color: var(--ui-fg-muted);
}

.info-item__value {
  margin: 0;
  font-size: var(--ui-text-base);
  font-weight: 600;
  color: var(--ui-fg-primary);
  word-break: break-word;
}

.info-item__value--mono {
  font-family: var(--ui-font-mono);
  font-weight: 500;
}

/* 账号资料里的账号头像小图 */
.account-avatar {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  border-radius: var(--ui-radius-full);
  background: var(--ui-accent-soft);
  border: 1px solid var(--ui-accent-border);
  color: var(--ui-fg-accent);
  font-size: var(--ui-text-base);
  font-weight: 600;
  overflow: hidden;
}

.account-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

/* ---------- 列表 ---------- */
.list-block {
  display: flex;
  flex-direction: column;
  gap: var(--ui-space-2);
}

.list-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--ui-space-4);
  width: 100%;
  padding: var(--ui-space-3) var(--ui-space-4);
  border: 1px solid var(--ui-border);
  border-radius: var(--ui-radius-md);
  background: var(--ui-bg-surface);
  color: var(--ui-fg-primary);
  text-align: left;
  cursor: pointer;
  transition: background var(--ui-duration-fast) var(--ui-ease-in-out),
    border-color var(--ui-duration-fast) var(--ui-ease-in-out);
}

.list-item:hover {
  background: var(--ui-bg-hover);
  border-color: var(--ui-border-strong);
}

.list-item:focus-visible {
  outline: none;
  box-shadow: var(--ui-ring);
}

.list-item__main {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.list-item__title {
  font-size: var(--ui-text-base);
  font-weight: 600;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.list-item__desc {
  font-size: var(--ui-text-xs);
  color: var(--ui-fg-muted);
}

.list-item__time {
  flex-shrink: 0;
  font-size: var(--ui-text-xs);
  color: var(--ui-fg-muted);
}

/* ---------- 快捷操作 ---------- */
.action-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
  gap: var(--ui-space-3);
}

.action-tile {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: var(--ui-space-2);
  padding: var(--ui-space-4);
  border: 1px solid var(--ui-border);
  border-radius: var(--ui-radius-md);
  background: var(--ui-bg-sunken);
  color: var(--ui-fg-primary);
  text-align: left;
  cursor: pointer;
  transition: border-color var(--ui-duration-fast) var(--ui-ease-in-out),
    background var(--ui-duration-fast) var(--ui-ease-in-out);
}

.action-tile:hover:not(:disabled) {
  border-color: var(--ui-accent-border);
  background: var(--ui-bg-hover);
}

.action-tile:focus-visible {
  outline: none;
  box-shadow: var(--ui-ring);
}

.action-tile:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

.action-tile__icon {
  margin-bottom: var(--ui-space-1);
}

.action-title {
  font-size: var(--ui-text-base);
  font-weight: 600;
}

.action-desc {
  font-size: var(--ui-text-xs);
  line-height: var(--ui-line-normal);
  color: var(--ui-fg-muted);
}

@media (max-width: 1100px) {
  .profile-layout {
    grid-template-columns: 1fr;
  }

  .section-nav {
    position: static;
    flex-direction: row;
    flex-wrap: wrap;
  }

  .section-nav__caption {
    display: none;
  }

  .section-nav__item {
    width: auto;
  }

  .stat-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 860px) {
  .info-grid,
  .action-grid {
    grid-template-columns: 1fr;
  }

  .page-head,
  .avatar-card {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
