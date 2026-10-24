<template>
  <div class="home-page">
    <!-- 页头 -->
    <header class="page-head">
      <div class="page-head__text">
        <h1 class="ui-page-title">我的简历</h1>
        <p class="ui-page-subtitle">继续编辑已有简历，或者直接开始一次 AI 对话、模拟面试。</p>
      </div>
      <div class="page-head__actions">
        <button class="ui-btn ui-btn--primary" :disabled="creating" @click="handleCreate">
          <AppIcon name="plus" :size="16" />
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
        class="ui-panel ui-panel--hover quick-item"
        @click="handleQuickAction(item)"
      >
        <span class="ui-icon-tile quick-item__icon" aria-hidden="true">
          <AppIcon :name="item.icon" :size="18" />
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
        <div class="ui-section-head">
          <h2 class="ui-section-head__title">简历列表</h2>
          <span class="ui-badge">{{ resumeStore.resumes.length }} 份</span>
        </div>

        <!-- 加载中 -->
        <div v-if="resumeStore.loading && resumeStore.resumes.length === 0">
          <div class="resume-grid" aria-hidden="true">
            <div v-for="index in 3" :key="index" class="resume-skeleton ui-panel">
              <div class="resume-skeleton__head">
                <div class="ui-skeleton resume-skeleton__icon"></div>
                <div class="resume-skeleton__lines">
                  <div class="ui-skeleton resume-skeleton__title"></div>
                  <div class="ui-skeleton resume-skeleton__time"></div>
                </div>
              </div>
              <div class="ui-skeleton resume-skeleton__badge"></div>
              <div class="ui-skeleton resume-skeleton__foot"></div>
            </div>
          </div>
          <p class="state-text">正在加载简历…</p>
        </div>

        <!-- 加载失败 -->
        <div
          v-else-if="resumeStore.error && resumeStore.resumes.length === 0"
          class="ui-callout ui-callout--danger"
        >
          <AppIcon name="warning-circle" :size="16" />
          <div class="callout__body">
            <strong class="callout__title">简历列表加载失败</strong>
            <p class="callout__text">{{ resumeStore.error }}，请检查后端服务是否已启动后重试。</p>
          </div>
          <button class="ui-btn ui-btn--sm ui-btn--outline" @click="resumeStore.fetchResumes()">重新加载</button>
        </div>

        <!-- 空状态 -->
        <div v-else-if="resumeStore.resumes.length === 0" class="ui-panel state-panel">
          <div class="ui-empty">
            <span class="ui-empty__icon" aria-hidden="true">
              <AppIcon name="files" :size="24" />
            </span>
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
        <section class="ui-panel side-card">
          <div class="ui-section-head">
            <h2 class="ui-section-head__title">概览</h2>
          </div>

          <div class="stat-grid">
            <div class="ui-stat">
              <span class="ui-stat__label">简历</span>
              <span class="ui-stat__value">
                {{ resumeStore.resumes.length }}<small class="stat-unit">份</small>
              </span>
            </div>
            <div class="ui-stat">
              <span class="ui-stat__label">对话</span>
              <span class="ui-stat__value">
                {{ recentSessions.length }}<small class="stat-unit">条</small>
              </span>
            </div>
          </div>

          <p class="side-row">
            <span class="ui-stat__label">最近更新</span>
            <span class="side-row__value">{{ lastUpdatedText }}</span>
          </p>
        </section>

        <section class="ui-panel side-card">
          <div class="ui-section-head">
            <h2 class="ui-section-head__title">最近对话</h2>
            <router-link class="ui-btn ui-btn--ghost ui-btn--sm" to="/chat">全部</router-link>
          </div>

          <div v-if="sessionsLoading" aria-hidden="true">
            <ul class="skeleton-list">
              <li v-for="index in 3" :key="index" class="ui-skeleton skeleton-row"></li>
            </ul>
            <p class="state-text state-text--left">正在加载对话…</p>
          </div>

          <div v-else-if="recentSessions.length === 0" class="ui-empty side-empty">
            <span class="ui-empty__icon" aria-hidden="true">
              <AppIcon name="chat-circle" :size="24" />
            </span>
            <p class="ui-empty__title">还没有对话记录</p>
            <p class="ui-empty__hint">去 AI 对话说说你想投的岗位。</p>
            <router-link class="ui-btn ui-btn--outline ui-btn--sm" to="/chat">去和 AI 对话</router-link>
          </div>

          <ul v-else class="session-list">
            <li v-for="session in recentSessions" :key="session.id">
              <router-link class="session-item" to="/chat" title="在 AI 对话中查看">
                <span class="session-item__title">{{ session.title || '未命名对话' }}</span>
                <span class="session-item__meta">
                  <span>{{ formatDateTime(session.lastMessageAt) }}</span>
                  <span>{{ session.messageCount ?? 0 }} 条消息</span>
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
import AppIcon from '../components/common/AppIcon.vue';
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
  { key: 'create', icon: 'file-plus', title: '新建简历', desc: '从空白简历开始，边写边存' },
  { key: 'chat', icon: 'chat-circle', title: 'AI 对话', desc: '让 AI 帮你起草或改写简历', to: '/chat' },
  { key: 'templates', icon: 'layout', title: '模板市场', desc: '挑一套版式，套用后导出 PDF', to: '/dashboard/templates' },
  { key: 'interview', icon: 'microphone', title: '模拟面试', desc: '按目标岗位出题并逐轮点评', to: '/dashboard/interview' },
];

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
  font-family: inherit;
  color: var(--ui-fg-primary);
  text-align: left;
  cursor: pointer;
}

