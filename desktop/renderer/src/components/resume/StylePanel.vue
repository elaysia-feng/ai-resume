<template>
  <section class="style-panel">
    <header class="style-panel__head">
      <h2 class="ui-section-head__title">样式</h2>
      <p class="style-panel__hint">调整排版后右侧预览会立即更新。</p>
    </header>

    <div class="control-group">
      <label class="ui-label" for="style-font">字体</label>
      <select
        id="style-font"
        class="ui-input ui-select"
        :value="localStyle.font"
        @change="update('font', $event.target.value)"
      >
        <option v-for="font in fonts" :key="font.value" :value="font.value">
          {{ font.label }}
        </option>
      </select>
    </div>

    <div class="control-group">
      <span class="ui-label">正文字号</span>
      <div class="ui-segmented" role="group" aria-label="正文字号">
        <button
          v-for="size in fontSizes"
          :key="size"
          type="button"
          class="ui-segmented__item"
          :class="{ 'is-active': Number(localStyle.fontSize) === size }"
          :aria-pressed="Number(localStyle.fontSize) === size"
          @click="update('fontSize', size)"
        >
          {{ size }}px
        </button>
      </div>
    </div>

    <div class="control-group">
      <span class="ui-label">行距</span>
      <div class="ui-segmented" role="group" aria-label="行距">
        <button
          v-for="sp in spacingOptions"
          :key="sp.value"
          type="button"
          class="ui-segmented__item"
          :class="{ 'is-active': localStyle.spacing === sp.value }"
          :aria-pressed="localStyle.spacing === sp.value"
          @click="update('spacing', sp.value)"
        >
          {{ sp.label }}
        </button>
      </div>
    </div>

    <div class="control-group">
      <span class="ui-label">区块间距</span>
      <div class="ui-segmented" role="group" aria-label="区块间距">
        <button
          v-for="gap in gapOptions"
          :key="gap.value"
          type="button"
          class="ui-segmented__item"
          :class="{ 'is-active': localStyle.sectionGap === gap.value }"
          :aria-pressed="localStyle.sectionGap === gap.value"
          @click="update('sectionGap', gap.value)"
        >
          {{ gap.label }}
        </button>
      </div>
    </div>

    <div class="control-group">
      <span class="ui-label">强调色</span>
      <div class="swatches">
        <button
          v-for="color in accentColors"
          :key="color.value"
          type="button"
          class="swatch"
          :class="{ 'is-active': localStyle.color === color.value }"
          :style="{ background: color.value }"
          :title="color.name"
          :aria-label="color.name"
          :aria-pressed="localStyle.color === color.value"
          @click="update('color', color.value)"
        >
          <AppIcon v-if="localStyle.color === color.value" name="check" :size="14" class="swatch__check" />
        </button>
      </div>
    </div>

    <div v-if="showSidebarColor" class="control-group">
      <span class="ui-label">左栏配色</span>
      <div class="swatches">
        <button
          v-for="color in sidebarColors"
          :key="color.value"
          type="button"
          class="swatch swatch--block"
          :class="{ 'is-active': localStyle.sidebarColor === color.value }"
          :title="color.name"
          :aria-pressed="localStyle.sidebarColor === color.value"
          @click="update('sidebarColor', color.value)"
        >
          <span class="swatch__chip" :style="{ background: color.swatch }"></span>
          {{ color.label }}
        </button>
      </div>
    </div>

    <div class="control-group">
      <span class="ui-label">区块分隔线</span>
      <label class="switch">
        <input
          type="checkbox"
          class="switch__input"
          :checked="localStyle.dividers !== false"
          @change="update('dividers', $event.target.checked)"
        />
        <span class="switch__track" aria-hidden="true"></span>
        <span class="switch__text">{{ localStyle.dividers !== false ? '显示' : '隐藏' }}</span>
      </label>
    </div>

    <div class="style-panel__preview-wrap">
      <span class="ui-label">样式示意</span>
      <div class="style-panel__preview" :style="previewStyle">
        <p class="preview-name">姓名</p>
        <p class="preview-job">职位</p>
        <p class="preview-section">分区标题</p>
        <p class="preview-body">这里展示当前字号、行距与区块间距的实际排版效果。</p>
      </div>
    </div>
  </section>
