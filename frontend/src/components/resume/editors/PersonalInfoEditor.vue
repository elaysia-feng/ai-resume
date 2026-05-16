<template>
  <div class="personal-info-editor">
    <!-- Avatar upload -->
    <div class="avatar-upload">
      <div class="avatar-preview" @click="triggerUpload">
        <img v-if="avatarPreviewUrl" :src="avatarPreviewUrl" class="avatar-img" />
        <div v-else class="avatar-placeholder">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none">
            <path d="M12 4a4 4 0 100 8 4 4 0 000-8zM6 20c0-3.3 2.7-6 6-6s6 2.7 6 6" stroke="#9ca3af" stroke-width="1.5" stroke-linecap="round"/>
          </svg>
        </div>
      </div>
      <div class="avatar-actions">
        <button class="avatar-btn" :disabled="avatarUploading" @click="triggerUpload">
          {{ avatarUploading ? '上传中...' : '上传头像' }}
        </button>
        <button v-if="modelValue.avatar" class="avatar-remove" :disabled="avatarUploading" @click="removeAvatar">移除</button>
        <span v-if="avatarMessage" class="avatar-message">{{ avatarMessage }}</span>
      </div>
      <input ref="fileInput" type="file" accept="image/*" class="hidden-input" @change="onFileChange" />
    </div>

    <!-- Active (edit) mode -->
    <div v-if="isActive" class="form-grid">
      <div class="field">
        <label class="field-label">姓名</label>
        <input
          class="field-input"
          type="text"
          :value="modelValue.name"
          placeholder="请输入姓名"
          @input="update('name', $event.target.value)"
        />
      </div>
      <div class="field">
        <label class="field-label">职位</label>
        <input
          class="field-input"
          type="text"
          :value="modelValue.title"
          placeholder="请输入职位"
          @input="update('title', $event.target.value)"
        />
      </div>
      <div class="field">
        <label class="field-label">邮箱</label>
        <input
          class="field-input"
          type="email"
          :value="modelValue.email"
          placeholder="请输入邮箱"
          @input="update('email', $event.target.value)"
        />
      </div>
      <div class="field">
        <label class="field-label">电话</label>
        <input
          class="field-input"
          type="tel"
          :value="modelValue.phone"
          placeholder="请输入电话"
          @input="update('phone', $event.target.value)"
        />
      </div>
      <div class="field">
        <label class="field-label">所在城市</label>
        <input
          class="field-input"
          type="text"
          :value="modelValue.location"
          placeholder="请输入所在城市"
          @input="update('location', $event.target.value)"
        />
      </div>
      <div class="field">
        <label class="field-label">微信</label>
        <input
          class="field-input"
          type="text"
          :value="modelValue.wechat"
          placeholder="请输入微信号"
          @input="update('wechat', $event.target.value)"
        />
      </div>
      <div class="field">
        <label class="field-label">GitHub</label>
        <input
          class="field-input"
          type="text"
          :value="modelValue.github"
          placeholder="请输入 GitHub 地址"
          @input="update('github', $event.target.value)"
        />
      </div>
      <div class="field">
        <label class="field-label">个人网站</label>
        <input
          class="field-input"
          type="text"
          :value="modelValue.website"
          placeholder="请输入个人网站"
          @input="update('website', $event.target.value)"
        />
      </div>
    </div>

    <!-- Collapsed (view) mode -->
    <div v-else class="summary">
      <span class="summary-name">{{ modelValue.name || '姓名' }}</span>
      <span class="summary-title">{{ modelValue.title || '职位' }}</span>
      <div class="summary-contacts">
        <span v-if="modelValue.email">{{ modelValue.email }}</span>
        <span v-if="modelValue.phone" class="separator">|</span>
        <span v-if="modelValue.phone">{{ modelValue.phone }}</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { uploadResumeAvatar } from '../../../api/resumes.js'
import { extractApiMessage, resolveApiAssetUrl } from '../../../api/request.js'

const props = defineProps({
  modelValue: {
    type: Object,
    default: () => ({
      name: '',
      title: '',
      email: '',
      phone: '',
      location: '',
      wechat: '',
      github: '',
      website: '',
      avatar: '',
    }),
  },
  isActive: {
    type: Boolean,
    default: false,
  },
  resumeId: {
    type: Number,
    default: null,
  },
})

const emit = defineEmits(['update:modelValue'])

function update(field, value) {
  emit('update:modelValue', { ...props.modelValue, [field]: value })
}

const fileInput = ref(null)
const avatarUploading = ref(false)
const avatarMessage = ref('')
const avatarPreviewUrl = computed(() => resolveApiAssetUrl(props.modelValue.avatar))

function triggerUpload() {
  fileInput.value?.click()
}

async function onFileChange(e) {
  const file = e.target.files?.[0]
  e.target.value = ''

  if (!file || !file.type.startsWith('image/')) {
    avatarMessage.value = '请选择图片文件'
    return
  }

  if (!props.resumeId) {
    avatarMessage.value = '简历未初始化，暂时不能上传头像'
    return
  }

  avatarUploading.value = true
  avatarMessage.value = ''

  try {
    const response = await uploadResumeAvatar(props.resumeId, file)
    update('avatar', response?.avatarUrl || response?.avatar || '')
    avatarMessage.value = '头像上传成功'
  } catch (error) {
    avatarMessage.value = extractApiMessage(error, '头像上传失败')
  } finally {
    avatarUploading.value = false
  }
}

function removeAvatar() {
  update('avatar', '')
}
</script>

<style scoped>
.personal-info-editor {
  padding: 16px 0;
}

/* Avatar */
.avatar-upload {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 16px;
  padding-bottom: 16px;
  border-bottom: 1px solid #f0f0f0;
}

.avatar-preview {
  width: auto;
  height: auto;
  max-width: 140px;
  max-height: 140px;
  border-radius: 8px;
  overflow: hidden;
  border: 2px dashed #d1d5db;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f9fafb;
  transition: border-color 0.15s;
  flex-shrink: 0;
}

.avatar-preview:hover {
  border-color: #22c55e;
}

.avatar-img {
  width: auto;
  height: auto;
  max-width: 100%;
  max-height: 140px;
  object-fit: contain;
  display: block;
}

.avatar-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
}

.avatar-actions {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.avatar-btn {
  padding: 6px 14px;
  border: 1.5px solid #22c55e;
  border-radius: 6px;
  background: transparent;
  color: #22c55e;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: background 0.15s, color 0.15s;
}

.avatar-btn:hover {
  background: #22c55e;
  color: #fff;
}

.avatar-remove {
  padding: 6px 14px;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  background: transparent;
  color: #9ca3af;
  font-size: 13px;
  cursor: pointer;
  transition: background 0.15s, color 0.15s;
}

.avatar-remove:hover {
  background: #fef2f2;
  color: #ef4444;
  border-color: #fca5a5;
}

.avatar-btn:disabled,
.avatar-remove:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.avatar-message {
  font-size: 12px;
  color: #6b7280;
  line-height: 1.5;
  max-width: 220px;
}

.hidden-input {
  display: none;
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
}

.field-input:focus {
  border-color: #22c55e;
}

.summary {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.summary-name {
  font-size: 16px;
  font-weight: 700;
  color: #1a1a2e;
}

.summary-title {
  font-size: 14px;
  color: #22c55e;
  font-weight: 500;
}

.summary-contacts {
  font-size: 13px;
  color: #6b7280;
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.separator {
  color: #d1d5db;
}
</style>
