<template>
  <div class="app-shell">
    <!-- 访客页（欢迎页 / 登录 / 注册）：整页铺满，不显示侧边栏与顶栏 -->
    <div v-if="isGuest" class="app-guest">
      <router-view />
    </div>

    <template v-else>
      <!-- 左侧固定侧边栏 -->
      <aside class="app-sidebar" :class="{ 'app-sidebar--collapsed': collapsed }">
        <!-- 品牌标识 -->
        <div class="sidebar-brand">
          <span class="sidebar-brand__mark" aria-hidden="true">
            <svg viewBox="0 0 512 512" width="30" height="30" fill="none">
              <rect x="16" y="16" width="480" height="480" rx="112" fill="var(--ui-brand-900)" />
              <path d="M138 84h170l80 80v248a34 34 0 0 1-34 34H138a34 34 0 0 1-34-34V118a34 34 0 0 1 34-34z" fill="#ffffff" fill-opacity="0.94" />
              <path d="M308 84l80 80h-80V84z" fill="#a8a29e" fill-opacity="0.55" />
              <g fill="none" stroke="#16a34a" stroke-width="26" stroke-linecap="round" stroke-linejoin="round">
                <path d="M150 392l62-50 46 30" />
                <path d="M258 372l74-62" />
              </g>
              <path d="M334 288l-2 44-44 2 46-46z" fill="#22c55e" />
            </svg>
          </span>
          <span class="sidebar-brand__text">AI Resume Forge</span>
        </div>

        <!-- 分组导航 -->
        <nav class="sidebar-nav ui-scroll" aria-label="主导航">
          <div v-for="group in navGroups" :key="group.title" class="nav-group">
            <p class="nav-group__title">{{ group.title }}</p>
            <router-link
              v-for="item in group.items"
              :key="item.path"
              :to="item.path"
              class="nav-item"
              :class="{ 'is-active': isActive(item.path) }"
              :title="item.label"
            >
              <AppIcon :name="item.icon" :size="18" class="nav-item__icon" />
              <span class="nav-item__label">{{ item.label }}</span>
            </router-link>
          </div>
        </nav>

        <!-- 用户信息与操作 -->
        <div class="sidebar-footer">
          <div class="sidebar-user">
            <span class="sidebar-user__avatar">
              <img v-if="avatarUrl" :src="avatarUrl" alt="用户头像" />
              <span v-else>{{ userInitial }}</span>
            </span>
            <span class="sidebar-user__meta">
              <span class="sidebar-user__name">{{ displayName }}</span>
              <span class="sidebar-user__email">{{ displayEmail }}</span>
            </span>
          </div>

          <button type="button" class="ui-btn ui-btn--ghost ui-btn--block sidebar-action" @click="handleLogout">
            <AppIcon name="sign-out" :size="18" class="sidebar-action__icon" />
            <span class="nav-item__label">退出登录</span>
          </button>

          <button type="button" class="ui-btn ui-btn--ghost ui-btn--block sidebar-action" @click="toggleCollapsed">
            <AppIcon
              name="caret-double-left"
              :size="18"
              class="sidebar-action__icon sidebar-action__icon--flip"
              :class="{ 'is-flipped': collapsed }"
            />
            <span class="nav-item__label">{{ collapsed ? '展开侧边栏' : '收起侧边栏' }}</span>
          </button>
        </div>
      </aside>

      <!-- 顶栏 + 内容区 -->
      <div class="app-body">
        <header class="app-header">
          <h1 class="app-header__title">{{ pageTitle }}</h1>
          <div class="app-header__meta">
            <span class="header-user">
              <span class="header-user__avatar">
                <img v-if="avatarUrl" :src="avatarUrl" alt="用户头像" />
                <span v-else>{{ userInitial }}</span>
              </span>
              <span class="header-user__name">{{ displayName }}</span>
            </span>
          </div>
        </header>

        <main class="app-content ui-scroll">
          <div class="app-content__inner">
            <router-view />
          </div>
        </main>
      </div>
    </template>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useAuthStore } from './store/authStore.js';
import { isLocalIdentity } from './api/localProfile.js';
import AppIcon from './components/common/AppIcon.vue';

const COLLAPSE_STORAGE_KEY = 'resume_forge_sidebar_collapsed';

const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();

// 访客页（欢迎页 / 登录 / 注册）不显示应用壳
const isGuest = computed(() => !!route.meta.guest);

// 侧边栏收起状态（本地记忆）
const collapsed = ref(localStorage.getItem(COLLAPSE_STORAGE_KEY) === '1');

function toggleCollapsed() {
  collapsed.value = !collapsed.value;
  localStorage.setItem(COLLAPSE_STORAGE_KEY, collapsed.value ? '1' : '0');
}

