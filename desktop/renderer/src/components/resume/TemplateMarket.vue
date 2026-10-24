<template>
  <section class="template-market">
    <header class="template-market__head">
      <div>
        <h2 class="section-title">选择模板</h2>
        <p class="template-market__hint">点击卡片即可切换简历模板，预览会实时更新。</p>
      </div>
    </header>

    <div v-if="loading" class="templates-grid" aria-busy="true">
      <div v-for="index in 6" :key="index" class="template-card template-card--skeleton">
        <div class="template-skeleton__preview"></div>
        <div class="template-skeleton__line"></div>
        <div class="template-skeleton__line template-skeleton__line--short"></div>
      </div>
    </div>

    <div v-else class="templates-grid">
      <button
        v-for="tpl in templates"
        :key="tpl.id"
        type="button"
        class="template-card"
        :class="{ 'is-selected': modelValue === tpl.id }"
        :aria-pressed="modelValue === tpl.id"
        @click="emit('update:modelValue', tpl.id)"
      >
        <span class="template-card__preview" :style="{ background: tpl.bg }">
          <span class="template-card__sheet">
            <span class="template-card__header" :style="{ background: tpl.header }"></span>
            <span class="template-card__body" :class="tpl.layout">
              <span v-if="tpl.sidebar" class="template-card__sidebar"></span>
              <span class="template-card__lines">
                <span class="template-card__line is-title" :style="{ width: tpl.line1 }"></span>
                <span class="template-card__line is-subtitle" :style="{ width: tpl.line2 }"></span>
                <span class="template-card__line" :style="{ width: tpl.line3 }"></span>
                <span class="template-card__line" :style="{ width: tpl.line4 }"></span>
              </span>
            </span>
          </span>
          <span v-if="modelValue === tpl.id" class="template-card__badge">使用中</span>
        </span>

        <span class="template-card__info">
          <span class="template-card__name">{{ tpl.name }}</span>
          <span class="template-card__desc">{{ tpl.desc }}</span>
        </span>
      </button>
    </div>
  </section>
</template>

<script setup>
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

const templates = [
  {
    id: 'classic',
    name: '标准版',
    desc: '结构清晰，适合程序员、产品经理、设计师等职位',
    bg: 'linear-gradient(180deg, var(--ui-neutral-0) 0%, var(--ui-neutral-50) 100%)',
    header: 'var(--ui-info)',
    layout: 'layout-classic',
    sidebar: false,
    line1: '80%',
    line2: '55%',
    line3: '75%',
    line4: '45%',
  },
  {
    id: 'modern',
    name: '简约求职版',
    desc: '信息清晰紧凑，适合通用岗位、校招及社招',
    bg: 'linear-gradient(180deg, var(--ui-brand-50) 0%, var(--ui-accent-soft) 100%)',
    header: 'var(--ui-brand-700)',
    layout: 'layout-modern',
    sidebar: true,
    line1: '65%',
    line2: '70%',
    line3: '60%',
    line4: '50%',
  },
  {
    id: 'compact',
    name: '双列紧凑版',
    desc: '信息密度高，适合应届毕业生、实习生、校招类岗位',
    bg: 'linear-gradient(180deg, var(--ui-neutral-100) 0%, var(--ui-neutral-200) 100%)',
    header: 'var(--ui-neutral-800)',
    layout: 'layout-modern',
    sidebar: true,
    line1: '60%',
    line2: '65%',
    line3: '55%',
    line4: '48%',
  },
  {
    id: 'academic',
    name: '学术科研版',
    desc: '极简风格、信息容量大，适合科研、教育、技术研发岗位',
    bg: 'linear-gradient(180deg, var(--ui-neutral-50) 0%, var(--ui-neutral-100) 100%)',
    header: 'var(--ui-neutral-700)',
    layout: 'layout-classic',
    sidebar: false,
    line1: '75%',
    line2: '50%',
    line3: '70%',
    line4: '42%',
  },
  {
    id: 'creative',
    name: '创意进阶版',
    desc: '不规则设计、视觉分区强烈，适合 UI 设计师、平面设计、品牌营销',
    bg: 'linear-gradient(180deg, var(--ui-warning-soft) 0%, var(--ui-bg-surface) 100%)',
    header: 'var(--ui-warning)',
    layout: 'layout-creative',
    sidebar: true,
    line1: '70%',
    line2: '40%',
    line3: '80%',
    line4: '60%',
  },
  {
    id: 'dual-creative',
    name: '双列创意版',
    desc: '兼顾信息密度与视觉美感，适合前端开发、交互设计、产品经理',
    bg: 'linear-gradient(180deg, var(--ui-danger-soft) 0%, var(--ui-bg-surface) 100%)',
    header: 'var(--ui-danger)',
    layout: 'layout-creative',
    sidebar: true,
    line1: '68%',
    line2: '45%',
    line3: '72%',
    line4: '55%',
  },
]
</script>

