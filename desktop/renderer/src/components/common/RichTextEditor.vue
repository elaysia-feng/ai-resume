<template>
  <div class="rich-text-editor">
    <!-- 工具栏 -->
    <div class="rich-text-toolbar">
      <!-- 加粗/斜体/下划线 -->
      <div class="toolbar-group">
        <button
          type="button"
          class="ui-icon-btn toolbar-btn"
          :class="{ 'is-active': isBold }"
          title="加粗 (Ctrl+B)"
          aria-label="加粗"
          @click="toggleBold"
        >
          <span class="toolbar-mark toolbar-mark--bold" aria-hidden="true">B</span>
        </button>
        <button
          type="button"
          class="ui-icon-btn toolbar-btn"
          :class="{ 'is-active': isItalic }"
          title="斜体 (Ctrl+I)"
          aria-label="斜体"
          @click="toggleItalic"
        >
          <span class="toolbar-mark toolbar-mark--italic" aria-hidden="true">I</span>
        </button>
        <button
          type="button"
          class="ui-icon-btn toolbar-btn"
          :class="{ 'is-active': isUnderline }"
          title="下划线 (Ctrl+U)"
          aria-label="下划线"
          @click="toggleUnderline"
        >
          <span class="toolbar-mark toolbar-mark--underline" aria-hidden="true">U</span>
        </button>
      </div>

      <!-- 分隔符 -->
      <span class="ui-divider toolbar-divider" aria-hidden="true"></span>

      <!-- 颜色选择器 -->
      <div class="toolbar-group">
        <div class="color-picker-wrapper" title="文字颜色">
          <AppIcon name="palette" :size="16" />
          <span class="color-picker">
            <input
              type="color"
              class="color-picker__input"
              v-model="currentColor"
              aria-label="文字颜色"
              @change="setColor"
            />
            <span class="color-preview" :style="{ backgroundColor: currentColor }"></span>
          </span>
        </div>
      </div>

      <!-- 分隔符 -->
      <span class="ui-divider toolbar-divider" aria-hidden="true"></span>

      <!-- 对齐 -->
      <div class="toolbar-group">
        <button
          type="button"
          class="ui-icon-btn toolbar-btn"
          :class="{ 'is-active': isLeft }"
          title="左对齐"
          aria-label="左对齐"
          @click="setAlign('left')"
        >
          <span class="align-mark align-mark--left" aria-hidden="true"></span>
        </button>
        <button
          type="button"
          class="ui-icon-btn toolbar-btn"
          :class="{ 'is-active': isCenter }"
          title="居中"
          aria-label="居中"
          @click="setAlign('center')"
        >
          <span class="align-mark align-mark--center" aria-hidden="true"></span>
        </button>
        <button
          type="button"
          class="ui-icon-btn toolbar-btn"
          :class="{ 'is-active': isRight }"
          title="右对齐"
          aria-label="右对齐"
          @click="setAlign('right')"
        >
          <span class="align-mark align-mark--right" aria-hidden="true"></span>
        </button>
      </div>

      <!-- 分隔符 -->
      <span class="ui-divider toolbar-divider" aria-hidden="true"></span>

      <!-- 列表 -->
      <div class="toolbar-group">
        <button
          type="button"
          class="ui-icon-btn toolbar-btn"
          :class="{ 'is-active': isList }"
          title="无序列表"
          aria-label="无序列表"
          @click="toggleList"
        >
          <AppIcon name="list" :size="16" />
        </button>
      </div>
    </div>

    <!-- 工具栏与编辑区之间的分隔线 -->
    <hr class="ui-divider" />

    <!-- 编辑器 -->
    <div
      ref="editorRef"
      class="ui-input rich-text-editor__content"
      :class="{ 'is-empty': isEmpty }"
      :data-placeholder="placeholder"
      :style="{ minHeight, fontSize: '12px', fontFamily: '宋体, SimSun, serif' }"
      contenteditable="true"
      @input="onInput"
      @blur="onBlur"
      @keyup="updateState"
      @mouseup="updateState"
    ></div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import AppIcon from './AppIcon.vue'
import { normalizeRichTextHtml, stripRichText } from '../../utils/richText.js'

const props = defineProps({
  modelValue: { type: String, default: '' },
  placeholder: { type: String, default: '请输入内容...' },
  minHeight: { type: String, default: '140px' },
})

const emit = defineEmits(['update:modelValue'])

const editorRef = ref(null)
const currentHtml = ref('')
// 原生颜色选择器只接受十六进制值，这里是颜色数据的默认值，不是样式色
const currentColor = ref('#000000')
const isBold = ref(false)
const isItalic = ref(false)
const isUnderline = ref(false)
const isLeft = ref(true)
const isCenter = ref(false)
const isRight = ref(false)
const isList = ref(false)

const isEmpty = computed(() => !stripRichText(currentHtml.value))

watch(
  () => props.modelValue,
  (val) => {
    if (editorRef.value && val !== currentHtml.value) {
      const normalized = normalizeRichTextHtml(val)
      currentHtml.value = normalized
      editorRef.value.innerHTML = normalized
    }
  },
  { immediate: true }
)

onMounted(() => {
  if (editorRef.value) {
    editorRef.value.innerHTML = currentHtml.value
  }
})

function onInput() {
  if (!editorRef.value) return
  currentHtml.value = editorRef.value.innerHTML
  emit('update:modelValue', currentHtml.value)
}

function onBlur() {
  if (!editorRef.value) return
  const html = normalizeRichTextHtml(editorRef.value.innerHTML)
  currentHtml.value = html
  editorRef.value.innerHTML = html
  emit('update:modelValue', html)
}

