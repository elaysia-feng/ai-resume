<template>
  <div class="self-evaluation-editor">
    <div v-if="isActive" class="form">
      <RichTextEditor
        class="field-rich-editor"
        :model-value="modelValue"
        placeholder="请输入自我评价..."
        min-height="160px"
        @update:model-value="emit('update:modelValue', $event)"
      />
    </div>

    <div v-else class="summary">
      <p v-if="modelValue" class="summary-text">{{ truncated }}</p>
      <p v-else class="summary-empty">暂无自我评价</p>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import RichTextEditor from '../../common/RichTextEditor.vue'
import { stripRichText } from '../../../utils/richText.js'

const props = defineProps({
  modelValue: {
    type: String,
    default: '',
  },
  isActive: {
    type: Boolean,
    default: false,
  },
})

const emit = defineEmits(['update:modelValue'])

const truncated = computed(() => {
  const text = stripRichText(props.modelValue)
  if (!text) return ''
  return text.length > 120
    ? text.slice(0, 120) + '...'
    : text
})
</script>

<style scoped>
.self-evaluation-editor {
  padding: 16px 0;
}

.form {
  display: flex;
  flex-direction: column;
}

.field-rich-editor {
  width: 100%;
}

.summary {
  padding: 4px 0;
}

.summary-text {
  font-size: 14px;
  color: #1a1a2e;
  line-height: 1.6;
  margin: 0;
  white-space: pre-wrap;
}

.summary-empty {
  font-size: 13px;
  color: #9ca3af;
  margin: 0;
}
</style>
