<template>
  <div class="re-editor">
    <div v-if="isActive">
      <!-- TEXT：富文本 -->
      <div v-if="schemaType === 'TEXT'" class="re-field">
        <label class="ui-label">正文内容</label>
        <RichTextEditor
          class="re-rich"
          :model-value="localText"
          placeholder="请输入文本内容..."
          min-height="180px"
          @update:model-value="handleTextChange"
        />
        <div class="re-editor__actions">
          <!-- 内置模块自动保存；自定义模块需要显式保存 -->
          <button v-if="isCustom" type="button" class="ui-btn ui-btn--sm ui-btn--primary" @click="saveText">保存</button>
          <span v-else class="re-summary__muted">自动保存</span>
        </div>
      </div>

      <!-- LIST / TAGS：条目列表 -->
      <div v-else class="re-editor__list">
        <div v-if="!localItems.length" class="ui-empty">
          <div class="ui-empty__icon">
            <AppIcon name="note-blank" :size="24" />
          </div>
          <div class="ui-empty__title">这个模块还没有内容</div>
          <div class="ui-empty__hint">在下方输入框填写内容后回车，或点击「+ 添加」。</div>
        </div>

        <div v-else class="re-list">
          <article v-for="(item, index) in localItems" :key="index" class="ui-panel re-entry">
            <header class="re-entry__head">
              <span class="re-entry__title">
                <span class="re-entry__index">{{ index + 1 }}</span>
                第 {{ index + 1 }} 条
              </span>
              <button
                type="button"
                class="ui-icon-btn re-entry__remove"
                title="删除这一条"
                aria-label="删除这一条"
                @click="removeItem(index)"
              >
                <AppIcon name="trash" :size="16" />
              </button>
            </header>

            <div v-if="schemaType === 'LIST'" class="re-field">
              <label class="ui-label" :for="`gen-item-${index}`">内容</label>
              <input
                :id="`gen-item-${index}`"
                class="ui-input"
                :class="{ 're-input--invalid': entryErrors[index] }"
                type="text"
                :value="item.text"
                placeholder="输入内容..."
                @input="updateItem(index, $event.target.value)"
                @blur="touch(index)"
              />
              <p v-if="entryErrors[index]" class="re-error">{{ entryErrors[index] }}</p>
            </div>

            <div v-else class="re-field">
              <span class="ui-label">标签</span>
              <span class="re-item__tag">
                {{ item }}
                <button type="button" class="re-item__tag-remove" title="移除标签" aria-label="移除标签" @click="removeItem(index)"><AppIcon name="x" :size="12" /></button>
              </span>
            </div>
          </article>
        </div>

        <div class="re-add-row">
          <input
            v-model="newItemText"
            class="ui-input"
            type="text"
            :placeholder="schemaType === 'LIST' ? '输入内容后回车添加...' : '输入标签后回车添加...'"
            @keydown.enter.prevent="addItem"
          />
          <button type="button" class="ui-btn ui-btn--sm ui-btn--outline" @click="addItem"> <AppIcon name="plus" :size="16" /> 添加</button>
        </div>
      </div>
    </div>

    <!-- 折叠态摘要 -->
    <div v-else class="re-summary">
      <template v-if="schemaType === 'TEXT'">
        <p v-if="summaryText" class="re-summary__text">{{ summaryText }}</p>
        <p v-else class="re-summary__muted">暂无内容</p>
      </template>
      <template v-else>
        <div v-if="localItems.length" class="re-summary__tags">
          <span v-for="(item, index) in localItems" :key="index" class="ui-badge">
            {{ schemaType === 'LIST' ? item.text : item }}
          </span>
        </div>
        <p v-else class="re-summary__muted">暂无内容</p>
      </template>
    </div>
  </div>
</template>

<script setup>
import AppIcon from '../../common/AppIcon.vue'
import { computed, reactive, ref } from 'vue'
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

function updateItem(index, value) {
  localItems.value = localItems.value.map((item, itemIndex) =>
    itemIndex === index ? { ...item, text: value } : item
  )
  emitUpdate()
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

// ── 必填校验（失焦后提示）───────────────────────────────────────────────
const touched = reactive({})

function touch(index) {
  touched[index] = true
}

const entryErrors = computed(() => {
  const errors = {}
  if (props.schemaType !== 'LIST') return errors
  localItems.value.forEach((item, index) => {
    if (!touched[index]) return
    if (!String(item?.text || '').trim()) {
      errors[index] = '这一条还没有内容'
    }
  })
  return errors
})
</script>
