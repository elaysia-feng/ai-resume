<template>
  <div class="re-editor">
    <div v-if="isActive">
      <p v-if="missingCount > 0" class="re-notice">
        还有 {{ missingCount }} 个必填项未填写，标记 <span class="re-required">*</span> 的字段需要补充。
      </p>

      <div v-if="!localEntries().length" class="ui-empty">
        <div class="ui-empty__icon">
          <AppIcon name="medal" :size="24" />
        </div>
        <div class="ui-empty__title">还没有荣誉证书</div>
        <div class="ui-empty__hint">证书、奖学金、竞赛获奖都可以列在这里，写清颁发机构更有说服力。</div>
        <button type="button" class="ui-btn ui-btn--sm ui-btn--primary" @click="addEntry"> <AppIcon name="plus" :size="16" /> 添加一条</button>
      </div>

      <div v-else class="re-list">
        <article v-for="(entry, index) in localEntries()" :key="index" class="ui-panel re-entry">
          <header class="re-entry__head">
            <span class="re-entry__title">
              <span class="re-entry__index">{{ index + 1 }}</span>
              证书 {{ index + 1 }}
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
              <label class="ui-label" :for="`cert-name-${index}`">证书名称 <span class="re-required">*</span></label>
              <input
                :id="`cert-name-${index}`"
                class="ui-input"
                :class="{ 're-input--invalid': entryErrors[`${index}.name`] }"
                type="text"
                :value="entry.name"
                placeholder="如：Oracle Certified Professional"
                @input="updateEntry(index, 'name', $event.target.value)"
                @blur="touch(`${index}.name`)"
              />
              <p v-if="entryErrors[`${index}.name`]" class="re-error">{{ entryErrors[`${index}.name`] }}</p>
            </div>

            <div class="re-field">
              <label class="ui-label" :for="`cert-level-${index}`">奖项等级</label>
              <select
                :id="`cert-level-${index}`"
                class="ui-input ui-select"
                :value="entry.level"
                @change="updateEntry(index, 'level', $event.target.value)"
              >
                <option value="">请选择</option>
                <option value="一等奖">一等奖</option>
                <option value="二等奖">二等奖</option>
                <option value="三等奖">三等奖</option>
                <option value="其他">其他</option>
              </select>
            </div>

            <div class="re-field">
              <label class="ui-label" :for="`cert-date-${index}`">获得时间</label>
              <input
                :id="`cert-date-${index}`"
                class="ui-input"
                type="text"
                :value="entry.date"
                placeholder="如：2023.06"
                @input="updateEntry(index, 'date', $event.target.value)"
              />
            </div>

            <div class="re-field">
              <label class="ui-label" :for="`cert-issuer-${index}`">颁发机构</label>
              <input
                :id="`cert-issuer-${index}`"
                class="ui-input"
                type="text"
                :value="entry.issuer"
                placeholder="如：中国计算机学会"
                @input="updateEntry(index, 'issuer', $event.target.value)"
              />
            </div>

            <div class="re-field re-field--full">
              <label class="ui-label">描述</label>
              <RichTextEditor
                class="re-rich"
                :model-value="entry.description"
                placeholder="请输入证书描述..."
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
        {{ entry.name || '证书名称' }} · {{ entry.date || '时间' }}
      </div>
      <div v-if="!modelValue || modelValue.length === 0" class="re-summary__muted">暂无荣誉证书</div>
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
    name: '',
    level: '',
    date: '',
    issuer: '',
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
  name: '请填写证书名称',
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
