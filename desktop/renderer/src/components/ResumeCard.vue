<template>
  <article class="resume-record">
    <router-link class="resume-record__main" :to="'/editor/' + resume.id" :aria-label="'编辑简历：' + (resume.title || '未命名简历')">
      <AppIcon name="file-text" :size="17" class="resume-record__icon" />
      <span class="resume-record__text">
        <span class="resume-record__title">{{ resume.title || '未命名简历' }}</span>
        <span class="resume-record__meta">
          <span>{{ templateLabel }}</span>
          <span aria-hidden="true">·</span>
          <span>{{ formatDate(resume.updatedAt) }}</span>
        </span>
      </span>
      <AppIcon name="caret-right" :size="15" class="resume-record__arrow" />
    </router-link>
    <button
      type="button"
      class="resume-record__delete"
      :aria-label="'删除简历：' + (resume.title || '未命名简历')"
      :disabled="deleting"
      @click="handleDelete"
    >
      <AppIcon :name="deleting ? 'spinner-gap' : 'trash'" :size="14" :spin="deleting" />
      <span>{{ deleting ? '删除中…' : '删除' }}</span>
    </button>
  </article>
</template>
<script setup>
import { computed, ref } from 'vue';
import AppIcon from './common/AppIcon.vue';
import { useResumeStore } from '../store/resumeStore.js';
import { RESUME_TEMPLATE_OPTIONS } from '../data/resumeTemplates.js';

const props = defineProps({
  resume: {
    type: Object,
    required: true,
  },
});
const emit = defineEmits(['deleted', 'delete-error']);

const resumeStore = useResumeStore();
const deleting = ref(false);

const TEMPLATE_LABELS = Object.fromEntries(RESUME_TEMPLATE_OPTIONS.map((template) => [template.id, template.name]));

const templateLabel = computed(
  () => TEMPLATE_LABELS[props.resume.template] || props.resume.template || RESUME_TEMPLATE_OPTIONS[0].name
);

function formatDate(dateStr) {
  if (!dateStr) return '尚未保存';
  const date = new Date(dateStr);
  if (Number.isNaN(date.getTime())) return '尚未保存';
  const now = new Date();
  const diffMs = now - date;
  const diffDays = Math.floor(diffMs / (1000 * 60 * 60 * 24));
  if (diffDays === 0) return '今天更新';
  if (diffDays === 1) return '昨天更新';
  if (diffDays < 30) return `${diffDays} 天前更新`;
  return date.toLocaleDateString('zh-CN');
}

async function handleDelete() {
  if (deleting.value || !window.confirm(`确定删除「${props.resume.title || '未命名简历'}」吗？该操作不可恢复。`)) {
    return;
  }
  deleting.value = true;
  try {
    await resumeStore.removeResume(props.resume.id);
    emit('deleted', props.resume.id);
  } catch (error) {
    emit('delete-error', error);
  } finally {
    deleting.value = false;
  }
}
</script>
<style scoped>
.resume-record {
  display: flex;
  min-width: 0;
  min-height: 47px;
  align-items: center;
  gap: 8px;
  border-bottom: 1px solid var(--ui-grid-line);
}

.resume-record__main {
  display: flex;
  min-width: 0;
  flex: 1;
  align-items: center;
  gap: 10px;
  padding: 5px 0;
  color: var(--ui-fg-primary);
}

.resume-record__main:focus-visible {
  outline: 2px solid var(--ui-accent);
  outline-offset: 2px;
}

.resume-record__icon {
  flex: 0 0 auto;
  color: var(--ui-fg-secondary);
}

.resume-record__text {
  display: flex;
  min-width: 0;
  flex: 1;
  flex-direction: column;
  gap: 2px;
}

.resume-record__title {
  overflow: hidden;
  font-size: 14px;
  font-weight: 600;
  line-height: 1.35;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.resume-record__meta {
  display: flex;
  min-width: 0;
  align-items: center;
  gap: 6px;
  overflow: hidden;
  color: var(--ui-fg-muted);
  font-size: 11px;
  line-height: 1.3;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.resume-record__arrow {
  flex: 0 0 auto;
  color: var(--ui-fg-muted);
}

.resume-record__delete {
  display: inline-flex;
  min-height: 30px;
  flex: 0 0 auto;
  align-items: center;
  gap: 5px;
  padding: 0 6px;
  border: 1px solid transparent;
  background: transparent;
  color: var(--ui-danger);
  font: inherit;
  font-size: 12px;
  cursor: pointer;
}

.resume-record__delete:hover {
  border-color: var(--ui-danger-border);
  background: var(--ui-danger-soft);
}

.resume-record__delete:focus-visible {
  outline: 2px solid var(--ui-danger);
  outline-offset: 2px;
}
</style>
