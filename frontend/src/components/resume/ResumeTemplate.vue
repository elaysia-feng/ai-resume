<template>
  <div class="resume-page-shell">
    <div v-if="isOverflowing" class="resume-page-status">
      <span class="resume-page-status__badge resume-page-status__badge--warning">
        当前内容已超出一页 A4，请适当精简内容
      </span>
    </div>

    <div ref="pageRef" class="resume-page" :class="{ 'resume-page--overflow': isOverflowing }">
      <div ref="pageContentRef" class="resume-page__content">
        <component
          :is="templateComponent"
          :sections="props.sections"
          :locale="props.locale"
          :style="templateStyle"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import '../../styles/resume-template-base.css'
import ClassicTemplate from './ClassicTemplate.vue'
import ModernTemplate from './ModernTemplate.vue'
import CreativeTemplate from './CreativeTemplate.vue'

const PAGE_HEIGHT = 1123

const props = defineProps({
  template: {
    type: String,
    default: 'classic',
    validator: (value) => ['classic', 'modern', 'creative'].includes(value),
  },
  sections: {
    type: Array,
    default: () => [],
  },
  locale: {
    type: String,
    default: 'zh',
  },
  bodyFontSize: {
    type: Number,
    default: 9,
  },
  sidebarColor: {
    type: String,
    default: 'gray',
  },
})

const pageRef = ref(null)
const pageContentRef = ref(null)
const isOverflowing = ref(false)
let resizeObserver = null

const templateComponent = computed(() => {
  const templateMap = {
    classic: ClassicTemplate,
    modern: ModernTemplate,
    creative: CreativeTemplate,
  }

  return templateMap[props.template] || ClassicTemplate
})

const typographyStyle = computed(() => {
  const bodyFontSize = [9, 10, 11, 12].includes(Number(props.bodyFontSize))
    ? Number(props.bodyFontSize)
    : 9

  return {
    '--resume-text-size': `${bodyFontSize}px`,
    '--resume-meta-size': `${Math.max(bodyFontSize - 1, 8)}px`,
    '--resume-date-size': `${Math.max(bodyFontSize - 1, 8)}px`,
    '--resume-chip-size': `${Math.max(bodyFontSize - 1, 8)}px`,
    '--resume-title-size': `${bodyFontSize + 2}px`,
  }
})

const sidebarThemeStyle = computed(() => {
  const themeMap = {
    black: {
      '--resume-sidebar-bg': 'linear-gradient(180deg, #111827 0%, #1f2937 100%)',
      '--resume-sidebar-border': 'rgba(255, 255, 255, 0.12)',
      '--resume-sidebar-divider': 'rgba(255, 255, 255, 0.14)',
      '--resume-sidebar-heading': '#f8fafc',
      '--resume-sidebar-text': 'rgba(248, 250, 252, 0.92)',
      '--resume-sidebar-text-soft': 'rgba(226, 232, 240, 0.82)',
      '--resume-sidebar-text-faint': 'rgba(203, 213, 225, 0.72)',
      '--resume-sidebar-accent': '#93c5fd',
      '--resume-sidebar-panel-bg': 'rgba(255, 255, 255, 0.08)',
      '--resume-sidebar-panel-border': 'rgba(255, 255, 255, 0.12)',
      '--resume-sidebar-chip-bg': 'rgba(255, 255, 255, 0.1)',
    },
    blue: {
      '--resume-sidebar-bg': 'linear-gradient(180deg, #1e3a8a 0%, #2563eb 100%)',
      '--resume-sidebar-border': 'rgba(191, 219, 254, 0.26)',
      '--resume-sidebar-divider': 'rgba(191, 219, 254, 0.24)',
      '--resume-sidebar-heading': '#eff6ff',
      '--resume-sidebar-text': 'rgba(239, 246, 255, 0.94)',
      '--resume-sidebar-text-soft': 'rgba(219, 234, 254, 0.82)',
      '--resume-sidebar-text-faint': 'rgba(191, 219, 254, 0.78)',
      '--resume-sidebar-accent': '#dbeafe',
      '--resume-sidebar-panel-bg': 'rgba(255, 255, 255, 0.1)',
      '--resume-sidebar-panel-border': 'rgba(191, 219, 254, 0.2)',
      '--resume-sidebar-chip-bg': 'rgba(255, 255, 255, 0.14)',
    },
    white: {
      '--resume-sidebar-bg': 'linear-gradient(180deg, #ffffff 0%, #f8fafc 100%)',
      '--resume-sidebar-border': '#dbe3ef',
      '--resume-sidebar-divider': '#dbe3ef',
      '--resume-sidebar-heading': '#102a43',
      '--resume-sidebar-text': '#243b53',
      '--resume-sidebar-text-soft': '#486581',
      '--resume-sidebar-text-faint': '#7b8794',
      '--resume-sidebar-accent': '#1d4ed8',
      '--resume-sidebar-panel-bg': 'rgba(241, 245, 249, 0.88)',
      '--resume-sidebar-panel-border': 'rgba(148, 163, 184, 0.2)',
      '--resume-sidebar-chip-bg': '#eef4fb',
    },
    gray: {
      '--resume-sidebar-bg': 'linear-gradient(180deg, #eef2f7 0%, #f8fbfc 100%)',
      '--resume-sidebar-border': '#d8e1ec',
      '--resume-sidebar-divider': '#d8e1ec',
      '--resume-sidebar-heading': '#134e4a',
      '--resume-sidebar-text': '#1f2937',
      '--resume-sidebar-text-soft': '#476a68',
      '--resume-sidebar-text-faint': '#6b7280',
      '--resume-sidebar-accent': '#0f766e',
      '--resume-sidebar-panel-bg': 'rgba(255, 255, 255, 0.78)',
      '--resume-sidebar-panel-border': 'rgba(15, 118, 110, 0.12)',
      '--resume-sidebar-chip-bg': '#e8f6f3',
    },
  }

  return themeMap[props.sidebarColor] || themeMap.gray
})

const templateStyle = computed(() => ({
  ...typographyStyle.value,
  ...sidebarThemeStyle.value,
}))

function measureOverflow() {
  const target = pageContentRef.value
  if (!target) {
    isOverflowing.value = false
    return
  }

  isOverflowing.value = target.offsetHeight > PAGE_HEIGHT + 1
}

watch(
  () => props.sections,
  async () => {
    await nextTick()
    measureOverflow()
  },
  { deep: true, immediate: true },
)

watch(
  () => props.template,
  async () => {
    await nextTick()
    measureOverflow()
  },
)

watch(
  () => props.bodyFontSize,
  async () => {
    await nextTick()
    measureOverflow()
  },
)

watch(
  () => props.sidebarColor,
  async () => {
    await nextTick()
    measureOverflow()
  },
)

onMounted(() => {
  measureOverflow()
  if (typeof ResizeObserver === 'undefined') {
    return
  }

  resizeObserver = new ResizeObserver(() => {
    measureOverflow()
  })

  if (pageContentRef.value) {
    resizeObserver.observe(pageContentRef.value)
  }
})

onBeforeUnmount(() => {
  resizeObserver?.disconnect()
})

function getExportElement() {
  return pageRef.value
}

defineExpose({
  getExportElement,
})
</script>
