<template>
  <div class="re-editor">
    <div v-if="isActive">
      <p v-if="missingCount > 0" class="re-notice">
        还有 {{ missingCount }} 个必填项未填写，标记 <span class="re-required">*</span> 的字段需要补充。
      </p>

      <div class="re-field">
        <label class="ui-label" for="self-eval">评价正文 <span class="re-required">*</span></label>
        <RichTextEditor
          id="self-eval"
          class="re-rich"
          :model-value="modelValue"
          placeholder="请输入自我评价..."
          min-height="160px"
          @update:model-value="handleChange"
        />
        <p v-if="fieldError" class="re-error">{{ fieldError }}</p>
      </div>
    </div>

    <div v-else class="re-summary">
      <p v-if="modelValue" class="re-summary__text">{{ truncated }}</p>
      <p v-else class="re-summary__muted">暂无自我评价</p>
    </div>
  </div>
</template>

<script setup>
import { computed, reactive } from 'vue'
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

// ── 必填校验（正文为空时提示）──────────────────────────────────────────
const touched = reactive({})

const fieldError = computed(() => {
  if (!touched.content) return ''
  return stripRichText(props.modelValue).trim() ? '' : '请填写自我评价，说明你的能力与目标'
})

const missingCount = computed(() => (stripRichText(props.modelValue).trim() ? 0 : 1))

function handleChange(value) {
  touched.content = true
  emit('update:modelValue', value)
}
</script>
