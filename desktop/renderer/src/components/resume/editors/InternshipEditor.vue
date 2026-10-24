<template>
  <div class="re-editor">
    <div v-if="isActive">
      <p v-if="missingCount > 0" class="re-notice">
        还有 {{ missingCount }} 个必填项未填写，标记 <span class="re-required">*</span> 的字段需要补充。
      </p>

      <div v-if="!localEntries().length" class="ui-empty">
        <div class="ui-empty__title">还没有实习经历</div>
        <div class="ui-empty__hint">实习经历同样按「公司 · 职位 · 时间」结构填写，突出具体产出。</div>
        <button type="button" class="ui-btn ui-btn--sm ui-btn--primary" @click="addEntry">+ 添加一条</button>
      </div>

      <div v-else class="re-list">
        <article v-for="(entry, index) in localEntries()" :key="index" class="ui-card re-entry">
          <header class="re-entry__head">
            <span class="re-entry__title">
              <span class="re-entry__index">{{ index + 1 }}</span>
              实习经历 {{ index + 1 }}
            </span>
            <button
              type="button"
              class="ui-btn ui-btn--sm ui-btn--ghost re-entry__remove"
              @click="removeEntry(index)"
            >
              删除
            </button>
          </header>

          <div class="re-form">
            <div class="re-field">
              <label class="ui-label" :for="`int-company-${index}`">实习公司 <span class="re-required">*</span></label>
              <input
                :id="`int-company-${index}`"
                class="ui-input"
                :class="{ 're-input--invalid': entryErrors[`${index}.company`] }"
                type="text"
                :value="entry.company"
                placeholder="如：阿里巴巴"
                @input="updateEntry(index, 'company', $event.target.value)"
                @blur="touch(`${index}.company`)"
              />
              <p v-if="entryErrors[`${index}.company`]" class="re-error">{{ entryErrors[`${index}.company`] }}</p>
            </div>

            <div class="re-field">
              <label class="ui-label" :for="`int-position-${index}`">实习职位 <span class="re-required">*</span></label>
              <input
                :id="`int-position-${index}`"
                class="ui-input"
                :class="{ 're-input--invalid': entryErrors[`${index}.position`] }"
                type="text"
                :value="entry.position"
                placeholder="如：Java 开发实习生"
                @input="updateEntry(index, 'position', $event.target.value)"
                @blur="touch(`${index}.position`)"
              />
              <p v-if="entryErrors[`${index}.position`]" class="re-error">{{ entryErrors[`${index}.position`] }}</p>
            </div>

            <div class="re-field">
              <label class="ui-label" :for="`int-start-${index}`">开始时间</label>
              <input
                :id="`int-start-${index}`"
                class="ui-input"
                type="text"
                :value="entry.startDate"
                placeholder="如：2022.06"
                @input="updateEntry(index, 'startDate', $event.target.value)"
              />
            </div>

            <div class="re-field">
              <label class="ui-label" :for="`int-end-${index}`">结束时间</label>
              <input
                :id="`int-end-${index}`"
                class="ui-input"
                type="text"
                :value="entry.endDate"
                placeholder="如：2022.09"
                @input="updateEntry(index, 'endDate', $event.target.value)"
              />
            </div>

            <div class="re-field re-field--full">
              <label class="ui-label">实习描述</label>
              <RichTextEditor
                class="re-rich"
                :model-value="entry.description"
                placeholder="请输入实习描述..."
                min-height="160px"
                @update:model-value="updateEntry(index, 'description', $event)"
              />
            </div>
          </div>
        </article>

        <button type="button" class="re-add" @click="addEntry">+ 添加一条</button>
      </div>
    </div>

    <div v-else class="re-summary">
      <div v-for="(entry, index) in modelValue" :key="index" class="re-summary__item">
        <strong>{{ entry.position || '职位' }}</strong>
        <span class="re-summary__muted"> @ {{ entry.company || '公司名称' }}</span>
        <span v-if="entry.startDate || entry.endDate" class="re-summary__muted">
          · {{ entry.startDate || '' }}{{ entry.endDate ? ' - ' + entry.endDate : '' }}
        </span>
      </div>
      <div v-if="!modelValue || modelValue.length === 0" class="re-summary__muted">暂无实习经历</div>
    </div>
  </div>
</template>

<script setup>
import { computed, reactive } from 'vue'
import RichTextEditor from '../../common/RichTextEditor.vue'

const props = defineProps({
  modelValue: {
    type: Array,
    default: () => [],
  },
  isActive: {
    type: Boolean,
    default: false,
  },
})

const emit = defineEmits(['update:modelValue'])

function createEmptyEntry() {
  return {
    company: '',
    position: '',
    startDate: '',
    endDate: '',
    description: '',
  }
}

const localEntries = () => (Array.isArray(props.modelValue) ? props.modelValue : [])

function updateEntry(index, field, value) {
  const updated = localEntries().map((item, itemIndex) =>
    itemIndex === index ? { ...item, [field]: value } : { ...item }
  )
  emit('update:modelValue', updated)
}

function addEntry() {
  emit('update:modelValue', [...localEntries(), createEmptyEntry()])
}

function removeEntry(index) {
  const updated = [...localEntries()]
  updated.splice(index, 1)
  emit('update:modelValue', updated)
}

// ── 必填校验（失焦后提示）───────────────────────────────────────────────
const REQUIRED_TEXT = {
  company: '请填写实习公司名称',
  position: '请填写实习职位',
}

const touched = reactive({})

function touch(key) {
  touched[key] = true
}

const entryErrors = computed(() => {
  const errors = {}
  localEntries().forEach((entry, index) => {
    Object.keys(REQUIRED_TEXT).forEach((field) => {
      const key = `${index}.${field}`
      if (!touched[key]) return
      if (!String(entry?.[field] || '').trim()) {
        errors[key] = REQUIRED_TEXT[field]
      }
    })
  })
  return errors
})

const missingCount = computed(() =>
  localEntries().reduce(
    (count, entry) =>
      count + Object.keys(REQUIRED_TEXT).filter((field) => !String(entry?.[field] || '').trim()).length,
    0
  )
)
</script>