.quick-item:focus-visible {
  outline: none;
  box-shadow: var(--ui-ring);
}

.quick-item__icon {
  flex-shrink: 0;
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
  letter-spacing: var(--ui-tracking-snug);
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

.resume-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: var(--ui-space-4);
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

/* 骨架卡片：形状贴近最终简历卡片（图标 + 标题 + 时间 + 状态 + 操作区） */
.resume-skeleton {
  display: flex;
  flex-direction: column;
  gap: var(--ui-space-3);
  padding: var(--ui-space-4) var(--ui-space-5) var(--ui-space-3);
}

.resume-skeleton__head {
  display: flex;
  align-items: flex-start;
  gap: var(--ui-space-3);
}

.resume-skeleton__icon {
  width: 36px;
  height: 36px;
  flex-shrink: 0;
  border-radius: var(--ui-radius-md);
}

.resume-skeleton__lines {
  display: flex;
  flex-direction: column;
  gap: var(--ui-space-2);
  flex: 1;
  min-width: 0;
}

.resume-skeleton__title {
  height: 14px;
  width: 62%;
}

.resume-skeleton__time {
  height: 10px;
  width: 40%;
}

.resume-skeleton__badge {
  height: 18px;
  width: 72px;
  border-radius: var(--ui-radius-full);
}

.resume-skeleton__foot {
  height: 12px;
  width: 100%;
  margin-top: var(--ui-space-2);
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

.side-empty {
  padding: var(--ui-space-6) var(--ui-space-2);
}

.stat-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--ui-space-4);
}

.stat-unit {
  margin-left: 2px;
  font-size: var(--ui-text-xs);
  font-weight: 500;
  color: var(--ui-fg-muted);
}

.side-row {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: var(--ui-space-3);
  margin: 0;
  padding-top: var(--ui-space-3);
  border-top: 1px solid var(--ui-border-subtle);
}

.side-row__value {
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
  display: flex;
  align-items: center;
  gap: var(--ui-space-3);
  font-size: var(--ui-text-xs);
  color: var(--ui-fg-muted);
  overflow: hidden;
}

.session-item__meta span {
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
