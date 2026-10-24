<template>
  <div class="re-editor">
    <div v-if="isActive">
      <p v-if="missingCount > 0" class="re-notice">
        还有 {{ missingCount }} 个必填项未填写，标记 <span class="re-required">*</span> 的字段需要补充。
      </p>

      <div v-if="!localEntries().length" class="ui-empty">
        <div class="ui-empty__icon">
          <AppIcon name="buildings" :size="24" />
        </div>
        <div class="ui-empty__title">还没有校园经历</div>
        <div class="ui-empty__hint">社团、竞赛、志愿经历都可以写在这里，突出你的角色与成果。</div>
        <button type="button" class="ui-btn ui-btn--sm ui-btn--primary" @click="addEntry"> <AppIcon name="plus" :size="16" /> 添加一条</button>
      </div>

      <div v-else class="re-list">
        <article v-for="(entry, index) in localEntries()" :key="index" class="ui-panel re-entry">
          <header class="re-entry__head">
            <span class="re-entry__title">
              <span class="re-entry__index">{{ index + 1 }}</span>
              校园经历 {{ index + 1 }}
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
              <label class="ui-label" :for="`cam-org-${index}`">组织/社团 <span class="re-required">*</span></label>
              <input
                :id="`cam-org-${index}`"
                class="ui-input"
                :class="{ 're-input--invalid': entryErrors[`${index}.organization`] }"
                type="text"
                :value="entry.organization"
                placeholder="如：校 ACM 竞赛团队"
                @input="updateEntry(index, 'organization', $event.target.value)"
                @blur="touch(`${index}.organization`)"
              />
              <p v-if="entryErrors[`${index}.organization`]" class="re-error">{{ entryErrors[`${index}.organization`] }}</p>
            </div>

            <div class="re-field">
              <label class="ui-label" :for="`cam-role-${index}`">担任职务</label>
              <input
                :id="`cam-role-${index}`"
                class="ui-input"
                type="text"
                :value="entry.role"
                placeholder="如：队长"
                @input="updateEntry(index, 'role', $event.target.value)"
              />
            </div>

            <div class="re-field">
              <label class="ui-label" :for="`cam-start-${index}`">开始时间</label>
              <input
                :id="`cam-start-${index}`"
                class="ui-input"
                type="text"
                :value="entry.startDate"
                placeholder="如：2020.09"
                @input="updateEntry(index, 'startDate', $event.target.value)"
              />
            </div>

            <div class="re-field">
              <label class="ui-label" :for="`cam-end-${index}`">结束时间</label>
              <input
                :id="`cam-end-${index}`"
                class="ui-input"
                type="text"
                :value="entry.endDate"
                placeholder="如：2022.06"
                @input="updateEntry(index, 'endDate', $event.target.value)"
              />
            </div>

            <div class="re-field re-field--full">
              <label class="ui-label">经历描述</label>
              <RichTextEditor
                class="re-rich"
                :model-value="entry.description"
                placeholder="请输入经历描述..."
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
        <strong>{{ entry.role || '职务' }}</strong>
        <span class="re-summary__muted"> @ {{ entry.organization || '组织名称' }}</span>
      </div>
      <div v-if="!modelValue || modelValue.length === 0" class="re-summary__muted">暂无校园经历</div>
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
    organization: '',
    role: '',
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
  organization: '请填写组织或社团名称',
}

const touched = reactive({})

function touch(key) {
  touched[key] = true
}

const entryErrors = computed(() => {
  const errors = {}
  localEntries().forEach((entry, index) => {
    const key = `${index}.organization`
    if (!touched[key]) return
    if (!String(entry?.organization || '').trim()) {
      errors[key] = REQUIRED_TEXT.organization
    }
  })
  return errors
})

const missingCount = computed(() =>
  localEntries().reduce(
    (count, entry) => count + (String(entry?.organization || '').trim() ? 0 : 1),
    0
  )
)
</script>
