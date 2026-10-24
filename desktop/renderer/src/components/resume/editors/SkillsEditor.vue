<template>
  <div class="skills-editor">
    <div v-if="isActive">
      <div
        v-for="(entry, index) in localEntries()"
        :key="index"
        class="entry-card"
      >
        <div class="entry-row">
          <div class="field field-name">
            <input
              class="field-input"
              type="text"
              :value="entry.name"
              placeholder="请输入技能名称"
              @input="updateEntry(index, 'name', $event.target.value)"
            />
          </div>
          <div class="field field-proficiency">
            <select
              class="field-input"
              :value="entry.proficiency"
              @change="updateEntry(index, 'proficiency', $event.target.value)"
            >
              <option value="">熟练度</option>
              <option value="精通">精通</option>
              <option value="掌握">掌握</option>
              <option value="熟悉">熟悉</option>
              <option value="了解">了解</option>
            </select>
          </div>
          <button class="delete-btn" @click="removeEntry(index)">删除</button>
        </div>
      </div>

      <button class="add-btn" @click="addEntry">+ 添加一项</button>
    </div>

    <div v-else class="summary">
      <div v-if="modelValue && modelValue.length > 0" class="chips">
        <span
          v-for="(skill, index) in modelValue"
          :key="index"
          class="chip"
        >
          {{ skill.name || '技能' }}
          <span v-if="skill.proficiency" class="chip-level">{{ skill.proficiency }}</span>
        </span>
      </div>
      <div v-else class="summary-empty">暂无技能特长</div>
    </div>
  </div>
</template>

<script setup>

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
</script>

<style scoped>
.skills-editor {
  padding: 16px 0;
}

.entry-card {
  margin-bottom: 8px;
}

.entry-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.field {
  display: flex;
  flex-direction: column;
}

.field-name {
  flex: 2;
}

.field-proficiency {
  flex: 1;
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
  border: 1.5px dashed #22c55e;
  border-radius: 6px;
  background: none;
  color: #22c55e;
}

.delete-btn {
  font-size: 12px;
  color: #ef4444;
  background: none;
  border: none;
  cursor: pointer;
  flex-shrink: 0;
  padding: 0 4px;
}

.delete-btn:hover {
  text-decoration: underline;
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

.chips {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.chip {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  background: #dcfce7;
  color: #22c55e;
  padding: 4px 10px;
  border-radius: 20px;
}

.chip-level {
  font-size: 11px;
  opacity: 0.75;
}

.summary-empty {
  font-size: 13px;
  color: #9ca3af;
}
</style>