</template>

<script setup>
import { reactive, watch, computed } from 'vue'
import AppIcon from '../common/AppIcon.vue'

const props = defineProps({
  modelValue: {
    type: Object,
  },
})

const emit = defineEmits(['update:modelValue'])

const localStyle = reactive({ ...props.modelValue })

watch(
  () => props.modelValue,
  (val) => Object.assign(localStyle, val),
  { deep: true },
)

function update(key, value) {
  localStyle[key] = value
  emit('update:modelValue', { ...localStyle })
}

// 字体栈与设计系统保持一致：界面字体为系统栈，衬线走 --ui-font-display
const fonts = [
  { value: 'Inter', label: '系统无衬线（默认）', stack: "var(--ui-font-sans)" },
  { value: 'Roboto', label: 'Roboto', stack: "'Roboto', var(--ui-font-sans)" },
  { value: 'Merriweather', label: 'Merriweather', stack: "'Merriweather', Georgia, var(--ui-font-display)" },
  { value: 'Georgia', label: 'Georgia 衬线', stack: "Georgia, 'Songti SC', var(--ui-font-display)" },
]

const accentColors = [
  { value: 'var(--ui-brand-600)', name: '品牌绿' },
  { value: 'var(--ui-info)', name: '天蓝' },
  { value: 'var(--ui-warning)', name: '琥珀' },
  { value: 'var(--ui-danger)', name: '砖红' },
  { value: 'var(--ui-neutral-800)', name: '墨黑' },
  { value: 'var(--ui-brand-800)', name: '松绿' },
]

const sidebarColors = [
  {
    value: 'blue',
    name: '深蓝',
    label: '深蓝',
    swatch: 'linear-gradient(180deg, var(--ui-info) 0%, var(--ui-neutral-950) 100%)',
  },
  { value: 'black', name: '墨黑', label: '墨黑', swatch: 'linear-gradient(180deg, var(--ui-neutral-900) 0%, var(--ui-neutral-800) 100%)' },
  { value: 'white', name: '纯白', label: '纯白', swatch: 'linear-gradient(180deg, var(--ui-neutral-0) 0%, var(--ui-neutral-50) 100%)' },
  { value: 'gray', name: '浅灰', label: '浅灰', swatch: 'linear-gradient(180deg, var(--ui-neutral-100) 0%, var(--ui-neutral-50) 100%)' },
]

const showSidebarColor = computed(() => ['compact', 'dual-creative', 'sales-growth'].includes(localStyle.template))

const fontSizes = [9, 10, 11, 12]

const spacingOptions = [
  { label: '紧凑', value: 'compact' },
  { label: '标准', value: 'normal' },
  { label: '宽松', value: 'relaxed' },
]

const gapOptions = [
  { label: '紧凑', value: 'compact' },
  { label: '标准', value: 'normal' },
  { label: '宽松', value: 'relaxed' },
]

const lineHeightMap = { compact: 1.35, normal: 1.6, relaxed: 1.85 }
const gapMap = { compact: 10, normal: 16, relaxed: 22 }

const previewStyle = computed(() => {
  const font = fonts.find((item) => item.value === localStyle.font) || fonts[0]
  const fontSize = Number(localStyle.fontSize) || 10

  return {
    fontFamily: font.stack,
    fontSize: `${fontSize}px`,
    lineHeight: String(lineHeightMap[localStyle.spacing] || lineHeightMap.normal),
    '--preview-accent': localStyle.color || 'var(--ui-brand-600)',
    '--preview-gap': `${gapMap[localStyle.sectionGap] || gapMap.normal}px`,
    '--preview-divider': localStyle.dividers === false ? 'none' : '1px solid var(--ui-border)',
  }
})
</script>

<style scoped>
.style-panel {
  display: flex;
  flex-direction: column;
  gap: var(--ui-space-5);
  padding: var(--ui-space-5);
}

.style-panel__head {
  display: flex;
  flex-direction: column;
  gap: var(--ui-space-1);
}

.style-panel__hint {
  margin: 0;
  font-size: var(--ui-text-sm);
  color: var(--ui-fg-secondary);
}

