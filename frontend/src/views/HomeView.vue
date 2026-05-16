<template>
  <div class="dashboard-page">
    <header class="dashboard-header">
      <div class="header-left">
        <h1 class="page-title">我的简历</h1>
        <button class="logout-btn" @click="handleLogout">退出</button>
      </div>
      <button class="create-btn" @click="handleCreate">
        + 新建简历
      </button>
    </header>

    <div class="dashboard-body">
      <section class="status-card">
        <div class="status-left">
          <div>
            <h2 class="status-title">后端状态</h2>
            <p class="status-subtitle">当前展示的是 Java 后端健康检查接口</p>
          </div>
          <span class="status-pill" :class="healthStatusClass">{{ healthText }}</span>
        </div>
        <button class="retry-btn plain" @click="loadHealthStatus">刷新状态</button>
      </section>

      <!-- Loading -->
      <div v-if="resumeStore.loading && resumeStore.resumes.length === 0" class="state-message">
        正在加载简历列表...
      </div>

      <!-- Error -->
      <div v-else-if="resumeStore.error && resumeStore.resumes.length === 0" class="state-message error">
        <p>{{ resumeStore.error }}</p>
        <button class="retry-btn" @click="resumeStore.fetchResumes()">重新加载</button>
      </div>

      <!-- Empty state -->
      <div v-else-if="resumeStore.resumes.length === 0" class="empty-state">
        <p class="empty-title">{{ authStore.user?.username ? authStore.user.username + '，' : '' }}您还没有简历</p>
        <p class="empty-sub">开始制作属于您的简历吧 <span class="kbd">N</span></p>
        <button class="create-btn" @click="handleCreate">+ 新建简历</button>
      </div>

      <!-- Resume grid -->
      <div v-else class="resume-grid">
        <ResumeCard
          v-for="resume in resumeStore.resumes"
          :key="resume.id"
          :resume="resume"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { useResumeStore } from '../store/resumeStore.js';
import { useAuthStore } from '../store/authStore.js';
import { extractApiMessage, getJavaHealth } from '../api/index.js';
import ResumeCard from '../components/ResumeCard.vue';

const resumeStore = useResumeStore();
const authStore = useAuthStore();
const router = useRouter();
const healthStatus = ref('checking');

const healthText = computed(() => {
  if (healthStatus.value === 'ok') return '服务正常';
  if (healthStatus.value === 'error') return '服务异常';
  return '检查中';
});

const healthStatusClass = computed(() => ({
  ok: healthStatus.value === 'ok',
  error: healthStatus.value === 'error',
  checking: healthStatus.value === 'checking',
}));

onMounted(() => {
  resumeStore.fetchResumes();
  loadHealthStatus();
  window.addEventListener('keydown', handleKeydown);
});

onUnmounted(() => {
  window.removeEventListener('keydown', handleKeydown);
});

function handleKeydown(e) {
  if (e.key === 'n' || e.key === 'N') {
    handleCreate();
  }
}

async function handleCreate() {
  try {
    const created = await resumeStore.createResume({ title: 'Untitled Resume' });
    const id = created?.id || created?._id;
    if (id) {
      router.push(`/editor/${id}`);
    }
  } catch {
    // error is set on the store
  }
}

async function loadHealthStatus() {
  healthStatus.value = 'checking';
  try {
    const data = await getJavaHealth();
    healthStatus.value = data?.status === 'ok' ? 'ok' : 'error';
  } catch (err) {
    console.warn(extractApiMessage(err, '后端健康检查失败'));
    healthStatus.value = 'error';
  }
}

function handleLogout() {
  authStore.logout();
  router.push('/login');
}
</script>

<style scoped>
.dashboard-page {
  min-height: 100vh;
  background: #f5f5f5;
  font-family: 'PingFang SC', 'Microsoft YaHei', sans-serif;
}

.dashboard-header {
  background: #fff;
  border-bottom: 1px solid #e5e7eb;
  padding: 20px 32px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 20px;
}

.page-title {
  font-size: 22px;
  font-weight: 700;
  color: #1a1a2e;
  margin: 0;
}

.logout-btn {
  padding: 6px 14px;
  border: 1.5px solid #e5e7eb;
  background: transparent;
  border-radius: 7px;
  font-size: 13px;
  color: #6b7280;
  cursor: pointer;
  transition: border-color 0.15s, color 0.15s;
}

.logout-btn:hover {
  border-color: #6ee7b7;
  color: #6ee7b7;
}

.create-btn {
  padding: 9px 20px;
  background: #6ee7b7;
  color: #fff;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.15s;
}

.create-btn:hover {
  background: #34d399;
}

.dashboard-body {
  padding: 32px;
  max-width: 1200px;
  margin: 0 auto;
}

.status-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 18px 20px;
  margin-bottom: 24px;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
}

.status-left {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex: 1;
}

.status-title {
  margin: 0;
  font-size: 16px;
  font-weight: 700;
  color: #1a1a2e;
}

.status-subtitle {
  margin: 6px 0 0;
  font-size: 13px;
  color: #6b7280;
}

.status-pill {
  border-radius: 999px;
  padding: 8px 12px;
  font-size: 12px;
  font-weight: 700;
}

.status-pill.ok {
  background: #dcfce7;
  color: #166534;
}

.status-pill.error {
  background: #fee2e2;
  color: #991b1b;
}

.status-pill.checking {
  background: #fef3c7;
  color: #92400e;
}

.state-message {
  text-align: center;
  color: #6b7280;
  font-size: 15px;
  padding: 60px 0;
}

.state-message.error {
  color: #ef4444;
}

.retry-btn {
  margin-top: 12px;
  padding: 8px 20px;
  border: 1.5px solid #ef4444;
  background: transparent;
  color: #ef4444;
  border-radius: 7px;
  font-size: 14px;
  cursor: pointer;
}

.retry-btn.plain {
  margin-top: 0;
  border-color: #d1d5db;
  color: #374151;
}

.empty-state {
  text-align: center;
  padding: 80px 0;
}

.empty-title {
  font-size: 20px;
  font-weight: 700;
  color: #1a1a2e;
  margin: 0 0 8px;
}

.empty-sub {
  font-size: 14px;
  color: #9ca3af;
  margin: 0 0 24px;
}

.kbd {
  display: inline-block;
  padding: 2px 8px;
  background: #fff;
  border: 1.5px solid #6ee7b7;
  border-radius: 5px;
  font-size: 12px;
  font-family: monospace;
  color: #6ee7b7;
  line-height: 1.5;
}

.resume-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 20px;
}

@media (max-width: 768px) {
  .status-card,
  .status-left {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
