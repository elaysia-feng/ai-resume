<template>
  <section class="preview-panel">
    <header class="preview-panel__bar">
      <div class="preview-panel__lead">
        <span class="preview-panel__title">简历预览</span>
        <span class="ui-badge">A4</span>
      </div>

      <div class="preview-panel__zoom" role="group" aria-label="预览缩放">
        <button
          v-for="option in zoomOptions"
          :key="option.value"
          type="button"
          class="preview-panel__zoom-btn"
          :class="{ 'is-active': zoomMode === option.value }"
          :aria-pressed="zoomMode === option.value"
          @click="zoomMode = option.value"
        >
          {{ option.label }}
        </button>
      </div>

      <div class="preview-panel__actions">
        <button
          type="button"
          class="ui-btn ui-btn--sm ui-btn--outline"
          :disabled="Boolean(exporting)"
          @click="handleExport('pdf')"
        >
          {{ exporting === 'pdf' ? '导出中…' : '导出 PDF' }}
        </button>
        <button
          type="button"
          class="ui-btn ui-btn--sm ui-btn--outline"
          :disabled="Boolean(exporting)"
          @click="handleExport('image')"
        >
          {{ exporting === 'image' ? '导出中…' : '导出图片' }}
        </button>
        <button type="button" class="ui-btn ui-btn--sm ui-btn--ghost" @click="emit('close')">
          关闭
        </button>
      </div>
    </header>

    <p v-if="errorMessage" class="preview-panel__error">{{ errorMessage }}</p>

    <div ref="viewportRef" class="preview-panel__viewport ui-scroll">
      <div class="preview-panel__stage" :style="stageStyle">
        <div ref="sheetRef" class="preview-panel__sheet" :style="sheetScaleStyle">
          <article v-if="resumeData" class="preview-sheet">
            <header class="preview-sheet__header">
              <h1 v-if="resumeData.name" class="preview-sheet__name">{{ resumeData.name }}</h1>
              <p v-if="resumeData.title" class="preview-sheet__title">{{ resumeData.title }}</p>
              <p v-if="resumeData.contact" class="preview-sheet__contact">{{ resumeData.contact }}</p>
            </header>

            <section
              v-for="(section, key) in resumeData.sections || []"
              :key="key"
              class="preview-sheet__section"
            >
              <h2 class="preview-sheet__section-title">{{ section.title }}</h2>
              <div v-for="(item, idx) in section.items || []" :key="idx" class="preview-sheet__item">
                <p v-if="item.header" class="preview-sheet__item-header">{{ item.header }}</p>
                <p v-if="item.sub" class="preview-sheet__item-sub">{{ item.sub }}</p>
                <p v-if="item.body" class="preview-sheet__item-body">{{ item.body }}</p>
              </div>
            </section>
          </article>

          <p v-else class="preview-sheet__empty">暂无可预览的简历内容</p>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { exportToImage, exportToPDF } from '../../utils/export.js'

const PAGE_WIDTH = 794

const props = defineProps({
  resumeData: {
    type: Object,
    default: null,
  },
})

const emit = defineEmits(['close'])

const viewportRef = ref(null)
const sheetRef = ref(null)
const zoomMode = ref('fit')
const fitScale = ref(1)
const sheetHeight = ref(1123)
const exporting = ref('')
const errorMessage = ref('')

const zoomOptions = [
  { label: '适应宽度', value: 'fit' },
  { label: '50%', value: 0.5 },
  { label: '75%', value: 0.75 },
  { label: '100%', value: 1 },
]

const scale = computed(() => {
  if (zoomMode.value === 'fit') {
    return fitScale.value
  }
  return Number(zoomMode.value)
})

const stageStyle = computed(() => ({
  width: `${Math.round(PAGE_WIDTH * scale.value)}px`,
  height: `${Math.round(sheetHeight.value * scale.value)}px`,
}))

const sheetScaleStyle = computed(() => ({
  transform: `scale(${scale.value})`,
}))

let viewportObserver = null
let sheetObserver = null

function updateFitScale() {
  const viewport = viewportRef.value
  if (!viewport) return
  const available = viewport.clientWidth - 48
  if (available <= 0) return
  fitScale.value = Number(Math.min(1, Math.max(0.25, available / PAGE_WIDTH)).toFixed(4))
}

function updateSheetHeight() {
  if (!sheetRef.value) return
  sheetHeight.value = Math.max(sheetRef.value.offsetHeight, 1123)
}

onMounted(() => {
  updateFitScale()
  updateSheetHeight()

  if (typeof ResizeObserver === 'undefined') return

  viewportObserver = new ResizeObserver(() => {
    updateFitScale()
  })
  if (viewportRef.value) {
    viewportObserver.observe(viewportRef.value)
  }

  sheetObserver = new ResizeObserver(() => {
    updateSheetHeight()
  })
  if (sheetRef.value) {
    sheetObserver.observe(sheetRef.value)
  }
})

onBeforeUnmount(() => {
  viewportObserver?.disconnect()
  sheetObserver?.disconnect()
})

