<template>
  <div class="resume-card">
    <div class="card-body">
      <h3 class="card-title">{{ resume.title || 'Untitled Resume' }}</h3>
      <p class="card-template">模板：{{ resume.template || 'classic' }}</p>
      <p class="card-updated">
        {{ formatDate(resume.updatedAt) }}
      </p>
    </div>
    <div class="card-actions">
      <router-link :to="`/editor/${resume.id}`" class="btn-edit">编辑</router-link>
      <button class="btn-delete" @click="handleDelete">删除</button>
    </div>
  </div>
</template>

<script setup>
import { useResumeStore } from '../store/resumeStore.js';

const props = defineProps({
  resume: {
    type: Object,
    required: true,
  },
});

const resumeStore = useResumeStore();

function formatDate(dateStr) {
  if (!dateStr) return '尚未保存';
  const date = new Date(dateStr);
  const now = new Date();
  const diffMs = now - date;
  const diffDays = Math.floor(diffMs / (1000 * 60 * 60 * 24));
  if (diffDays === 0) return '今天更新';
  if (diffDays === 1) return '昨天更新';
  if (diffDays < 30) return `${diffDays} 天前更新`;
  return date.toLocaleDateString('zh-CN');
}

function handleDelete() {
  if (window.confirm(`确定删除「${props.resume.title || 'Untitled Resume'}」吗？该操作不可恢复。`)) {
    resumeStore.removeResume(props.resume.id);
  }
}
</script>

<style scoped>
.resume-card {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 14px;
  transition: box-shadow 0.15s, border-color 0.15s;
}

.resume-card:hover {
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
  border-color: #d1d5db;
}

.card-body {
  flex: 1;
}

.card-title {
  font-size: 16px;
  font-weight: 700;
  color: #1a1a2e;
  margin: 0 0 6px;
}

.card-updated {
  font-size: 13px;
  color: #9ca3af;
  margin: 0;
}

.card-template {
  font-size: 13px;
  color: #6b7280;
  margin: 0 0 6px;
}

.card-actions {
  display: flex;
  gap: 8px;
}

.btn-edit {
  flex: 1;
  padding: 8px 12px;
  background: #10d9a0;
  color: #fff;
  border: none;
  border-radius: 7px;
  font-size: 13px;
  font-weight: 600;
  text-align: center;
  text-decoration: none;
  cursor: pointer;
  transition: background 0.15s;
}

.btn-edit:hover {
  background: #0ea57a;
}

.btn-delete {
  padding: 8px 12px;
  background: transparent;
  color: #ef4444;
  border: 1.5px solid #fecaca;
  border-radius: 7px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.15s, border-color 0.15s;
}

.btn-delete:hover {
  background: #fef2f2;
  border-color: #ef4444;
}
</style>