// 导航分组：工作台 / 创作 / 成长 / 对话 / 设置
// icon 字段对应 src/icons/paths.js 的键名
const navGroups = [
  {
    title: '工作台',
    items: [{ path: '/dashboard', label: '我的简历', icon: 'squares-four' }],
  },
  {
    title: '创作',
    items: [
      { path: '/dashboard/templates', label: '模板市场', icon: 'layout' },
      { path: '/resume/create', label: '新建简历', icon: 'file-plus' },
    ],
  },
  {
    title: '成长',
    items: [
      { path: '/dashboard/profile', label: '个人档案', icon: 'user-circle' },
      { path: '/dashboard/interview', label: '模拟面试', icon: 'microphone' },
      { path: '/dashboard/history', label: 'Agent 会话', icon: 'clock-counter-clockwise' },
    ],
  },
  {
    title: '对话',
    items: [{ path: '/chat', label: 'AI 对话', icon: 'chat-circle' }],
  },
  {
    title: '设置',
    items: [{ path: '/settings', label: '设置', icon: 'gear' }],
  },
];

// 路由名 → 中文页面标题
const pageTitles = {
  Home: '我的简历',
  Templates: '模板市场',
  ResumeCreate: '新建简历',
  ResumeEditor: '简历编辑',
  Profile: '个人档案',
  Interview: '模拟面试',
  History: 'Agent 会话',
  Chat: 'AI 对话',
  Settings: '设置',
};

const pageTitle = computed(() => pageTitles[route.name] || 'AI Resume Forge');

function isActive(path) {
  return route.path === path;
}

const displayName = computed(() => authStore.currentUser?.username || '用户');
// 本地身份没有真实邮箱，侧边栏显示「本地身份」而不是占位邮箱
const displayEmail = computed(() => {
  if (isLocalIdentity(authStore.currentUser)) {
    return '本地身份';
  }
  return authStore.currentUser?.email || '未绑定邮箱';
});
const avatarUrl = computed(() => authStore.currentUser?.avatarUrl || '');
const userInitial = computed(() => displayName.value.charAt(0).toUpperCase());

function handleLogout() {
  authStore.logout();
  router.push('/login');
}
</script>

<style scoped>
/* ========================================
   应用壳骨架：左侧栏 + 顶栏 + 独立滚动内容区
   ======================================== */
.app-shell {
  display: flex;
  height: 100%;
  min-height: 0;
  background: var(--ui-bg-app);
  background-image: var(--ui-bg-app-wash);
  color: var(--ui-fg-primary);
  font-family: var(--ui-font-sans);
}

/* ---- 访客页整页铺满 ---- */
.app-guest {
  flex: 1;
  min-width: 0;
  height: 100%;
  overflow-y: auto;
}

/* ========================================
   侧边栏
   ======================================== */
.app-sidebar {
  width: var(--ui-sidebar-width);
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  min-height: 0;
  background: var(--ui-bg-surface);
  border-right: 1px solid var(--ui-border-hairline);
  transition: width var(--ui-duration-normal) var(--ui-ease-out);
}

.app-sidebar--collapsed {
  width: var(--ui-sidebar-width-collapsed);
}

/* ---- 品牌标识 ---- */
.sidebar-brand {
  display: flex;
  align-items: center;
  gap: var(--ui-space-3);
  height: var(--ui-header-height);
  flex-shrink: 0;
  padding: 0 var(--ui-space-4);
  border-bottom: 1px solid var(--ui-border-hairline);
}

.sidebar-brand__mark {
  display: flex;
  flex-shrink: 0;
  filter: drop-shadow(0 2px 6px rgba(22, 163, 74, 0.28));
}

.sidebar-brand__text {
  font-family: var(--ui-font-display);
  font-size: var(--ui-text-base);
  font-weight: 650;
  letter-spacing: var(--ui-tracking-snug);
  color: var(--ui-fg-primary);
  white-space: nowrap;
  overflow: hidden;
}

/* ---- 分组导航 ---- */
.sidebar-nav {
  flex: 1;
  min-height: 0;
  padding: var(--ui-space-4) var(--ui-space-3);
}

.nav-group + .nav-group {
  margin-top: var(--ui-space-5);
}

.nav-group__title {
  margin: 0 0 var(--ui-space-2);
  padding: 0 var(--ui-space-3);
  font-size: var(--ui-text-xs);
  font-weight: 600;
  color: var(--ui-fg-muted);
  white-space: nowrap;
}

.nav-item {
  position: relative;
  display: flex;
  align-items: center;
  gap: var(--ui-space-3);
  padding: var(--ui-space-2) var(--ui-space-3);
  border-radius: var(--ui-radius-md);
  color: var(--ui-fg-secondary);
  font-size: var(--ui-text-base);
  font-weight: 500;
  line-height: 1.5rem;
  text-decoration: none;
  transition: background var(--ui-duration-fast) var(--ui-ease-out),
    color var(--ui-duration-fast) var(--ui-ease-out);
}

.nav-item + .nav-item {
  margin-top: var(--ui-space-1);
}

