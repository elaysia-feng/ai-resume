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
import CompactTemplate from './CompactTemplate.vue'
import AcademicTemplate from './AcademicTemplate.vue'
import DualCreativeTemplate from './DualCreativeTemplate.vue'

const PAGE_HEIGHT = 1123

const props = defineProps({
  template: {
    type: String,
    default: 'classic',
    validator: (value) => ['classic', 'modern', 'creative', 'compact', 'academic', 'dual-creative'].includes(value),
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
    compact: CompactTemplate,
    academic: AcademicTemplate,
    'dual-creative': DualCreativeTemplate,
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

// 左栏配色是「纸面文档」的配色，不跟随界面深浅色主题，
// 因此全部由 --ui-* 中性色阶 / 品牌色阶推导，保证导出 PDF 后仍是同一张纸。
const sidebarThemeStyle = computed(() => {
  const themeMap = {
    black: {
      '--resume-sidebar-bg': 'linear-gradient(180deg, var(--ui-neutral-900) 0%, var(--ui-neutral-800) 100%)',
      '--resume-sidebar-border': 'var(--ui-neutral-700)',
      '--resume-sidebar-divider': 'var(--ui-neutral-700)',
      '--resume-sidebar-heading': 'var(--ui-neutral-50)',
      '--resume-sidebar-text': 'var(--ui-neutral-200)',
      '--resume-sidebar-text-soft': 'var(--ui-neutral-300)',
      '--resume-sidebar-text-faint': 'var(--ui-neutral-400)',
      '--resume-sidebar-accent': 'var(--ui-brand-300)',
      '--resume-sidebar-panel-bg': 'var(--ui-neutral-800)',
      '--resume-sidebar-panel-border': 'var(--ui-neutral-700)',
      '--resume-sidebar-chip-bg': 'var(--ui-neutral-700)',
    },
    blue: {
      // 这里必须用固定色值，不能用 color-mix()：
      // 导出 PDF 走 html2canvas，它不支持 color-mix()，
      // 解析不出来会渲染成黑色/透明块。
      '--resume-sidebar-bg': 'linear-gradient(180deg, #046ea5 0%, #074d72 100%)',
      '--resume-sidebar-border': '#083b55',
      '--resume-sidebar-divider': '#09334a',
      '--resume-sidebar-heading': 'var(--ui-neutral-50)',
      '--resume-sidebar-text': 'var(--ui-neutral-100)',
      '--resume-sidebar-text-soft': 'var(--ui-neutral-200)',
      '--resume-sidebar-text-faint': 'var(--ui-neutral-300)',
      '--resume-sidebar-accent': '#62b3dc',
      '--resume-sidebar-panel-bg': 'rgba(12, 10, 9, 0.55)',
      '--resume-sidebar-panel-border': 'rgba(2, 132, 199, 0.32)',
      '--resume-sidebar-chip-bg': 'rgba(12, 10, 9, 0.42)',
    },
    white: {
      '--resume-sidebar-bg': 'linear-gradient(180deg, var(--ui-neutral-0) 0%, var(--ui-neutral-50) 100%)',
      '--resume-sidebar-border': 'var(--ui-neutral-200)',
      '--resume-sidebar-divider': 'var(--ui-neutral-200)',
      '--resume-sidebar-heading': 'var(--ui-neutral-900)',
      '--resume-sidebar-text': 'var(--ui-neutral-700)',
      '--resume-sidebar-text-soft': 'var(--ui-neutral-600)',
      '--resume-sidebar-text-faint': 'var(--ui-neutral-500)',
      '--resume-sidebar-accent': 'var(--ui-brand-700)',
      '--resume-sidebar-panel-bg': 'var(--ui-neutral-50)',
      '--resume-sidebar-panel-border': 'var(--ui-neutral-200)',
      '--resume-sidebar-chip-bg': 'var(--ui-brand-50)',
    },
    gray: {
      '--resume-sidebar-bg': 'linear-gradient(180deg, var(--ui-neutral-100) 0%, var(--ui-neutral-50) 100%)',
      '--resume-sidebar-border': 'var(--ui-neutral-200)',
      '--resume-sidebar-divider': 'var(--ui-neutral-200)',
      '--resume-sidebar-heading': 'var(--ui-neutral-900)',
      '--resume-sidebar-text': 'var(--ui-neutral-700)',
      '--resume-sidebar-text-soft': 'var(--ui-neutral-600)',
      '--resume-sidebar-text-faint': 'var(--ui-neutral-500)',
      '--resume-sidebar-accent': 'var(--ui-brand-700)',
      '--resume-sidebar-panel-bg': 'var(--ui-neutral-0)',
      '--resume-sidebar-panel-border': 'var(--ui-neutral-200)',
      '--resume-sidebar-chip-bg': 'var(--ui-brand-50)',
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
