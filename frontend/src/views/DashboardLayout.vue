<template>
  <div class="dashboard">
    <!-- 左侧导航栏 -->
    <aside class="sidebar">
      <div class="sidebar-header">
        <svg width="32" height="32" viewBox="0 0 48 48" fill="none">
          <rect width="48" height="48" rx="13" fill="#6ee7b7"/>
          <path d="M14 16h20v3H17v5h15v3H17v7h-3V16z" fill="white"/>
        </svg>
        <span class="logo-text">Resume Forge</span>
      </div>

      <nav class="nav-list">
        <router-link to="/dashboard" class="nav-item" :class="{ active: isActive('/dashboard') }">
          <span class="nav-icon">📄</span>
          <span>简历列表</span>
        </router-link>
        <router-link to="/dashboard/profile" class="nav-item" :class="{ active: isActive('/dashboard/profile') }">
          <span class="nav-icon">👤</span>
          <span>个人中心</span>
        </router-link>
        <router-link to="/dashboard/history" class="nav-item" :class="{ active: isActive('/dashboard/history') }">
          <span class="nav-icon">🕐</span>
          <span>Agent 会话</span>
        </router-link>
        <router-link to="/dashboard/templates" class="nav-item" :class="{ active: isActive('/dashboard/templates') }">
          <span class="nav-icon">🎨</span>
          <span>模板管理</span>
        </router-link>
      </nav>
    </aside>

    <!-- 主内容区 -->
    <div class="main-wrapper">
      <!-- 顶部栏 -->
      <header class="topbar">
        <div class="user-info">
          <div class="top-avatar">
            <img v-if="authStore.user?.avatarUrl" :src="authStore.user.avatarUrl" alt="avatar" class="top-avatar-img" />
            <span v-else>{{ userInitial }}</span>
          </div>
          <div class="user-meta">
            <span class="user-name">{{ authStore.user?.username || '用户' }}</span>
            <span class="user-email">{{ authStore.user?.email || '未同步邮箱' }}</span>
          </div>
        </div>
        <button class="logout-btn" @click="handleLogout">退出</button>
      </header>

      <!-- 页面内容 -->
      <main class="content">
        <router-view />
      </main>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../store/authStore.js'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const userInitial = computed(() => {
  const name = authStore.user?.username || 'U'
  return name.charAt(0).toUpperCase()
})

const isActive = (path) => {
  return route.path === path || route.path.startsWith(path + '/')
}

const handleLogout = () => {
  authStore.logout()
  router.push('/login')
}
</script>

<style scoped>
.dashboard {
  display: flex;
  min-height: 100vh;
  background: #f5f5f5;
}

.sidebar {
  width: 240px;
  background: #1a1a2e;
  color: #fff;
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
}

.sidebar-header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 24px 20px;
  border-bottom: 1px solid rgba(255,255,255,0.1);
}

.logo-text {
  font-size: 18px;
  font-weight: 700;
  color: #fff;
}

.nav-list {
  padding: 16px 12px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  border-radius: 8px;
  color: rgba(255,255,255,0.7);
  text-decoration: none;
  font-size: 15px;
  transition: all 0.2s;
}

.nav-item:hover {
  background: rgba(255,255,255,0.1);
  color: #fff;
}

.nav-item.active {
  background: rgba(110, 231, 183, 0.2);
  color: #6ee7b7;
}

.nav-icon {
  font-size: 18px;
}

.main-wrapper {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.topbar {
  background: #fff;
  border-bottom: 1px solid #e5e7eb;
  padding: 16px 32px;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 16px;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.top-avatar {
  width: 38px;
  height: 38px;
  border-radius: 50%;
  background: linear-gradient(135deg, #22c55e, #14b8a6);
  color: #fff;
  font-size: 14px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

.top-avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.user-meta {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 2px;
}

.user-name {
  font-size: 14px;
  font-weight: 600;
  color: #1a1a2e;
}

.user-email {
  font-size: 12px;
  color: #6b7280;
}

.logout-btn {
  padding: 8px 16px;
  border: 1.5px solid #e5e7eb;
  background: transparent;
  border-radius: 8px;
  font-size: 14px;
  color: #6b7280;
  cursor: pointer;
  transition: all 0.2s;
}

.logout-btn:hover {
  border-color: #6ee7b7;
  color: #6ee7b7;
}

.content {
  padding: 32px;
  flex: 1;
  overflow-y: auto;
}
</style>
