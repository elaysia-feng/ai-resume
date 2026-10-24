<template>
  <div class="re-editor">
    <div v-if="isActive">
      <p v-if="missingCount > 0" class="re-notice">
        还有 {{ missingCount }} 个必填项未填写，标记 <span class="re-required">*</span> 的字段需要补充。
      </p>

      <div v-if="!localEntries().length" class="ui-empty">
        <div class="ui-empty__icon">
          <AppIcon name="briefcase" :size="24" />
        </div>
        <div class="ui-empty__title">还没有工作经历</div>
        <div class="ui-empty__hint">按「公司 · 职位 · 时间」的结构填写，工作描述里尽量写量化结果。</div>
        <button type="button" class="ui-btn ui-btn--sm ui-btn--primary" @click="addEntry"> <AppIcon name="plus" :size="16" /> 添加一条</button>
      </div>

      <div v-else class="re-list">
        <article v-for="(entry, index) in localEntries()" :key="index" class="ui-panel re-entry">
          <header class="re-entry__head">
            <span class="re-entry__title">
              <span class="re-entry__index">{{ index + 1 }}</span>
              工作经历 {{ index + 1 }}
            </span>
            <button
              type="button"
              class="ui-icon-btn re-entry__remove"
              title="删除这一条"
              aria-label="删除这一条"
              @click="removeEntry(index)"
            >
              <AppIcon name="trash" :size="16" />
            </button>
          </header>

          <div class="re-form">
            <div class="re-field">
              <label class="ui-label" :for="`exp-company-${index}`">公司名称 <span class="re-required">*</span></label>
              <input
                :id="`exp-company-${index}`"
                class="ui-input"
                :class="{ 're-input--invalid': entryErrors[`${index}.company`] }"
                type="text"
                :value="entry.company"
                placeholder="如：字节跳动"
                @input="updateEntry(index, 'company', $event.target.value)"
                @blur="touch(`${index}.company`)"
              />
              <p v-if="entryErrors[`${index}.company`]" class="re-error">{{ entryErrors[`${index}.company`] }}</p>
            </div>

            <div class="re-field">
              <label class="ui-label" :for="`exp-position-${index}`">职位 <span class="re-required">*</span></label>
              <input
                :id="`exp-position-${index}`"
                class="ui-input"
                :class="{ 're-input--invalid': entryErrors[`${index}.position`] }"
                type="text"
                :value="entry.position"
                placeholder="如：后端开发工程师"
                @input="updateEntry(index, 'position', $event.target.value)"
                @blur="touch(`${index}.position`)"
              />
              <p v-if="entryErrors[`${index}.position`]" class="re-error">{{ entryErrors[`${index}.position`] }}</p>
            </div>

            <div class="re-field">
              <label class="ui-label" :for="`exp-start-${index}`">开始时间</label>
              <input
                :id="`exp-start-${index}`"
                class="ui-input"
                type="text"
                :value="entry.startDate"
                placeholder="如：2020.09"
                @input="updateEntry(index, 'startDate', $event.target.value)"
              />
            </div>

            <div class="re-field">
              <label class="ui-label" :for="`exp-end-${index}`">结束时间</label>
              <input
                :id="`exp-end-${index}`"
                class="ui-input"
                type="text"
                :value="entry.endDate"
                placeholder="如：2024.06"
                @input="updateEntry(index, 'endDate', $event.target.value)"
              />
              <label class="re-check">
                <input
                  type="checkbox"
                  :checked="entry.current"
                  @change="syncCurrent(index, $event.target.checked)"
                />
                <span>至今</span>
              </label>
            </div>

            <div class="re-field re-field--full">
              <label class="ui-label">工作描述</label>
              <RichTextEditor
                class="re-rich"
                :model-value="entry.description"
                placeholder="请输入工作描述..."
                min-height="160px"
                @update:model-value="updateEntry(index, 'description', $event)"
              />
            </div>
          </div>
        </article>

        <button type="button" class="re-add" @click="addEntry"> <AppIcon name="plus" :size="16" /> 添加一条</button>
      </div>
    </div>

    <div v-else class="re-summary">
      <div v-for="(entry, index) in modelValue" :key="index" class="re-summary__item">
        <strong>{{ entry.position || '职位' }}</strong>
        <span class="re-summary__muted"> @ {{ entry.company || '公司名称' }}</span>
        <span v-if="entry.startDate || entry.endDate" class="re-summary__muted">
          · {{ entry.startDate || '' }}{{ entry.current ? ' - 至今' : (entry.endDate ? ' - ' + entry.endDate : '') }}
        </span>
      </div>
      <div v-if="!modelValue || modelValue.length === 0" class="re-summary__muted">暂无工作经验</div>
    </div>
  </div>
</template>

<script setup>
import AppIcon from '../../common/AppIcon.vue'
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
    current: false,
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

function syncCurrent(index, checked) {
  const updated = localEntries().map((item, itemIndex) => {
    if (itemIndex !== index) {
      return { ...item }
    }
    return {
      ...item,
      current: checked,
      endDate: checked ? '' : item.endDate,
    }
  })
  emit('update:modelValue', updated)
}

// ── 必填校验（失焦后提示）───────────────────────────────────────────────
const REQUIRED_TEXT = {
  company: '请填写公司名称',
  position: '请填写职位名称',
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
