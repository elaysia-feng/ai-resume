<template>
  <div class="home-page">
    <!-- 页头 -->
    <header class="page-head">
      <div class="page-head__text">
        <h1 class="page-title">我的简历</h1>
        <p class="page-subtitle">继续编辑已有简历，或者直接开始一次 AI 对话、模拟面试。</p>
      </div>
      <div class="page-head__actions">
        <button class="ui-btn ui-btn--primary" :disabled="creating" @click="handleCreate">
          {{ creating ? '创建中…' : '新建简历' }}
        </button>
      </div>
    </header>

    <!-- 快捷入口 -->
    <section class="quick-grid" aria-label="快捷入口">
      <button
        v-for="item in quickActions"
        :key="item.key"
        type="button"
        class="ui-card quick-item"
        @click="handleQuickAction(item)"
      >
        <span class="quick-item__icon" aria-hidden="true">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round">
            <path v-for="(d, i) in iconPaths[item.icon]" :key="i" :d="d" />
          </svg>
        </span>
        <span class="quick-item__body">
          <span class="quick-item__title">{{ item.title }}</span>
          <span class="quick-item__desc">{{ item.desc }}</span>
        </span>
      </button>
    </section>

    <div class="home-body">
      <!-- 简历列表 -->
      <section class="panel">
        <header class="panel__head">
          <h2 class="panel__title">简历列表</h2>
          <span class="ui-badge">{{ resumeStore.resumes.length }} 份</span>
        </header>

        <!-- 加载中 -->
        <div v-if="resumeStore.loading && resumeStore.resumes.length === 0" class="ui-card state-card">
          <ul class="skeleton-list" aria-hidden="true">
            <li v-for="index in 3" :key="index" class="skeleton-row"></li>
          </ul>
          <p class="state-text">正在加载简历…</p>
        </div>

        <!-- 加载失败 -->
        <div v-else-if="resumeStore.error && resumeStore.resumes.length === 0" class="ui-card state-card">
          <div class="ui-empty">
            <p class="ui-empty__title">简历列表加载失败</p>
            <p class="ui-empty__hint">{{ resumeStore.error }}，请检查后端服务是否已启动后重试。</p>
            <button class="ui-btn ui-btn--primary" @click="resumeStore.fetchResumes()">重新加载</button>
          </div>
        </div>

        <!-- 空状态 -->
        <div v-else-if="resumeStore.resumes.length === 0" class="ui-card state-card">
          <div class="ui-empty">
            <p class="ui-empty__title">{{ greeting }}你还没有简历</p>
            <p class="ui-empty__hint">
              新建一份空白简历开始写，或者让 AI 帮你从零起草；按 N 键可以快速新建。
            </p>
            <div class="ui-empty__actions">
              <button class="ui-btn ui-btn--primary" :disabled="creating" @click="handleCreate">新建简历</button>
              <router-link class="ui-btn ui-btn--outline" to="/chat">去和 AI 对话</router-link>
            </div>
          </div>
        </div>

        <!-- 简历卡片 -->
        <div v-else class="resume-grid">
          <ResumeCard
            v-for="resume in resumeStore.resumes"
            :key="resume.id"
            :resume="resume"
          />
        </div>
      </section>

      <!-- 侧栏：概览 + 最近对话 -->
      <aside class="side">
        <section class="ui-card side-card">
          <h2 class="side-card__title">概览</h2>
          <dl class="stat-list">
            <div class="stat">
              <dt>简历</dt>
              <dd>{{ resumeStore.resumes.length }} 份</dd>
            </div>
            <div class="stat">
              <dt>对话</dt>
              <dd>{{ recentSessions.length }} 条</dd>
            </div>
            <div class="stat">
              <dt>最近更新</dt>
              <dd>{{ lastUpdatedText }}</dd>
            </div>
          </dl>
        </section>

        <section class="ui-card side-card">
          <header class="side-card__head">
            <h2 class="side-card__title">最近对话</h2>
            <router-link class="ui-btn ui-btn--ghost ui-btn--sm" to="/chat">全部</router-link>
          </header>

          <p v-if="sessionsLoading" class="state-text state-text--left">正在加载对话…</p>
          <p v-else-if="recentSessions.length === 0" class="side-card__empty">
            还没有对话记录，去 AI 对话说说你想投的岗位。
          </p>
          <ul v-else class="session-list">
            <li v-for="session in recentSessions" :key="session.id">
              <router-link class="session-item" to="/chat" title="在 AI 对话中查看">
                <span class="session-item__title">{{ session.title || '未命名对话' }}</span>
                <span class="session-item__meta">
                  {{ formatDateTime(session.lastMessageAt) }} · {{ session.messageCount ?? 0 }} 条消息
                </span>
              </router-link>
            </li>
          </ul>
        </section>
      </aside>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { useResumeStore } from '../store/resumeStore.js';
