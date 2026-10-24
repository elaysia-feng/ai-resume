<template>
  <div class="re-editor">
    <div v-if="isActive">
      <p v-if="missingCount > 0" class="re-notice">
        还有 {{ missingCount }} 个必填项未填写，标记 <span class="re-required">*</span> 的字段需要补充。
      </p>

      <div v-if="!localEntries().length" class="ui-empty">
        <div class="ui-empty__icon">
          <AppIcon name="graduation-cap" :size="24" />
        </div>
        <div class="ui-empty__title">还没有教育经历</div>
        <div class="ui-empty__hint">按「学校 · 专业 · 学历 · 时间」的结构填写，再补充在校期间的成果。</div>
        <button type="button" class="ui-btn ui-btn--sm ui-btn--primary" @click="addEntry"> <AppIcon name="plus" :size="16" /> 添加一条</button>
      </div>

      <div v-else class="re-list">
        <article v-for="(entry, index) in localEntries()" :key="index" class="ui-panel re-entry">
          <header class="re-entry__head">
            <span class="re-entry__title">
              <span class="re-entry__index">{{ index + 1 }}</span>
              教育经历 {{ index + 1 }}
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
              <label class="ui-label" :for="`edu-school-${index}`">学校 <span class="re-required">*</span></label>
              <input
                :id="`edu-school-${index}`"
                class="ui-input"
                :class="{ 're-input--invalid': entryErrors[`${index}.school`] }"
                type="text"
                :value="entry.school"
                placeholder="如：北京理工大学"
                @input="updateEntry(index, 'school', $event.target.value)"
                @blur="touch(`${index}.school`)"
              />
              <p v-if="entryErrors[`${index}.school`]" class="re-error">{{ entryErrors[`${index}.school`] }}</p>
            </div>

            <div class="re-field">
              <label class="ui-label" :for="`edu-degree-${index}`">学历</label>
              <select
                :id="`edu-degree-${index}`"
                class="ui-input ui-select"
                :value="entry.degree"
                @change="updateEntry(index, 'degree', $event.target.value)"
              >
                <option value="">请选择</option>
                <option value="高中">高中</option>
                <option value="大专">大专</option>
                <option value="本科">本科</option>
                <option value="硕士">硕士</option>
                <option value="博士">博士</option>
              </select>
            </div>

            <div class="re-field">
              <label class="ui-label" :for="`edu-major-${index}`">专业</label>
              <input
                :id="`edu-major-${index}`"
                class="ui-input"
                type="text"
                :value="entry.major"
                placeholder="如：计算机科学与技术"
                @input="updateEntry(index, 'major', $event.target.value)"
              />
            </div>

            <div class="re-field">
              <label class="ui-label" :for="`edu-gpa-${index}`">GPA</label>
              <input
                :id="`edu-gpa-${index}`"
                class="ui-input"
                type="text"
                :value="entry.gpa"
                placeholder="如：3.8/4.0"
                @input="updateEntry(index, 'gpa', $event.target.value)"
              />
            </div>

            <div class="re-field">
              <label class="ui-label" :for="`edu-start-${index}`">开始时间</label>
              <input
                :id="`edu-start-${index}`"
                class="ui-input"
                type="text"
                :value="entry.startDate"
                placeholder="如：2020.09"
                @input="updateEntry(index, 'startDate', $event.target.value)"
              />
            </div>

            <div class="re-field">
              <label class="ui-label" :for="`edu-end-${index}`">结束时间</label>
              <input
                :id="`edu-end-${index}`"
                class="ui-input"
                type="text"
                :value="entry.endDate"
                placeholder="如：2024.06"
                @input="updateEntry(index, 'endDate', $event.target.value)"
              />
            </div>

            <div class="re-field re-field--full">
              <label class="ui-label">在校描述</label>
              <RichTextEditor
                class="re-rich"
                :model-value="entry.description"
                placeholder="请输入在校经历描述..."
                min-height="150px"
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
        {{ entry.school || '学校名称' }} · {{ entry.degree || '学历' }}
      </div>
      <div v-if="!modelValue || modelValue.length === 0" class="re-summary__muted">暂无教育经历</div>
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
    school: '',
    degree: '',
    major: '',
    gpa: '',
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
  school: '请填写学校名称',
}

const touched = reactive({})

function touch(key) {
  touched[key] = true
}

const entryErrors = computed(() => {
  const errors = {}
  localEntries().forEach((entry, index) => {
    const key = `${index}.school`
    if (!touched[key]) return
    if (!String(entry?.school || '').trim()) {
      errors[key] = REQUIRED_TEXT.school
    }
  })
  return errors
})

const missingCount = computed(() =>
  localEntries().reduce(
    (count, entry) => count + (String(entry?.school || '').trim() ? 0 : 1),
    0
  )
)
</script>
