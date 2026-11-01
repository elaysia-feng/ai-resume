<template>
  <div class="app-shell">
    <aside class="app-sidebar" :class="{ 'app-sidebar--collapsed': collapsed }">
        <div class="sidebar-brand">
          <AppBrandMark :size="collapsed ? 24 : 32" />
          <span class="sidebar-brand__text">
            <span class="sidebar-brand__name">AI Resume Forge</span>
            <span class="sidebar-brand__tagline">你的求职准备工作台</span>
          </span>
          <button
            type="button"
            class="sidebar-collapse"
            :aria-label="collapsed ? '展开侧边栏' : '收起侧边栏'"
            :title="collapsed ? '展开侧边栏' : '收起侧边栏'"
            @click="toggleCollapsed"
          >
            <AppIcon
              name="caret-double-left"
              :size="17"
              :class="{ 'is-flipped': collapsed }"
            />
          </button>
        </div>

        <nav class="sidebar-nav ui-scroll" aria-label="主导航">
          <div v-for="group in navGroups" :key="group.title || group.items[0].path" class="nav-group">
            <p v-if="group.title" class="nav-group__title">{{ group.title }}</p>
            <router-link
              v-for="item in group.items"
              :key="item.label + ':' + (item.section || '')"
              :to="item.to || item.path"
              class="nav-item"
              :class="{ 'is-active': isActive(item) }"
              :aria-current="isActive(item) ? 'page' : undefined"
              :title="item.label"
            >
              <AppIcon :name="item.icon" :size="19" class="nav-item__icon" />
              <span class="nav-item__label">{{ item.label }}</span>
            </router-link>
          </div>
        </nav>

        <div class="sidebar-footer">
          <div class="sidebar-footer__row">
            <div class="sidebar-user" title="数据保存在本机">
              <AppIcon name="desktop" :size="18" class="sidebar-user__icon" />
              <span class="sidebar-user__meta">
                <span class="sidebar-user__name">本地工作区</span>
                <span class="sidebar-user__email">数据保存在本机</span>
              </span>
            </div>
          </div>
        </div>
      </aside>

      <div class="app-body">
        <main class="app-content ui-scroll">
          <div class="app-content__inner" :class="{ 'app-content__inner--flush': route.name === 'Home' }">
            <router-view v-slot="{ Component, route: viewRoute }">
              <component :is="Component" :key="viewRoute.name + ':' + (viewRoute.params.id || '')" />
            </router-view>
          </div>
        </main>
      </div>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import { useRoute } from 'vue-router';
import AppIcon from './components/common/AppIcon.vue';
import AppBrandMark from './components/common/AppBrandMark.vue';

const COLLAPSE_STORAGE_KEY = 'resume_forge_sidebar_collapsed';

const route = useRoute();

// 侧边栏收起状态（本地记忆）
const collapsed = ref(localStorage.getItem(COLLAPSE_STORAGE_KEY) === '1');

function toggleCollapsed() {
  collapsed.value = !collapsed.value;
  localStorage.setItem(COLLAPSE_STORAGE_KEY, collapsed.value ? '1' : '0');
}

// 保持多入口工作台，简历、模板、对话和面试均可独立进入。
// icon 字段对应 src/icons/paths.js 的键名
const navGroups = [
  {
    title: '',
    items: [
      { path: '/dashboard', label: '工作台', icon: 'squares-four' },
      { path: '/dashboard', to: { path: '/dashboard', query: { section: 'resumes' } }, section: 'resumes', label: '简历', icon: 'file-text' },
      { path: '/dashboard/templates', label: '简历模板', icon: 'layout' },
      { path: '/chat', label: '工作区对话', icon: 'chat-circle' },
      { path: '/dashboard/interview', label: '模拟面试', icon: 'microphone' },
      { path: '/settings', label: '设置', icon: 'gear' },
    ],
  },
];

function isActive(item) {
  if (item.section === 'resumes') {
    return route.path === '/dashboard' && route.query.section === 'resumes';
  }
  if (item.path === '/dashboard') {
    return route.path === item.path && route.query.section !== 'resumes';
  }
  return route.path === item.path || route.path.startsWith(item.path + '/');
}

</script>
<style scoped>
.app-shell {
  display: flex;
  width: 100%;
  height: 100%;
  min-height: 0;
  overflow: hidden;
  background: var(--ui-bg-app);
  color: var(--ui-fg-primary);
  font-family: var(--ui-font-sans);
}

.app-sidebar {
  display: flex;
  width: var(--ui-sidebar-width);
  min-width: var(--ui-sidebar-width);
  height: 100%;
  flex-direction: column;
  background: var(--ui-bg-app);
  border-right: 1px solid var(--ui-grid-line-strong);
}

.app-sidebar--collapsed {
  width: var(--ui-sidebar-width-collapsed);
  min-width: var(--ui-sidebar-width-collapsed);
}

.sidebar-brand {
  position: relative;
  display: flex;
  min-height: 140px;
  align-items: center;
  gap: 8px;
  padding: 0 14px;
  border-bottom: 1px solid var(--ui-grid-line);
}

.sidebar-brand__text {
  display: flex;
  min-width: 0;
  flex: 1;
  flex-direction: column;
  padding-top: 1px;
}

.sidebar-collapse {
  display: grid;
  width: 22px;
  height: 30px;
  flex: 0 0 auto;
  place-items: center;
  border: 1px solid transparent;
  background: transparent;
  color: var(--ui-fg-muted);
  cursor: pointer;
}