<style scoped>
.template-market {
  display: flex;
  flex-direction: column;
  gap: var(--ui-space-4);
  padding: var(--ui-space-5);
}

.template-market__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--ui-space-3);
}

.section-title {
  margin: 0;
  font-size: var(--ui-text-lg);
  font-weight: 600;
  color: var(--ui-fg-primary);
}

.template-market__hint {
  margin: var(--ui-space-1) 0 0;
  font-size: var(--ui-text-sm);
  color: var(--ui-fg-secondary);
}

.templates-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: var(--ui-space-4);
}

.template-card {
  display: flex;
  flex-direction: column;
  gap: var(--ui-space-3);
  padding: var(--ui-space-3);
  border: 1px solid var(--ui-border);
  border-radius: var(--ui-radius-lg);
  background: var(--ui-bg-surface);
  text-align: left;
  cursor: pointer;
  transition: border-color var(--ui-duration-fast) var(--ui-ease-out),
    box-shadow var(--ui-duration-fast) var(--ui-ease-out),
    transform var(--ui-duration-fast) var(--ui-ease-out);
}

.template-card:hover {
  border-color: var(--ui-border-strong);
  box-shadow: var(--ui-shadow-md);
  transform: translateY(-2px);
}

.template-card:focus-visible {
  outline: none;
  box-shadow: var(--ui-ring);
}

.template-card.is-selected {
  border-color: var(--ui-accent);
  box-shadow: var(--ui-shadow-sm);
}

.template-card__preview {
  position: relative;
  display: block;
  width: 100%;
  aspect-ratio: 3 / 4;
  padding: var(--ui-space-3);
  border-radius: var(--ui-radius-md);
  border: 1px solid var(--ui-border-subtle);
}

.template-card__sheet {
  display: flex;
  flex-direction: column;
  width: 100%;
  height: 100%;
  overflow: hidden;
  border-radius: var(--ui-radius-sm);
  border: 1px solid var(--ui-border);
  background: var(--ui-bg-surface);
}

.template-card__header {
  display: block;
  height: 18%;
  width: 100%;
}

.template-card__body {
  flex: 1;
  display: grid;
  gap: 6px;
  padding: 7px;
}

.layout-classic {
  grid-template-columns: minmax(0, 1fr);
}

.layout-modern,
.layout-creative {
  grid-template-columns: 26% minmax(0, 1fr);
}

.template-card__sidebar {
  display: block;
  border-radius: 3px;
  background: var(--ui-bg-active);
}

.template-card__lines {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.template-card__line {
  display: block;
  height: 5px;
  border-radius: 3px;
  background: var(--ui-bg-active);
}

.template-card__line.is-title {
  height: 7px;
  background: var(--ui-border-strong);
}

.template-card__line.is-subtitle {
  height: 4px;
}

.template-card__badge {
  position: absolute;
  top: var(--ui-space-3);
  right: var(--ui-space-3);
  padding: 2px var(--ui-space-2);
  border-radius: var(--ui-radius-full);
  background: var(--ui-accent);
  color: var(--ui-accent-fg);
  font-size: var(--ui-text-xs);
  font-weight: 600;
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

.template-card.is-selected .template-card__name {
  color: var(--ui-fg-accent);
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
  aspect-ratio: 3 / 4;
  border-radius: var(--ui-radius-md);
  background: var(--ui-bg-sunken);
}

.template-skeleton__line {
  height: 12px;
  border-radius: var(--ui-radius-sm);
  background: var(--ui-bg-sunken);
}

.template-skeleton__line--short {
  width: 60%;
}
</style>