function updateState() {
  const sel = window.getSelection()
  if (!sel?.rangeCount || !editorRef.value?.contains(sel.anchorNode)) return

  isBold.value = document.queryCommandState('bold')
  isItalic.value = document.queryCommandState('italic')
  isUnderline.value = document.queryCommandState('underline')

  const align = document.queryCommandValue('justifyFull') || document.queryCommandValue('justifyLeft')
  isLeft.value = align === 'left'
  isCenter.value = align === 'center'
  isRight.value = align === 'right'

  isList.value = document.queryCommandState('insertUnorderedList')

  // 获取当前颜色
  const activeEl = getActiveElement()
  if (activeEl) {
    const color = activeEl.style.color
    if (color) {
      currentColor.value = color
    }
  }
}

function getActiveElement() {
  const sel = window.getSelection()
  if (!sel?.anchorNode) return null
  let node = sel.anchorNode
  if (node.nodeType === 3) node = node.parentElement
  while (node && node !== editorRef.value) {
    if (node.nodeType === 1) return node
    node = node.parentElement
  }
  return editorRef.value
}

function execCmd(command, value = null) {
  editorRef.value?.focus()
  document.execCommand(command, false, value)
  onInput()
  updateState()
}

function setColor() {
  execCmd('foreColor', currentColor.value)
}

function setAlign(align) {
  execCmd('justify' + align.charAt(0).toUpperCase() + align.slice(1))
}

function toggleBold() {
  execCmd('bold')
}

function toggleItalic() {
  execCmd('italic')
}

function toggleUnderline() {
  execCmd('underline')
}

function toggleList() {
  execCmd('insertUnorderedList')
}
</script>

<style scoped>
/* 工具栏 + 分隔线 + 编辑区合成一个输入框式的整体 */
.rich-text-editor {
  display: flex;
  flex-direction: column;
}

.rich-text-toolbar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--ui-space-1);
  padding: var(--ui-space-2);
  border: 1px solid var(--ui-border);
  border-bottom: 0;
  border-radius: var(--ui-radius-md) var(--ui-radius-md) 0 0;
  background: var(--ui-bg-sunken);
}

.toolbar-group {
  display: flex;
  align-items: center;
  gap: var(--ui-space-1);
}

/* 竖向分隔线：用 .ui-divider 的颜色规则 */
.toolbar-divider {
  width: 1px;
  height: 20px;
  margin: 0 var(--ui-space-2);
  flex-shrink: 0;
}

/* 激活态：品牌色背景 + 品牌色图标 */
.toolbar-btn.is-active {
  background: var(--ui-accent-soft);
  border-color: var(--ui-accent-border);
  color: var(--ui-fg-accent);
}

/* 加粗/斜体/下划线用字形标记，不用图标 */
.toolbar-mark {
  font-family: var(--ui-font-sans);
  font-size: var(--ui-text-sm);
  line-height: 1;
  pointer-events: none;
}

.toolbar-mark--bold {
  font-weight: 700;
}

.toolbar-mark--italic {
  font-style: italic;
  font-family: var(--ui-font-display);
}

.toolbar-mark--underline {
  text-decoration: underline;
  text-underline-offset: 2px;
}

/* 对齐标记：三根线按左/中/右排布 */
.align-mark {
  display: block;
  width: 16px;
  height: 12px;
  background-image: linear-gradient(currentColor, currentColor);
  background-repeat: no-repeat;
  background-size: 100% 2px, 68% 2px, 44% 2px;
  pointer-events: none;
}

.align-mark--left {
  background-position: left 0, left 5px, left 10px;
}

.align-mark--center {
  background-position: center 0, center 5px, center 10px;
}

.align-mark--right {
  background-position: right 0, right 5px, right 10px;
}

/* 颜色选择器：原生取色器盖在色块上 */
.color-picker-wrapper {
  display: inline-flex;
  align-items: center;
  gap: var(--ui-space-2);
  color: var(--ui-fg-secondary);
}

.color-picker {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border: 1px solid var(--ui-border);
  border-radius: var(--ui-radius-md);
  background: var(--ui-bg-surface);
  cursor: pointer;
  transition: border-color var(--ui-duration-fast) var(--ui-ease-out);
}

.color-picker:hover {
  border-color: var(--ui-border-strong);
}

.color-picker__input {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  padding: 0;
  border: 0;
  opacity: 0;
  cursor: pointer;
}

.color-preview {
  width: 16px;
  height: 16px;
  border-radius: var(--ui-radius-xs);
  border: 1px solid var(--ui-border-hairline);
  pointer-events: none;
}

/* 编辑区沿用 .ui-input 的边框与聚焦态 */
.rich-text-editor__content {
  padding: var(--ui-space-3) var(--ui-space-4);
  border-top: 0;
  border-radius: 0 0 var(--ui-radius-md) var(--ui-radius-md);
  line-height: 1.6;
  overflow-y: auto;
}

.rich-text-editor__content.is-empty::before {
  content: attr(data-placeholder);
  color: var(--ui-fg-muted);
}

.rich-text-editor__content :deep(p) {
  margin: 0 0 6px;
}

.rich-text-editor__content :deep(p:last-child) {
  margin-bottom: 0;
}

@media (max-width: 480px) {
  .rich-text-toolbar {
    gap: 2px;
    padding: var(--ui-space-1);
  }

  .toolbar-divider {
    margin: 0 var(--ui-space-1);
  }
}
</style>