.control-group {
  display: flex;
  flex-direction: column;
}

.control-group :deep(.ui-label) {
  margin-bottom: var(--ui-space-2);
}

/* ---------- 分段选择 ---------- */
/* 字号 / 行距 / 间距统一使用 .ui-segmented，这里只做换行适配 */
.control-group :deep(.ui-segmented) {
  flex-wrap: wrap;
}

/* ---------- 色板 ---------- */
.swatches {
  display: flex;
  flex-wrap: wrap;
  gap: var(--ui-space-2);
}

.swatch {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: var(--ui-space-2);
  width: 32px;
  height: 32px;
  padding: 0;
  border: 2px solid transparent;
  border-radius: var(--ui-radius-md);
  cursor: pointer;
  box-shadow: inset 0 0 0 1px var(--ui-border);
  transition: border-color var(--ui-duration-fast) var(--ui-ease-out),
    transform var(--ui-duration-fast) var(--ui-ease-out);
}

.swatch:hover {
  transform: translateY(-1px);
}

.swatch:focus-visible {
  outline: none;
  box-shadow: var(--ui-ring);
}

.swatch.is-active {
  border-color: var(--ui-accent);
}

.swatch--block {
  width: auto;
  padding: 0 var(--ui-space-2) 0 3px;
  background: var(--ui-bg-surface);
  color: var(--ui-fg-secondary);
  font-size: var(--ui-text-xs);
  font-weight: 500;
}

.swatch--block.is-active {
  color: var(--ui-fg-primary);
}

.swatch__chip {
  width: 14px;
  height: 14px;
  border-radius: var(--ui-radius-xs);
  box-shadow: inset 0 0 0 1px var(--ui-border);
}

.swatch__check {
  color: var(--ui-accent-fg);
  filter: drop-shadow(0 1px 1px var(--ui-overlay));
}

/* ---------- 开关 ---------- */
.switch {
  display: inline-flex;
  align-items: center;
  gap: var(--ui-space-2);
  cursor: pointer;
}

.switch__input {
  position: absolute;
  opacity: 0;
  width: 0;
  height: 0;
}

.switch__track {
  position: relative;
  width: 36px;
  height: 20px;
  border-radius: var(--ui-radius-full);
  background: var(--ui-bg-active);
  transition: background var(--ui-duration-fast) var(--ui-ease-out);
}

.switch__track::after {
  content: '';
  position: absolute;
  top: 2px;
  left: 2px;
  width: 16px;
  height: 16px;
  border-radius: 50%;
  background: var(--ui-bg-surface);
  box-shadow: var(--ui-shadow-xs);
  transition: transform var(--ui-duration-fast) var(--ui-ease-out);
}

.switch__input:checked + .switch__track {
  background: var(--ui-accent);
}

.switch__input:checked + .switch__track::after {
  transform: translateX(16px);
}

.switch__input:focus-visible + .switch__track {
  box-shadow: var(--ui-ring);
}

.switch__text {
  font-size: var(--ui-text-sm);
  color: var(--ui-fg-secondary);
}

/* ---------- 实时预览 ---------- */
.style-panel__preview-wrap {
  display: flex;
  flex-direction: column;
  gap: var(--ui-space-2);
  padding-top: var(--ui-space-4);
  border-top: 1px solid var(--ui-border);
}

.style-panel__preview {
  padding: var(--ui-space-4);
  border: 1px solid var(--ui-border);
  border-radius: var(--ui-radius-md);
  background: var(--ui-bg-surface);
  color: var(--ui-fg-primary);
}

.preview-name {
  margin: 0;
  font-size: 1.4em;
  font-weight: 700;
  color: var(--ui-fg-primary);
}

.preview-job {
  margin: 2px 0 0;
  font-size: 1.1em;
  color: var(--ui-fg-secondary);
}

.preview-section {
  margin: var(--preview-gap) 0 0;
  padding-bottom: 3px;
  border-bottom: var(--preview-divider);
  font-weight: 700;
  color: var(--preview-accent);
}

.preview-body {
  margin: var(--ui-space-1) 0 0;
  color: var(--ui-fg-primary);
}
</style>
