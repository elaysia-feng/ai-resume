<template>
  <article class="resume-card ui-panel ui-panel--hover">
    <header class="resume-card__head">
      <span class="ui-icon-tile ui-icon-tile--muted" aria-hidden="true">
        <AppIcon name="file-text" :size="18" />
      </span>
      <div class="resume-card__head-text">
        <h3 class="resume-card__title">
          <!-- 标题链接铺满整张卡片：鼠标点任意位置都能进入编辑，Tab 只需一次 -->
          <router-link class="resume-card__link" :to="`/editor/${resume.id}`">
            {{ resume.title || '未命名简历' }}
          </router-link>
        </h3>
        <p class="resume-card__time">
          <AppIcon name="clock" :size="14" />
          <span>{{ formatDate(resume.updatedAt) }}</span>
        </p>
      </div>
    </header>

    <p class="resume-card__meta">
      <span class="ui-badge">{{ templateLabel }}</span>
    </p>

    <footer class="resume-card__foot">
      <span class="resume-card__hint">点击卡片进入编辑</span>
      <button type="button" class="ui-btn ui-btn--sm ui-btn--ghost resume-card__delete" @click="handleDelete">
        <AppIcon name="trash" :size="14" />
        删除
      </button>
    </footer>
  </article>
</template>

<script setup>
import { computed } from 'vue';
import AppIcon from './common/AppIcon.vue';
import { useResumeStore } from '../store/resumeStore.js';

const props = defineProps({
  resume: {
    type: Object,
    required: true,
  },
});

const resumeStore = useResumeStore();

// 模板 id → 中文名（与编辑器里的模板清单保持一致）
const TEMPLATE_LABELS = {
  classic: '标准版',
  modern: '简约求职版',
  compact: '双列紧凑版',
  academic: '学术科研版',
  creative: '创意进阶版',
  'dual-creative': '双列创意版',
};

const templateLabel = computed(
  () => TEMPLATE_LABELS[props.resume.template] || props.resume.template || '标准版'
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

function handleDelete() {
  if (window.confirm(`确定删除「${props.resume.title || '未命名简历'}」吗？该操作不可恢复。`)) {
    resumeStore.removeResume(props.resume.id);
  }
}
</script>

<style scoped>
.resume-card {
  display: flex;
  flex-direction: column;
  gap: var(--ui-space-3);
  padding: var(--ui-space-4) var(--ui-space-5) var(--ui-space-3);
}

/* 键盘聚焦在标题链接上时，整张卡片给出焦点提示 */
.resume-card:focus-within {
  border-color: var(--ui-border-focus);
  box-shadow: var(--ui-ring);
}

/* ---- 标题区：图标 + 标题 + 更新时间 ---- */
.resume-card__head {
  display: flex;
  align-items: flex-start;
  gap: var(--ui-space-3);
}

.resume-card__head-text {
  display: flex;
  flex-direction: column;
  gap: var(--ui-space-1);
  min-width: 0;
}

.resume-card__title {
  margin: 0;
  font-size: var(--ui-text-md);
  font-weight: 600;
  letter-spacing: var(--ui-tracking-snug);
  line-height: var(--ui-line-tight);
  color: var(--ui-fg-primary);
  word-break: break-word;
}

.resume-card__link {
  color: inherit;
  border-radius: var(--ui-radius-sm);
}

.resume-card__link:focus-visible {
  outline: none;
}

/* 链接铺满整张卡片，点击任意位置都能进入编辑 */
.resume-card__link::after {
  content: '';
  position: absolute;
  inset: 0;
  border-radius: var(--ui-radius-lg);
}

/* ---- 更新时间 ---- */
.resume-card__time {
  display: flex;
  align-items: center;
  gap: var(--ui-space-1);
  margin: 0;
  font-size: var(--ui-text-xs);
  color: var(--ui-fg-muted);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* ---- 状态标签 ---- */
.resume-card__meta {
  display: flex;
  align-items: center;
  gap: var(--ui-space-2);
  margin: 0;
  min-width: 0;
}

/* ---- 底部操作 ---- */
.resume-card__foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--ui-space-2);
  margin-top: auto;
  padding-top: var(--ui-space-3);
  border-top: 1px solid var(--ui-border-subtle);
}

.resume-card__hint {
  font-size: var(--ui-text-xs);
  color: var(--ui-fg-muted);
}

/* 删除按钮压在整卡链接之上，避免被链接的覆盖层挡住 */
.resume-card__delete {
  position: relative;
  z-index: 1;
  flex-shrink: 0;
  color: var(--ui-danger);
}

.resume-card__delete:hover {
  background: var(--ui-danger-soft);
  color: var(--ui-danger-hover);
}
</style>