.nav-item:hover {
  background: var(--ui-bg-hover);
  color: var(--ui-fg-primary);
}

.nav-item:focus-visible {
  outline: none;
  box-shadow: var(--ui-ring);
}

.nav-item.is-active {
  background: var(--ui-bg-selected);
  color: var(--ui-fg-accent);
  font-weight: 600;
}

/* 选中项左侧一道品牌色竖条：替代整块底色的第二种强调手段 */
.nav-item.is-active::before {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 3px;
  height: 16px;
  border-radius: var(--ui-radius-full);
  background: var(--ui-accent);
}

.nav-item__icon {
  width: 18px;
  height: 18px;
  flex-shrink: 0;
}

.nav-item__label {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* ---- 侧边栏底部：用户信息与操作 ---- */
.sidebar-footer {
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  gap: var(--ui-space-1);
  padding: var(--ui-space-3);
  border-top: 1px solid var(--ui-border);
}

.sidebar-user {
  display: flex;
  align-items: center;
  gap: var(--ui-space-3);
  padding: var(--ui-space-2);
  min-width: 0;
}

.sidebar-user__avatar,
.header-user__avatar {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  width: 32px;
  height: 32px;
  border-radius: var(--ui-radius-full);
  background: var(--ui-accent-soft);
  border: 1px solid var(--ui-accent-border);
  color: var(--ui-fg-accent);
  font-size: var(--ui-text-sm);
  font-weight: 600;
  overflow: hidden;
}

.sidebar-user__avatar img,
.header-user__avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.sidebar-user__meta {
  display: flex;
  flex-direction: column;
  min-width: 0;
  line-height: 1.25rem;
}

.sidebar-user__name {
  font-size: var(--ui-text-base);
  font-weight: 600;
  color: var(--ui-fg-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.sidebar-user__email {
  font-size: var(--ui-text-xs);
  color: var(--ui-fg-muted);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.sidebar-action {
  justify-content: flex-start;
  gap: var(--ui-space-3);
  color: var(--ui-fg-secondary);
}

.sidebar-action__icon {
  width: 18px;
  height: 18px;
  flex-shrink: 0;
  transition: transform var(--ui-duration-normal) var(--ui-ease-in-out);
}

.sidebar-action__icon--flip.is-flipped {
  transform: rotate(180deg);
}

/* ---- 收起态：只留图标 ---- */
.app-sidebar--collapsed .sidebar-brand {
  justify-content: center;
  padding: 0;
}

.app-sidebar--collapsed .sidebar-brand__text,
.app-sidebar--collapsed .nav-item__label,
.app-sidebar--collapsed .nav-group__title,
.app-sidebar--collapsed .sidebar-user__meta {
  display: none;
}

.app-sidebar--collapsed .nav-item,
.app-sidebar--collapsed .sidebar-action {
  justify-content: center;
  padding-left: 0;
  padding-right: 0;
}

.app-sidebar--collapsed .nav-group + .nav-group {
  margin-top: var(--ui-space-3);
  padding-top: var(--ui-space-3);
  border-top: 1px solid var(--ui-border-subtle);
}

.app-sidebar--collapsed .sidebar-user {
  justify-content: center;
}

/* ========================================
   顶栏 + 内容区
   ======================================== */
.app-body {
  flex: 1;
  min-width: 0;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

.app-header {
  height: var(--ui-header-height);
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--ui-space-4);
  padding: 0 var(--ui-space-6);
  background: var(--ui-bg-surface);
  background-image: var(--ui-surface-wash);
  border-bottom: 1px solid var(--ui-border-hairline);
}

.app-header__title {
  margin: 0;
  font-family: var(--ui-font-display);
  font-size: var(--ui-text-md);
  font-weight: 620;
  letter-spacing: var(--ui-tracking-snug);
  color: var(--ui-fg-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.app-header__meta {
  display: flex;
  align-items: center;
  gap: var(--ui-space-3);
  min-width: 0;
}

.header-user {
  display: flex;
  align-items: center;
  gap: var(--ui-space-2);
  padding: var(--ui-space-1) var(--ui-space-3) var(--ui-space-1) var(--ui-space-1);
  border: 1px solid var(--ui-border-hairline);
  border-radius: var(--ui-radius-full);
  background: var(--ui-bg-surface);
  box-shadow: var(--ui-shadow-xs);
  min-width: 0;
}

.header-user__name {
  font-size: var(--ui-text-sm);
  color: var(--ui-fg-secondary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.app-content {
  flex: 1;
  min-height: 0;
  padding: var(--ui-space-8);
}

.app-content__inner {
  width: 100%;
  max-width: var(--ui-content-max);
  margin: 0 auto;
}

/* ---- 窄窗口：压缩间距，避免横向滚动 ---- */
@media (max-width: 1100px) {
  .app-content {
    padding: var(--ui-space-6);
  }

  .app-header {
    padding: 0 var(--ui-space-4);
  }
}
</style>
