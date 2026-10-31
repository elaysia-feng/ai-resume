<template>
  <section class="template-market">
    <header class="ui-section-head template-market__head">
      <div>
        <h2 class="ui-section-head__title">选择模板</h2>
        <p class="ui-section-head__hint">点击卡片即可切换简历模板，预览会实时更新。</p>
      </div>
    </header>

    <div v-if="loading" class="templates-grid" aria-busy="true">
      <div v-for="index in RESUME_TEMPLATE_OPTIONS.length" :key="index" class="template-card ui-panel template-card--skeleton">
        <div class="ui-skeleton template-skeleton__preview"></div>
        <div class="ui-skeleton template-skeleton__line"></div>
        <div class="ui-skeleton template-skeleton__line template-skeleton__line--short"></div>
      </div>
    </div>

    <div v-else class="templates-grid">
      <div
        v-for="tpl in templates"
        :key="tpl.id"
        role="button"
        tabindex="0"
        class="template-card ui-panel ui-panel--hover"
        :class="{ 'is-selected': modelValue === tpl.id }"
        :aria-pressed="modelValue === tpl.id"
        :aria-label="`${tpl.name}，${tpl.tagline}`"
        @click="emit('update:modelValue', tpl.id)"
        @keydown.enter.prevent="emit('update:modelValue', tpl.id)"
        @keydown.space.prevent="emit('update:modelValue', tpl.id)"
      >
        <div class="template-card__preview">
          <TemplatePreview :template-id="tpl.id" />
          <span v-if="modelValue === tpl.id" class="ui-badge ui-badge--accent template-card__badge">
            <AppIcon name="check" :size="12" />
            使用中
          </span>
        </div>

        <span class="template-card__info">
          <span class="template-card__name">{{ tpl.name }}</span>
          <span class="template-card__desc">{{ tpl.desc }}</span>
        </span>
      </div>
    </div>
  </section>
</template>

<script setup>
import AppIcon from '../common/AppIcon.vue'
import TemplatePreview from './TemplatePreview.vue'
import { RESUME_TEMPLATE_OPTIONS } from '../../data/resumeTemplates.js'

defineProps({
  modelValue: {
    type: String,
    default: null,
  },
  // 可选：加载中显示骨架卡片，而不是整页遮罩
  loading: {
    type: Boolean,
    default: false,
  },
})

const emit = defineEmits(['update:modelValue'])

const templates = RESUME_TEMPLATE_OPTIONS
</script>

<style scoped>
.template-market {
  display: flex;
  flex-direction: column;
  gap: var(--ui-space-4);
  padding: var(--ui-space-5);
}

.template-market__head {
  /* 间距交给 flex gap，避免和 .ui-section-head 默认外边距叠加 */
  margin-bottom: 0;
  align-items: flex-start;
}

.templates-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  align-items: stretch;
  gap: var(--ui-space-4);
}

.template-card {
  display: flex;
  flex-direction: column;
  gap: var(--ui-space-3);
  min-width: 0;
  padding: var(--ui-space-3);
  border: 1px solid var(--ui-border);
  border-radius: var(--ui-radius-lg);
  font: inherit;
  text-align: left;
  cursor: pointer;
}

.template-card:focus-visible {
  outline: none;
  box-shadow: var(--ui-ring);
}

.template-card.is-selected {
  border-color: var(--ui-accent);
  box-shadow: var(--ui-shadow-sm);
}

.template-card.is-selected .template-card__name {
  color: var(--ui-fg-accent);
}

.template-card__preview {
  position: relative;
  display: block;
  width: 100%;
  /* A4 比例固定，缩略图不参与卡片高度计算 */
  aspect-ratio: 210 / 297;
  flex-shrink: 0;
  padding: var(--ui-space-3);
  border-radius: var(--ui-radius-md);
  border: 1px solid var(--ui-border-subtle);
}

.template-card__badge {
  position: absolute;
  top: var(--ui-space-3);
  right: var(--ui-space-3);
}

.template-card__info {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.template-card__name {
  font-size: var(--ui-text-sm);
  font-weight: 600;
  color: var(--ui-fg-primary);
}

.template-card__desc {
  font-size: var(--ui-text-xs);
  line-height: 1.5;
  color: var(--ui-fg-secondary);
}

/* ---------- 骨架卡片 ---------- */
.template-card--skeleton {
  cursor: default;
  pointer-events: none;
}

.template-skeleton__preview {
  width: 100%;
  aspect-ratio: 210 / 297;
  border-radius: var(--ui-radius-md);
}

.template-skeleton__line {
  height: 12px;
  border-radius: var(--ui-radius-sm);
}

.template-skeleton__line--short {
  width: 60%;
}
</style>
