<template>
  <div class="re-editor">
    <div v-if="isActive">
      <p v-if="missingCount > 0" class="re-notice">
        还有 {{ missingCount }} 个必填项未填写，标记 <span class="re-required">*</span> 的字段需要补充。
      </p>

      <div class="re-form">
        <div class="re-field">
          <label class="ui-label" for="intent-position">期望职位 <span class="re-required">*</span></label>
          <input
            id="intent-position"
            class="ui-input"
            :class="{ 're-input--invalid': fieldErrors.desiredPosition }"
            type="text"
            :value="modelValue.desiredPosition"
            placeholder="如：Java 后端开发工程师"
            @input="update('desiredPosition', $event.target.value)"
            @blur="touch('desiredPosition')"
          />
          <p v-if="fieldErrors.desiredPosition" class="re-error">{{ fieldErrors.desiredPosition }}</p>
        </div>

        <div class="re-field">
          <label class="ui-label" for="intent-city">期望城市</label>
          <input
            id="intent-city"
            class="ui-input"
            type="text"
            :value="modelValue.desiredCity"
            placeholder="如：北京"
            @input="update('desiredCity', $event.target.value)"
          />
        </div>

        <div class="re-field">
          <label class="ui-label" for="intent-salary">期望薪资</label>
          <input
            id="intent-salary"
            class="ui-input"
            type="text"
            :value="modelValue.salaryRange"
            placeholder="如：20K-30K"
            @input="update('salaryRange', $event.target.value)"
          />
        </div>

        <div class="re-field">
          <label class="ui-label" for="intent-type">工作性质</label>
          <select
            id="intent-type"
            class="ui-input ui-select"
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

        <div class="re-field">
          <label class="ui-label" for="intent-status">求职状态</label>
          <select
            id="intent-status"
            class="ui-input ui-select"
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
    </div>

    <div v-else class="re-summary">
      <span class="re-summary__primary">{{ modelValue.desiredPosition || '期望职位' }}</span>
      <div class="re-summary__tags">
        <span v-if="modelValue.desiredCity" class="ui-badge">{{ modelValue.desiredCity }}</span>
        <span v-if="modelValue.salaryRange" class="ui-badge">{{ modelValue.salaryRange }}</span>
        <span v-if="modelValue.employmentType" class="ui-badge">{{ modelValue.employmentType }}</span>
        <span v-if="modelValue.jobStatus" class="ui-badge">{{ modelValue.jobStatus }}</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, reactive } from 'vue'

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

// ── 必填校验（失焦后提示）───────────────────────────────────────────────
const touched = reactive({})

function touch(field) {
  touched[field] = true
}

const fieldErrors = computed(() => {
  const errors = {}
  if (touched.desiredPosition && !String(props.modelValue?.desiredPosition || '').trim()) {
    errors.desiredPosition = '请填写期望职位，HR 会据此匹配岗位'
  }
  return errors
})

const missingCount = computed(() =>
  String(props.modelValue?.desiredPosition || '').trim() ? 0 : 1
)
</script>