function buildFilename() {
  const rawName = String(props.resumeData?.name || 'resume').trim()
  const safeName = rawName.replace(/[<>:"/\\|?*\x00-\x1F]/g, '').replace(/\s+/g, '_')
  return safeName || 'resume'
}

async function handleExport(type) {
  if (exporting.value || !sheetRef.value) return

  exporting.value = type
  errorMessage.value = ''

  // 导出按 A4 原尺寸渲染，先把缩放恢复到 100%
  const previousZoom = zoomMode.value
  zoomMode.value = 1
  await nextTick()

  try {
    if (type === 'pdf') {
      await exportToPDF(sheetRef.value, buildFilename())
    } else {
      await exportToImage(sheetRef.value, buildFilename())
    }
  } catch (error) {
    console.error('导出失败', error)
    errorMessage.value = type === 'pdf'
      ? '导出 PDF 失败，请稍后重试或改用「导出图片」。'
      : '导出图片失败，请稍后重试。'
  } finally {
    zoomMode.value = previousZoom
    exporting.value = ''
  }
}
</script>

<style scoped>
.preview-panel {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
  background: var(--ui-bg-canvas);
  color: var(--ui-fg-primary);
}

/* ---------- 顶部工具条 ---------- */
.preview-panel__bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--ui-space-4);
  flex-wrap: wrap;
  padding: var(--ui-space-3) var(--ui-space-4);
  border-bottom: 1px solid var(--ui-border);
  background: var(--ui-bg-surface);
}

.preview-panel__lead {
  display: flex;
  align-items: center;
  gap: var(--ui-space-2);
}

.preview-panel__title {
  font-size: var(--ui-text-md);
  font-weight: 600;
}

.preview-panel__zoom {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  padding: 2px;
  border: 1px solid var(--ui-border);
  border-radius: var(--ui-radius-md);
  background: var(--ui-bg-sunken);
}

.preview-panel__zoom-btn {
  padding: 3px var(--ui-space-3);
  border: 0;
  border-radius: var(--ui-radius-sm);
  background: transparent;
  color: var(--ui-fg-secondary);
  font-size: var(--ui-text-xs);
  font-weight: 500;
  cursor: pointer;
  transition: background var(--ui-duration-fast) var(--ui-ease-out),
    color var(--ui-duration-fast) var(--ui-ease-out);
}

.preview-panel__zoom-btn:hover {
  background: var(--ui-bg-hover);
  color: var(--ui-fg-primary);
}

.preview-panel__zoom-btn:focus-visible {
  outline: none;
  box-shadow: var(--ui-ring);
}

.preview-panel__zoom-btn.is-active {
  background: var(--ui-bg-surface);
  color: var(--ui-fg-accent);
  box-shadow: var(--ui-shadow-xs);
}

.preview-panel__actions {
  display: flex;
  align-items: center;
  gap: var(--ui-space-2);
}

.preview-panel__error {
  margin: 0;
  padding: var(--ui-space-2) var(--ui-space-4);
  border-bottom: 1px solid var(--ui-danger-border);
  background: var(--ui-danger-soft);
  color: var(--ui-danger);
  font-size: var(--ui-text-sm);
}

/* ---------- 画布 ---------- */
.preview-panel__viewport {
  flex: 1;
  min-height: 0;
  overflow: auto;
  padding: var(--ui-space-6);
}

.preview-panel__stage {
  position: relative;
  margin: 0 auto;
}

.preview-panel__sheet {
  position: absolute;
  top: 0;
  left: 0;
  width: 794px;
  min-height: 1123px;
  transform-origin: top left;
  background: var(--ui-neutral-0);
  color: var(--ui-neutral-800);
  border: 1px solid var(--ui-neutral-200);
  border-radius: 3px;
  box-shadow: var(--ui-shadow-lg);
}

/* ---------- 纸面内容 ---------- */
.preview-sheet {
  padding: 56px 58px 60px;
  font-family: var(--ui-font-sans);
  font-size: 13px;
  line-height: 1.6;
  color: var(--ui-neutral-800);
}

.preview-sheet__header {
  padding-bottom: var(--ui-space-4);
  border-bottom: 1px solid var(--ui-neutral-200);
}

.preview-sheet__name {
  margin: 0;
  font-family: var(--ui-font-display);
  font-size: 26px;
  font-weight: 700;
  line-height: 1.2;
  color: var(--ui-neutral-900);
}

.preview-sheet__title {
  margin: var(--ui-space-1) 0 0;
  font-size: 14px;
  font-weight: 600;
  color: var(--ui-neutral-600);
}

.preview-sheet__contact {
  margin: var(--ui-space-2) 0 0;
  font-size: 12px;
  color: var(--ui-neutral-500);
}

.preview-sheet__section {
  margin-top: var(--ui-space-5);
}

.preview-sheet__section-title {
  margin: 0 0 var(--ui-space-2);
  padding-bottom: 5px;
  border-bottom: 1px solid var(--ui-neutral-200);
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.04em;
  color: var(--ui-neutral-900);
}

.preview-sheet__item {
  margin-bottom: var(--ui-space-3);
}

.preview-sheet__item-header {
  margin: 0;
  font-size: 13px;
  font-weight: 600;
  color: var(--ui-neutral-900);
}

.preview-sheet__item-sub {
  margin: 2px 0 0;
  font-size: 12px;
  color: var(--ui-neutral-600);
}

.preview-sheet__item-body {
  margin: var(--ui-space-1) 0 0;
  font-size: 12px;
  color: var(--ui-neutral-700);
  white-space: pre-wrap;
}

.preview-sheet__empty {
  margin: 0;
  padding: var(--ui-space-16) 0;
  text-align: center;
  color: var(--ui-neutral-500);
}
</style>