import { useAuthStore } from '../store/authStore.js';
import { listChatSessions } from '../api/chat.js';
import { formatDateTime } from '../utils/resume.js';
import ResumeCard from '../components/ResumeCard.vue';

const resumeStore = useResumeStore();
const authStore = useAuthStore();
const router = useRouter();

const creating = ref(false);
const sessionsLoading = ref(false);
const recentSessions = ref([]);

// 快捷入口：新建简历走当前页的创建流程，其余直接跳转
const quickActions = [
  { key: 'create', icon: 'filePlus', title: '新建简历', desc: '从空白简历开始，边写边存' },
  { key: 'chat', icon: 'chat', title: 'AI 对话', desc: '让 AI 帮你起草或改写简历', to: '/chat' },
  { key: 'templates', icon: 'template', title: '模板市场', desc: '挑一套版式，套用后导出 PDF', to: '/dashboard/templates' },
  { key: 'interview', icon: 'mic', title: '模拟面试', desc: '按目标岗位出题并逐轮点评', to: '/dashboard/interview' },
];

// 内联 SVG 图标（离线可用，不引第三方图标库）
const iconPaths = {
  filePlus: [
    'M15 3H6a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z',
    'M14 3v5h5',
    'M9 14h6',
    'M12 11v6',
  ],
  chat: ['M21 11.5a8.5 8.5 0 0 1-8.5 8.5c-1.6 0-3.1-.45-4.3-1.2L3 20.5l1.7-4.8a8.5 8.5 0 1 1 16.3-4.2z'],
  template: ['M4 4h16v16H4z', 'M4 9h16', 'M9 20V9'],
  mic: ['M12 3a3 3 0 0 0-3 3v6a3 3 0 0 0 6 0V6a3 3 0 0 0-3-3z', 'M19 11v1a7 7 0 0 1-14 0v-1', 'M12 19v3', 'M8 22h8'],
};

const greeting = computed(() => {
  const name = authStore.currentUser?.username;
  return name ? `${name}，` : '';
});

// 最近一次简历更新时间
const lastUpdatedText = computed(() => {
  const times = resumeStore.resumes
    .map((resume) => new Date(resume.updatedAt || 0).getTime())
    .filter((time) => !Number.isNaN(time) && time > 0);
  if (!times.length) {
    return '暂无';
  }
  return new Date(Math.max(...times)).toLocaleDateString('zh-CN');
});

onMounted(() => {
  resumeStore.fetchResumes();
  loadRecentSessions();
  window.addEventListener('keydown', handleKeydown);
});

onUnmounted(() => {
  window.removeEventListener('keydown', handleKeydown);
});

function handleKeydown(e) {
  if (e.key !== 'n' && e.key !== 'N') {
    return;
  }
  // 正在输入框里打字时不触发快捷键
  const tag = e.target?.tagName;
  if (tag === 'INPUT' || tag === 'TEXTAREA' || e.target?.isContentEditable) {
    return;
  }
  handleCreate();
}

function handleQuickAction(item) {
  if (item.to) {
    router.push(item.to);
    return;
  }
  handleCreate();
}

async function handleCreate() {
  if (creating.value) {
    return;
  }
  creating.value = true;
  try {
    const created = await resumeStore.createResume({ title: 'Untitled Resume' });
    const id = created?.id || created?._id;
    if (id) {
      router.push(`/editor/${id}`);
    }
  } catch {
    // error is set on the store
  } finally {
    creating.value = false;
  }
}

async function loadRecentSessions() {
  sessionsLoading.value = true;
  try {
    const list = await listChatSessions();
    recentSessions.value = (Array.isArray(list) ? list : []).slice(0, 4);
  } catch {
    // 概览信息拿不到不影响主流程，静默留空
    recentSessions.value = [];
  } finally {
    sessionsLoading.value = false;
  }
}
</script>

<style scoped>
/* ========================================
   工作台首页：页头 + 快捷入口 + 简历列表 + 侧栏概览
   ======================================== */
.home-page {
  display: flex;
  flex-direction: column;
  gap: var(--ui-space-6);
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
  flex-shrink: 0;
  gap: var(--ui-space-2);
}

/* ---------- 快捷入口 ---------- */
.quick-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: var(--ui-space-4);
}

