<template>
  <div class="re-editor">
    <div v-if="isActive">
      <p v-if="missingCount > 0" class="re-notice">
        还有 {{ missingCount }} 个必填项未填写，标记 <span class="re-required">*</span> 的字段需要补充。
      </p>

      <div v-if="!localEntries().length" class="ui-empty">
        <div class="ui-empty__icon">
          <AppIcon name="code" :size="24" />
        </div>
        <div class="ui-empty__title">还没有技能特长</div>
        <div class="ui-empty__hint">按「技能 + 熟练度」逐条填写，岗位要求的关键技术尽量靠前。</div>
        <button type="button" class="ui-btn ui-btn--sm ui-btn--primary" @click="addEntry"> <AppIcon name="plus" :size="16" /> 添加一条</button>
      </div>

      <div v-else class="re-list">
        <article v-for="(entry, index) in localEntries()" :key="index" class="ui-panel re-entry">
          <header class="re-entry__head">
            <span class="re-entry__title">
              <span class="re-entry__index">{{ index + 1 }}</span>
              技能 {{ index + 1 }}
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
              <label class="ui-label" :for="`skill-name-${index}`">技能名称 <span class="re-required">*</span></label>
              <input
                :id="`skill-name-${index}`"
                class="ui-input"
                :class="{ 're-input--invalid': entryErrors[`${index}.name`] }"
                type="text"
                :value="entry.name"
                placeholder="如：Spring Boot"
                @input="updateEntry(index, 'name', $event.target.value)"
                @blur="touch(`${index}.name`)"
              />
              <p v-if="entryErrors[`${index}.name`]" class="re-error">{{ entryErrors[`${index}.name`] }}</p>
            </div>

            <div class="re-field">
              <label class="ui-label" :for="`skill-level-${index}`">熟练度</label>
              <select
                :id="`skill-level-${index}`"
                class="ui-input ui-select"
                :value="entry.proficiency"
                @change="updateEntry(index, 'proficiency', $event.target.value)"
              >
                <option value="">请选择</option>
                <option value="精通">精通</option>
                <option value="掌握">掌握</option>
                <option value="熟悉">熟悉</option>
                <option value="了解">了解</option>
              </select>
            </div>
          </div>
        </article>

        <button type="button" class="re-add" @click="addEntry"> <AppIcon name="plus" :size="16" /> 添加一条</button>
      </div>
    </div>

    <div v-else class="re-summary">
      <div v-if="modelValue && modelValue.length > 0" class="re-summary__tags">
        <span v-for="(skill, index) in modelValue" :key="index" class="ui-badge ui-badge--accent">
          {{ skill.name || '技能' }}
          <template v-if="skill.proficiency">· {{ skill.proficiency }}</template>
        </span>
      </div>
      <div v-else class="re-summary__muted">暂无技能特长</div>
    </div>
  </div>
</template>

<script setup>
import AppIcon from '../../common/AppIcon.vue'
import { computed, reactive } from 'vue'

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
  return { name: '', proficiency: '' }
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
  name: '请填写技能名称',
}

const touched = reactive({})

function touch(key) {
  touched[key] = true
}

const entryErrors = computed(() => {
  const errors = {}
  localEntries().forEach((entry, index) => {
    const key = `${index}.name`
    if (!touched[key]) return
    if (!String(entry?.name || '').trim()) {
      errors[key] = REQUIRED_TEXT.name
    }
  })
  return errors
})

const missingCount = computed(() =>
  localEntries().reduce(
    (count, entry) => count + (String(entry?.name || '').trim() ? 0 : 1),
    0
  )
)
</script>
