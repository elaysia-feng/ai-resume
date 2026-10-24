<template>
  <div class="re-editor">
    <div v-if="isActive">
      <p v-if="missingCount > 0" class="re-notice">
        还有 {{ missingCount }} 个必填项未填写，标记 <span class="re-required">*</span> 的字段需要补充。
      </p>

      <div v-if="!localEntries().length" class="ui-empty">
        <div class="ui-empty__title">还没有项目经历</div>
        <div class="ui-empty__hint">项目名称、担任角色与时间轴要能对上，技术栈单独列一行更容易被搜到。</div>
        <button type="button" class="ui-btn ui-btn--sm ui-btn--primary" @click="addEntry">+ 添加一条</button>
      </div>

      <div v-else class="re-list">
        <article v-for="(entry, index) in localEntries()" :key="index" class="ui-card re-entry">
          <header class="re-entry__head">
            <span class="re-entry__title">
              <span class="re-entry__index">{{ index + 1 }}</span>
              项目经历 {{ index + 1 }}
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
              <label class="ui-label" :for="`prj-name-${index}`">项目名称 <span class="re-required">*</span></label>
              <input
                :id="`prj-name-${index}`"
                class="ui-input"
                :class="{ 're-input--invalid': entryErrors[`${index}.name`] }"
                type="text"
                :value="entry.name"
                placeholder="如：分布式任务调度平台"
                @input="updateEntry(index, 'name', $event.target.value)"
                @blur="touch(`${index}.name`)"
              />
              <p v-if="entryErrors[`${index}.name`]" class="re-error">{{ entryErrors[`${index}.name`] }}</p>
            </div>

            <div class="re-field">
              <label class="ui-label" :for="`prj-role-${index}`">项目角色</label>
              <input
                :id="`prj-role-${index}`"
                class="ui-input"
                type="text"
                :value="entry.role"
                placeholder="如：后端负责人"
                @input="updateEntry(index, 'role', $event.target.value)"
              />
            </div>

            <div class="re-field">
              <label class="ui-label" :for="`prj-start-${index}`">开始时间</label>
              <input
                :id="`prj-start-${index}`"
                class="ui-input"
                type="text"
                :value="entry.startDate"
                placeholder="如：2022.01"
                @input="updateEntry(index, 'startDate', $event.target.value)"
              />
            </div>

            <div class="re-field">
              <label class="ui-label" :for="`prj-end-${index}`">结束时间</label>
              <input
                :id="`prj-end-${index}`"
                class="ui-input"
                type="text"
                :value="entry.endDate"
                placeholder="如：2022.06"
                @input="updateEntry(index, 'endDate', $event.target.value)"
              />
            </div>

            <div class="re-field re-field--full">
              <label class="ui-label">项目描述</label>
              <RichTextEditor
                class="re-rich"
                :model-value="entry.description"
                placeholder="请输入项目描述..."
                min-height="160px"
                @update:model-value="updateEntry(index, 'description', $event)"
              />
            </div>

            <div class="re-field re-field--full">
              <label class="ui-label" :for="`prj-tech-${index}`">技术栈</label>
              <input
                :id="`prj-tech-${index}`"
                class="ui-input"
                type="text"
                :value="entry.techStack"
                placeholder="如：Vue3, TypeScript, Node.js"
                @input="updateEntry(index, 'techStack', $event.target.value)"
              />
            </div>
          </div>
        </article>

        <button type="button" class="re-add" @click="addEntry">+ 添加一条</button>
      </div>
    </div>

    <div v-else class="re-summary">
      <div v-for="(entry, index) in modelValue" :key="index" class="re-summary__item">
        {{ entry.name || '项目名称' }} · {{ entry.role || '角色' }}
      </div>
      <div v-if="!modelValue || modelValue.length === 0" class="re-summary__muted">暂无项目经历</div>
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
    name: '',
    role: '',
    startDate: '',
    endDate: '',
    description: '',
    techStack: '',
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
  name: '请填写项目名称',
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
