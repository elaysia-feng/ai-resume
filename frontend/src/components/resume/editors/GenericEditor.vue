<template>
  <div class="generic-editor">
    <div v-if="isActive">
      <!-- TEXT: textarea editor -->
      <div v-if="schemaType === 'TEXT'" class="text-editor">
        <RichTextEditor
          class="text-rich-editor"
          :model-value="localText"
          placeholder="请输入文本内容..."
          min-height="180px"
          @update:model-value="handleTextChange"
        />
        <div class="editor-actions">
          <!-- Built-in sections auto-save (no button shown); CUSTOM sections require explicit save -->
          <button v-if="isCustom" class="save-btn" @click="saveText">保存</button>
          <span v-else class="auto-save-hint">自动保存</span>
        </div>
      </div>

      <!-- LIST / TAGS: add-item list editor -->
      <div v-else class="list-editor">
        <div
          v-for="(item, index) in localItems"
          :key="index"
          class="list-entry"
        >
          <div class="entry-index">{{ index + 1 }}</div>
          <div class="entry-fields">
            <!-- LIST items have a 'text' field -->
            <input
              v-if="schemaType === 'LIST'"
              class="entry-input"
              type="text"
              v-model="item.text"
              placeholder="输入内容..."
              @input="emitUpdate"
            />
            <!-- TAGS items use a tag chip -->
            <div v-else class="tag-chip">
              <span class="tag-text">{{ item }}</span>
              <button class="tag-remove" @click="removeItem(index)">×</button>
            </div>
          </div>
          <button class="entry-delete" @click="removeItem(index)">删除</button>
        </div>

        <!-- Add item row -->
        <div class="add-row">
          <input
            v-if="schemaType === 'LIST'"
            class="add-input"
            type="text"
            v-model="newItemText"
            placeholder="输入内容后回车添加..."
            @keydown.enter.prevent="addItem"
          />
          <input
            v-else
            class="add-input"
            type="text"
            v-model="newItemText"
            placeholder="输入标签后回车添加..."
            @keydown.enter.prevent="addItem"
          />
          <button class="add-btn" @click="addItem">+ 添加</button>
        </div>
      </div>
    </div>

    <!-- Summary mode -->
    <div v-else class="summary-mode">
      <div v-if="schemaType === 'TEXT'" class="summary-text">
        {{ summaryText || '暂无内容' }}
      </div>
      <div v-else class="summary-list">
        <span v-for="(item, i) in localItems" :key="i" class="summary-tag">
          {{ schemaType === 'LIST' ? item.text : item }}
        </span>
        <span v-if="!localItems || localItems.length === 0" class="summary-empty">
          暂无内容
        </span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import RichTextEditor from '../../common/RichTextEditor.vue'
import { stripRichText } from '../../../utils/richText.js'

const props = defineProps({
  // For TEXT schema: modelValue is the text string or { text: '' }
  // For LIST/TAGS schema: modelValue is the bare array (unwrapped by ResumeEditorView)
  modelValue: {
    type: [String, Array, Object],
    default: () => null,
  },
  isActive: {
    type: Boolean,
    default: false,
  },
  // schemaType is passed by ResumeEditorView via section metadata
  schemaType: {
    type: String,
    default: 'TEXT',
  },
  // true for user-created CUSTOM modules — shows explicit save button
  isCustom: {
    type: Boolean,
    default: false,
  },
})

const emit = defineEmits(['update:modelValue'])

// TEXT: local text state
const localText = ref(
  typeof props.modelValue === 'string'
    ? props.modelValue
    : (props.modelValue?.text ?? '')
)

// LIST/TAGS: local items state (always an array)
const localItems = ref(
  Array.isArray(props.modelValue)
    ? [...props.modelValue]
    : []
)

const newItemText = ref('')
const summaryText = computed(() => stripRichText(localText.value))

function saveText() {
  emit('update:modelValue', localText.value)
}

function handleTextChange(value) {
  localText.value = value
  if (!props.isCustom) {
    emit('update:modelValue', value)
  }
}

function emitUpdate() {
  emit('update:modelValue', [...localItems.value])
}

function addItem() {
  const text = newItemText.value.trim()
  if (!text) return
  if (props.schemaType === 'LIST') {
    localItems.value = [...localItems.value, { text }]
  } else {
    localItems.value = [...localItems.value, text]
  }
  newItemText.value = ''
  emitUpdate()
}

function removeItem(index) {
  const updated = [...localItems.value]
  updated.splice(index, 1)
  localItems.value = updated
  emitUpdate()
}
</script>

<style scoped>
.generic-editor {
  padding: 4px 0;
}

/* TEXT editor */
.text-editor {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.text-rich-editor {
  width: 100%;
}

.editor-actions {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 8px;
}

.save-btn {
  padding: 6px 16px;
  border: none;
  border-radius: 6px;
  background: #22c55e;
  color: #fff;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  font-family: inherit;
  transition: background 0.15s;
}

.save-btn:hover {
  background: #16a34a;
}

.auto-save-hint {
  font-size: 12px;
  color: #9ca3af;
  font-style: italic;
}

/* LIST/TAGS editor */
.list-editor {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.list-entry {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px;
  background: #f9fafb;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
}

.entry-index {
  font-size: 12px;
  font-weight: 700;
  color: #9ca3af;
  width: 20px;
  flex-shrink: 0;
  text-align: center;
}

.entry-fields {
  flex: 1;
  min-width: 0;
}

.entry-input {
  width: 100%;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  padding: 6px 10px;
  font-size: 14px;
  font-family: inherit;
  outline: none;
  box-sizing: border-box;
  background: #fff;
  transition: border-color 0.15s;
}

.entry-input:focus {
  border-color: #22c55e;
}

.tag-chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 10px;
  background: #dcfce7;
  border-radius: 999px;
  font-size: 13px;
  color: #22c55e;
}

.tag-text {
  font-weight: 500;
}

.tag-remove {
  background: none;
  border: none;
  cursor: pointer;
  color: #22c55e;
  font-size: 14px;
  padding: 0 2px;
  line-height: 1;
}

.entry-delete {
  background: none;
  border: none;
  color: #ef4444;
  font-size: 12px;
  cursor: pointer;
  flex-shrink: 0;
  padding: 0 4px;
}

.entry-delete:hover {
  text-decoration: underline;
}

/* Add row */
.add-row {
  display: flex;
  gap: 8px;
  align-items: center;
}

.add-input {
  flex: 1;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  padding: 8px 12px;
  font-size: 14px;
  font-family: inherit;
  outline: none;
  box-sizing: border-box;
  background: #fff;
  transition: border-color 0.15s;
}

.add-input:focus {
  border-color: #22c55e;
}

.add-btn {
  padding: 8px 14px;
  border: 1.5px dashed #22c55e;
  border-radius: 6px;
  background: none;
  color: #22c55e;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  font-family: inherit;
  white-space: nowrap;
  transition: background 0.15s;
}

.add-btn:hover {
  background: #dcfce7;
}

/* Summary mode */
.summary-mode {
  padding: 4px 0;
}

.summary-text {
  font-size: 14px;
  color: #1a1a2e;
  line-height: 1.6;
}

.summary-list {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.summary-tag {
  padding: 3px 10px;
  background: #dcfce7;
  border-radius: 999px;
  font-size: 12px;
  color: #22c55e;
}

.summary-empty {
  font-size: 13px;
  color: #9ca3af;
}
</style>