.quick-item {
  display: flex;
  align-items: flex-start;
  gap: var(--ui-space-3);
  padding: var(--ui-space-4);
  text-align: left;
  cursor: pointer;
  transition: border-color var(--ui-duration-fast) var(--ui-ease-in-out),
    box-shadow var(--ui-duration-fast) var(--ui-ease-in-out),
    background var(--ui-duration-fast) var(--ui-ease-in-out);
}

.quick-item:hover {
  border-color: var(--ui-border-strong);
  box-shadow: var(--ui-shadow-md);
}

.quick-item:focus-visible {
  outline: none;
  box-shadow: var(--ui-ring);
}

.quick-item__icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  width: 32px;
  height: 32px;
  border-radius: var(--ui-radius-md);
  background: var(--ui-accent-soft);
  color: var(--ui-fg-accent);
}

.quick-item__icon svg {
  width: 18px;
  height: 18px;
}

.quick-item__body {
  display: flex;
  flex-direction: column;
  gap: var(--ui-space-1);
  min-width: 0;
}

.quick-item__title {
  font-size: var(--ui-text-base);
  font-weight: 600;
  color: var(--ui-fg-primary);
}

.quick-item__desc {
  font-size: var(--ui-text-sm);
  color: var(--ui-fg-secondary);
  line-height: var(--ui-line-normal);
}

/* ---------- 主体两栏 ---------- */
.home-body {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 320px;
  gap: var(--ui-space-5);
  align-items: start;
}

.panel {
  display: flex;
  flex-direction: column;
  gap: var(--ui-space-4);
  min-width: 0;
}

.panel__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--ui-space-3);
}

.panel__title {
  margin: 0;
  font-size: var(--ui-text-lg);
  font-weight: 600;
  color: var(--ui-fg-primary);
}

.resume-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: var(--ui-space-4);
}

/* ---------- 状态块 ---------- */
.state-card {
  padding: var(--ui-space-6);
}

.state-text {
  margin: var(--ui-space-4) 0 0;
  text-align: center;
  font-size: var(--ui-text-sm);
  color: var(--ui-fg-muted);
}

.state-text--left {
  margin: 0;
  text-align: left;
}

.ui-empty__actions {
  display: flex;
  flex-wrap: wrap;
  gap: var(--ui-space-2);
  justify-content: center;
}

.skeleton-list {
  display: flex;
  flex-direction: column;
  gap: var(--ui-space-3);
  margin: 0;
  padding: 0;
  list-style: none;
}

.skeleton-row {
  height: 108px;
  border-radius: var(--ui-radius-lg);
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

/* ---------- 侧栏 ---------- */
.side {
  display: flex;
  flex-direction: column;
  gap: var(--ui-space-4);
  min-width: 0;
}

.side-card {
  padding: var(--ui-space-5);
  display: flex;
  flex-direction: column;
  gap: var(--ui-space-3);
}

.side-card__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--ui-space-2);
}

.side-card__title {
  margin: 0;
  font-size: var(--ui-text-base);
  font-weight: 600;
  color: var(--ui-fg-primary);
}

.side-card__empty {
  margin: 0;
  font-size: var(--ui-text-sm);
  line-height: var(--ui-line-normal);
  color: var(--ui-fg-muted);
}

.stat-list {
  display: flex;
  flex-direction: column;
  gap: var(--ui-space-2);
  margin: 0;
}

.stat {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: var(--ui-space-3);
}

.stat dt {
  font-size: var(--ui-text-sm);
  color: var(--ui-fg-secondary);
}

.stat dd {
  margin: 0;
  font-size: var(--ui-text-sm);
  font-weight: 600;
  color: var(--ui-fg-primary);
  text-align: right;
}

/* ---------- 最近对话 ---------- */
.session-list {
  display: flex;
  flex-direction: column;
  gap: var(--ui-space-1);
  margin: 0;
  padding: 0;
  list-style: none;
}

.session-item {
  display: flex;
  flex-direction: column;
  gap: 2px;
  padding: var(--ui-space-2) var(--ui-space-3);
  border-radius: var(--ui-radius-md);
  transition: background var(--ui-duration-fast) var(--ui-ease-in-out);
}

.session-item:hover {
  background: var(--ui-bg-hover);
}

.session-item:focus-visible {
  outline: none;
  box-shadow: var(--ui-ring);
}

.session-item__title {
  font-size: var(--ui-text-sm);
  font-weight: 600;
  color: var(--ui-fg-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.session-item__meta {
  font-size: var(--ui-text-xs);
  color: var(--ui-fg-muted);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* ---------- 窄窗口 ---------- */
@media (max-width: 1100px) {
  .home-body {
    grid-template-columns: 1fr;
  }

  .page-head {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
