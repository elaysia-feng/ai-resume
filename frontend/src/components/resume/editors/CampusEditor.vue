<template>
  <div class="campus-editor">
    <div v-if="isActive">
      <div
        v-for="(entry, index) in localEntries()"
        :key="index"
        class="entry-card"
      >
        <div class="entry-header">
          <span class="entry-title">校园经历 {{ index + 1 }}</span>
          <button class="delete-btn" @click="removeEntry(index)">删除</button>
        </div>

        <div class="form-grid">
          <div class="field">
            <label class="field-label">组织/社团</label>
            <input
              class="field-input"
              type="text"
              :value="entry.organization"
              placeholder="请输入组织或社团名称"
              @input="updateEntry(index, 'organization', $event.target.value)"
            />
          </div>
          <div class="field">
            <label class="field-label">担任职务</label>
            <input
              class="field-input"
              type="text"
              :value="entry.role"
              placeholder="请输入担任的职务"
              @input="updateEntry(index, 'role', $event.target.value)"
            />
          </div>
          <div class="field">
            <label class="field-label">开始时间</label>
            <input
              class="field-input"
              type="text"
              :value="entry.startDate"
              placeholder="如：2020.09"
              @input="updateEntry(index, 'startDate', $event.target.value)"
            />
          </div>
          <div class="field">
            <label class="field-label">结束时间</label>
            <input
              class="field-input"
              type="text"
              :value="entry.endDate"
              placeholder="如：2022.06"
              @input="updateEntry(index, 'endDate', $event.target.value)"
            />
          </div>
        </div>

        <div class="field field-full">
          <label class="field-label">经历描述</label>
          <RichTextEditor
            class="field-rich-editor"
            :model-value="entry.description"
            placeholder="请输入经历描述..."
            min-height="160px"
            @update:model-value="updateEntry(index, 'description', $event)"
          />
        </div>
      </div>

      <button class="add-btn" @click="addEntry">+ 添加一条</button>
    </div>

    <div v-else class="summary-list">
      <div v-for="(entry, index) in modelValue" :key="index" class="summary-item">
        <span class="summary-role">{{ entry.role || '职务' }}</span>
        <span class="summary-at"> @ </span>
        <span class="summary-org">{{ entry.organization || '组织名称' }}</span>
      </div>
      <div v-if="!modelValue || modelValue.length === 0" class="summary-empty">
        暂无校园经历
      </div>
    </div>
  </div>
</template>

<script setup>
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
</script>

<style scoped>
.campus-editor {
  padding: 16px 0;
}

.entry-card {
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 16px;
  margin-bottom: 12px;
}

.entry-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.entry-title {
  font-size: 13px;
  font-weight: 600;
  color: #374151;
}

.delete-btn {
  font-size: 12px;
  color: #ef4444;
  background: none;
  border: none;
  cursor: pointer;
  padding: 0;
}

.delete-btn:hover {
  text-decoration: underline;
}

.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px 16px;
  margin-bottom: 12px;
}

.field {
  display: flex;
  flex-direction: column;
}

.field-full {
  width: 100%;
}

.field-label {
  font-size: 12px;
  font-weight: 600;
  color: #374151;
  margin-bottom: 4px;
}

.field-input {
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  padding: 8px 12px;
  font-size: 14px;
  width: 100%;
  box-sizing: border-box;
  outline: none;
  transition: border-color 0.15s;
  background-color: #fff;
}

.field-input:focus {
  border-color: #22c55e;
}

.field-rich-editor {
  width: 100%;
}

.add-btn {
  width: 100%;
  padding: 8px;
  border: 1.5px dashed #22c55e;
  border-radius: 6px;
  background: none;
  color: #22c55e;
  font-size: 14px;
  cursor: pointer;
  transition: background 0.15s;
}

.add-btn:hover {
  background: #dcfce7;
}

.summary-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.summary-item {
  font-size: 14px;
  color: #1a1a2e;
}

.summary-role {
  font-weight: 600;
}

.summary-at {
  color: #9ca3af;
}

.summary-empty {
  font-size: 13px;
  color: #9ca3af;
}
</style>