.sidebar-collapse:hover {
  border-color: var(--ui-grid-line);
  color: var(--ui-fg-primary);
}

.sidebar-collapse:focus-visible {
  outline: 2px solid var(--ui-accent);
  outline-offset: 1px;
}

.sidebar-collapse .app-icon {
  transition: transform var(--ui-duration-normal) var(--ui-ease-out);
}

.sidebar-collapse .is-flipped {
  transform: rotate(180deg);
}

.sidebar-brand__name {
  overflow: hidden;
  color: var(--ui-fg-primary);
  font-family: var(--ui-font-display);
  font-size: 17px;
  font-weight: 650;
  letter-spacing: -0.02em;
  line-height: 1.3;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.sidebar-brand__tagline {
  margin-top: 4px;
  color: var(--ui-fg-muted);
  font-size: 12px;
  line-height: 1.35;
  white-space: nowrap;
}

.sidebar-nav {
  flex: 1;
  min-height: 0;
  padding: 0 2px 12px;
  overflow-y: auto;
}

.nav-group + .nav-group {
  padding-top: 12px;
  border-top: 1px solid var(--ui-grid-line);
}

.nav-group__title {
  margin: 10px 0 6px;
  padding: 0 22px;
  color: var(--ui-fg-muted);
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.04em;
}

.nav-item {
  position: relative;
  display: flex;
  min-height: 66px;
  align-items: center;
  gap: 28px;
  padding: 0 24px;
  border-bottom: 1px solid var(--ui-grid-line);
  color: var(--ui-fg-primary);
  font-size: 16px;
  font-weight: 500;
  line-height: 1.35;
  text-decoration: none;
  transition: background var(--ui-duration-fast) var(--ui-ease-out),
    color var(--ui-duration-fast) var(--ui-ease-out);
}

.nav-item:first-of-type {
  min-height: 58px;
}

.nav-item:hover {
  background: var(--ui-bg-hover);
}

.nav-item:focus-visible {
  outline: 2px solid var(--ui-accent);
  outline-offset: -3px;
}

.nav-item.is-active {
  background: var(--ui-accent);
  color: var(--ui-accent-fg);
  font-weight: 600;
}

.nav-item__icon {
  width: 19px;
  height: 19px;
  flex: 0 0 auto;
}

.nav-item__label {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.sidebar-footer {
  flex: 0 0 auto;
  padding: 8px 10px;
  border-top: 1px solid var(--ui-grid-line-strong);
}

.sidebar-footer__row {
  display: flex;
  min-width: 0;
  min-height: 44px;
  align-items: center;
  gap: 5px;
}

.sidebar-user {
  display: flex;
  min-width: 0;
  flex: 1;
  align-items: center;
  gap: 8px;
  padding: 4px 6px;
  color: var(--ui-fg-primary);
  text-decoration: none;
}

.sidebar-user__icon {
  flex: 0 0 auto;
  color: var(--ui-fg-secondary);
}

.sidebar-user__meta {
  display: flex;
  min-width: 0;
  flex-direction: column;
  line-height: 1.3;
}

.sidebar-user__name,
.sidebar-user__email {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.sidebar-user__name {
  color: var(--ui-fg-primary);
  font-size: 13px;
  font-weight: 600;
}

.sidebar-user__email {
  margin-top: 2px;
  color: var(--ui-fg-muted);
  font-size: 11px;
}

.app-sidebar--collapsed .sidebar-brand {
  justify-content: center;
  gap: 0;
  padding-right: 0;
  padding-left: 0;
}

.app-sidebar--collapsed .sidebar-brand__text,
.app-sidebar--collapsed .nav-group__title,
.app-sidebar--collapsed .nav-item__label,
.app-sidebar--collapsed .sidebar-user__meta {
  display: none;
}

.app-sidebar--collapsed .nav-item {
  justify-content: center;
  padding-right: 0;
  padding-left: 0;
}

.app-sidebar--collapsed .sidebar-user {
  justify-content: center;
}

.app-body {
  display: flex;
  min-width: 0;
  min-height: 0;
  flex: 1;
  flex-direction: column;
}

.app-content {
  min-height: 0;
  flex: 1;
  padding: 0;
  overflow: auto;
}

.app-content__inner {
  width: 100%;
  min-height: 100%;
  margin: 0;
  padding: var(--ui-space-8);
}

.app-content__inner--flush {
  padding: 0;
}

@media (max-width: 1100px) {
  .app-content__inner {
    padding: var(--ui-space-6);
  }

  .app-content__inner--flush {
    padding: 0;
  }
}

@media (max-width: 900px) {
  .app-sidebar,
  .app-sidebar--collapsed {
    width: var(--ui-sidebar-width-collapsed);
    min-width: var(--ui-sidebar-width-collapsed);
  }

  .sidebar-brand {
    justify-content: center;
    gap: 0;
    padding-right: 0;
    padding-left: 0;
  }

  .sidebar-brand__text,
  .nav-group__title,
  .nav-item__label,
  .sidebar-user__meta,
  .sidebar-action__label {
    display: none;
  }

  .nav-item,
  .sidebar-action {
    justify-content: center;
    padding-right: 0;
    padding-left: 0;
  }

  .sidebar-user {
    justify-content: center;
  }
}
</style>
