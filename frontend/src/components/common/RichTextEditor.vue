<template>
  <div class="rich-text-editor">
    <!-- 工具栏 -->
    <div class="rich-text-toolbar">
      <!-- 加粗/斜体/下划线 -->
      <div class="toolbar-group">
        <button class="toolbar-btn" :class="{ active: isBold }" @click="toggleBold" title="加粗 (Ctrl+B)">
          <svg viewBox="0 0 24 24" width="16" height="16" fill="currentColor">
            <path d="M6 4h8a4 4 0 0 1 4 4 4 4 0 0 1-4 4H6z"/>
            <path d="M6 12h9a4 4 0 0 1 4 4 4 4 0 0 1-4 4H6z"/>
          </svg>
        </button>
        <button class="toolbar-btn" :class="{ active: isItalic }" @click="toggleItalic" title="斜体 (Ctrl+I)">
          <svg viewBox="0 0 24 24" width="16" height="16" fill="currentColor">
            <line x1="19" y1="4" x2="10" y2="4" stroke="currentColor" stroke-width="2"/>
            <line x1="14" y1="20" x2="5" y2="20" stroke="currentColor" stroke-width="2"/>
            <line x1="15" y1="4" x2="9" y2="20" stroke="currentColor" stroke-width="2"/>
          </svg>
        </button>
        <button class="toolbar-btn" :class="{ active: isUnderline }" @click="toggleUnderline" title="下划线 (Ctrl+U)">
          <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M6 3v7a6 6 0 0 0 6 6 6 6 0 0 0 6-6V3"/>
            <line x1="4" y1="21" x2="20" y2="21"/>
          </svg>
        </button>
      </div>

      <!-- 分隔符 -->
      <div class="toolbar-divider"></div>

      <!-- 颜色选择器 -->
      <div class="toolbar-group">
        <div class="color-picker-wrapper" title="文字颜色">
          <input type="color" class="color-picker" v-model="currentColor" @change="setColor">
          <span class="color-preview" :style="{ backgroundColor: currentColor }"></span>
        </div>
      </div>

      <!-- 分隔符 -->
      <div class="toolbar-divider"></div>

      <!-- 对齐 -->
      <div class="toolbar-group">
        <button class="toolbar-btn" :class="{ active: isLeft }" @click="setAlign('left')" title="左对齐">
          <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2">
            <line x1="3" y1="6" x2="21" y2="6"/>
            <line x1="3" y1="12" x2="15" y2="12"/>
            <line x1="3" y1="18" x2="18" y2="18"/>
          </svg>
        </button>
        <button class="toolbar-btn" :class="{ active: isCenter }" @click="setAlign('center')" title="居中">
          <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2">
            <line x1="3" y1="6" x2="21" y2="6"/>
            <line x1="6" y1="12" x2="18" y2="12"/>
            <line x1="4" y1="18" x2="20" y2="18"/>
          </svg>
        </button>
        <button class="toolbar-btn" :class="{ active: isRight }" @click="setAlign('right')" title="右对齐">
          <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2">
            <line x1="3" y1="6" x2="21" y2="6"/>
            <line x1="9" y1="12" x2="21" y2="12"/>
            <line x1="6" y1="18" x2="21" y2="18"/>
          </svg>
        </button>
      </div>

      <!-- 分隔符 -->
      <div class="toolbar-divider"></div>

      <!-- 列表 -->
      <div class="toolbar-group">
        <button class="toolbar-btn" :class="{ active: isList }" @click="toggleList" title="无序列表">
          <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="4" cy="7" r="1.5" fill="currentColor"/>
            <circle cx="4" cy="12" r="1.5" fill="currentColor"/>
            <circle cx="4" cy="17" r="1.5" fill="currentColor"/>
            <line x1="9" y1="7" x2="21" y2="7"/>
            <line x1="9" y1="12" x2="21" y2="12"/>
            <line x1="9" y1="17" x2="21" y2="17"/>
          </svg>
        </button>
      </div>
    </div>

    <!-- 编辑器 -->
    <div
      ref="editorRef"
      class="rich-text-editor__content"
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
import { normalizeRichTextHtml, stripRichText } from '../../utils/richText.js'

const props = defineProps({
  modelValue: { type: String, default: '' },
  placeholder: { type: String, default: '请输入内容...' },
  minHeight: { type: String, default: '140px' },
})

const emit = defineEmits(['update:modelValue'])

const editorRef = ref(null)
const currentHtml = ref('')
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
.rich-text-editor {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.rich-text-toolbar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 4px;
  padding: 8px 10px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #f9fafb;
}

.toolbar-group {
  display: flex;
  gap: 2px;
}

.toolbar-divider {
  width: 1px;
  height: 20px;
  background: #e5e7eb;
  margin: 0 4px;
}

.toolbar-select {
  height: 30px;
  padding: 0 10px;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  background: #fff;
  font-size: 13px;
  color: #374151;
  cursor: pointer;
  outline: none;
  transition: border-color 0.15s, box-shadow 0.15s;
}

.toolbar-select:hover {
  border-color: #93c5fd;
}

.toolbar-select:focus {
  border-color: #3b82f6;
  box-shadow: 0 0 0 2px rgba(59, 130, 246, 0.15);
}

.toolbar-select--small {
  width: 60px;
}

.toolbar-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  border: 1px solid transparent;
  border-radius: 6px;
  background: transparent;
  color: #4b5563;
  cursor: pointer;
  transition: all 0.15s;
}

.toolbar-btn:hover {
  background: #e5e7eb;
  color: #1f2937;
}

.toolbar-btn.active {
  background: #eff6ff;
  color: #3b82f6;
  border-color: #bfdbfe;
}

.color-picker-wrapper {
  position: relative;
  display: flex;
  align-items: center;
}

.color-picker {
  width: 30px;
  height: 30px;
  padding: 2px;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  cursor: pointer;
  background: #fff;
}

.color-preview {
  position: absolute;
  left: 4px;
  width: 16px;
  height: 16px;
  border-radius: 3px;
  border: 1px solid rgba(0,0,0,0.1);
  pointer-events: none;
}

.rich-text-editor__content {
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  padding: 10px 12px;
  line-height: 1.6;
  color: #000;
  background: #fff;
  outline: none;
  overflow-y: auto;
}

.rich-text-editor__content:focus {
  border-color: #3b82f6;
}

.rich-text-editor__content.is-empty::before {
  content: attr(data-placeholder);
  color: #9ca3af;
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
    padding: 6px 8px;
  }

  .toolbar-btn {
    width: 26px;
    height: 26px;
  }

  .toolbar-select {
    height: 26px;
    font-size: 12px;
  }
}
</style>
