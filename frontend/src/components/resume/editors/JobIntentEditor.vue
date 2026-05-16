<template>
  <div class="job-intent-editor">
    <div v-if="isActive" class="form-grid">
      <div class="field">
        <label class="field-label">期望职位</label>
        <input
          class="field-input"
          type="text"
          :value="modelValue.desiredPosition"
          placeholder="请输入期望职位"
          @input="update('desiredPosition', $event.target.value)"
        />
      </div>
      <div class="field">
        <label class="field-label">期望城市</label>
        <input
          class="field-input"
          type="text"
          :value="modelValue.desiredCity"
          placeholder="请输入期望城市"
          @input="update('desiredCity', $event.target.value)"
        />
      </div>
      <div class="field">
        <label class="field-label">期望薪资</label>
        <input
          class="field-input"
          type="text"
          :value="modelValue.salaryRange"
          placeholder="如：20K-30K"
          @input="update('salaryRange', $event.target.value)"
        />
      </div>
      <div class="field">
        <label class="field-label">工作性质</label>
        <select
          class="field-input"
          :value="modelValue.employmentType"
          @change="update('employmentType', $event.target.value)"
        >
          <option value="">请选择</option>
          <option value="全职">全职</option>
          <option value="兼职">兼职</option>
          <option value="实习">实习</option>
          <option value="自由职业">自由职业</option>
        </select>
      </div>
      <div class="field">
        <label class="field-label">求职状态</label>
        <select
          class="field-input"
          :value="modelValue.jobStatus"
          @change="update('jobStatus', $event.target.value)"
        >
          <option value="">请选择</option>
          <option value="在职">在职</option>
          <option value="离职">离职</option>
          <option value="应届">应届</option>
          <option value="随时可入职">随时可入职</option>
        </select>
      </div>
    </div>

    <div v-else class="summary">
      <span class="summary-primary">{{ modelValue.desiredPosition || '期望职位' }}</span>
      <div class="summary-tags">
        <span v-if="modelValue.desiredCity" class="tag">{{ modelValue.desiredCity }}</span>
        <span v-if="modelValue.salaryRange" class="tag">{{ modelValue.salaryRange }}</span>
        <span v-if="modelValue.employmentType" class="tag">{{ modelValue.employmentType }}</span>
        <span v-if="modelValue.jobStatus" class="tag">{{ modelValue.jobStatus }}</span>
      </div>
    </div>
  </div>
</template>

<script setup>
const props = defineProps({
  modelValue: {
    type: Object,
    default: () => ({
      desiredPosition: '',
      desiredCity: '',
      salaryRange: '',
      employmentType: '',
      jobStatus: '',
    }),
  },
  isActive: {
    type: Boolean,
    default: false,
  },
})

const emit = defineEmits(['update:modelValue'])

function update(field, value) {
  emit('update:modelValue', { ...props.modelValue, [field]: value })
}
</script>

<style scoped>
.job-intent-editor {
  padding: 16px 0;
}

.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px 16px;
}

.field {
  display: flex;
  flex-direction: column;
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

.summary {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.summary-primary {
  font-size: 15px;
  font-weight: 600;
  color: #1a1a2e;
}

.summary-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.tag {
  font-size: 12px;
  background: #dcfce7;
  color: #22c55e;
  padding: 2px 8px;
  border-radius: 4px;
}
</style>
