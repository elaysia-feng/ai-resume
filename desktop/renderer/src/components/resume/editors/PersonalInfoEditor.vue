<template>
  <div class="re-editor">
    <!-- 头像 -->
    <div class="re-avatar">
      <div class="re-avatar__preview" title="点击上传头像" @click="triggerUpload">
        <img v-if="avatarPreviewUrl" :src="avatarPreviewUrl" class="re-avatar__img" alt="简历头像" />
        <AppIcon v-else name="user" :size="24" />
      </div>
      <div class="re-avatar__actions">
        <button type="button" class="ui-btn ui-btn--sm ui-btn--outline" :disabled="avatarUploading" @click="triggerUpload">
          <AppIcon name="upload-simple" :size="16" />
          {{ avatarUploading ? '上传中...' : '上传头像' }}
        </button>
        <button
          v-if="modelValue.avatar"
          type="button"
          class="ui-btn ui-btn--sm ui-btn--ghost"
          :disabled="avatarUploading"
          @click="removeAvatar"
        >
          移除头像
        </button>
        <p
          v-if="avatarMessage"
          class="ui-callout re-avatar__message"
          :class="avatarMessageType === 'error' ? 'ui-callout--danger' : 'ui-callout--accent'"
        >
          <AppIcon :name="avatarMessageType === 'error' ? 'warning-circle' : 'check-circle'" :size="16" />
          <span>{{ avatarMessage }}</span>
        </p>
      </div>
      <input ref="fileInput" type="file" accept="image/*" class="re-avatar__input" @change="onFileChange" />
    </div>

    <!-- 编辑态 -->
    <div v-if="isActive">
      <p v-if="missingCount > 0" class="re-notice">
        还有 {{ missingCount }} 个必填项未填写，标记 <span class="re-required">*</span> 的字段需要补充。
      </p>

      <div class="re-form">
        <div class="re-field">
          <label class="ui-label" for="basic-name">姓名 <span class="re-required">*</span></label>
          <input
            id="basic-name"
            class="ui-input"
            :class="{ 're-input--invalid': fieldErrors.name }"
            type="text"
            :value="modelValue.name"
            placeholder="请输入姓名"
            @input="update('name', $event.target.value)"
            @blur="touch('name')"
          />
          <p v-if="fieldErrors.name" class="re-error">{{ fieldErrors.name }}</p>
        </div>

        <div class="re-field">
          <label class="ui-label" for="basic-title">职位</label>
          <input
            id="basic-title"
            class="ui-input"
            type="text"
            :value="modelValue.title"
            placeholder="如：Java 后端开发工程师"
            @input="update('title', $event.target.value)"
          />
        </div>

        <div class="re-field">
          <label class="ui-label" for="basic-email">邮箱 <span class="re-required">*</span></label>
          <input
            id="basic-email"
            class="ui-input"
            :class="{ 're-input--invalid': fieldErrors.email }"
            type="email"
            :value="modelValue.email"
            placeholder="如：zhangsan@example.com"
            @input="update('email', $event.target.value)"
            @blur="touch('email')"
          />
          <p v-if="fieldErrors.email" class="re-error">{{ fieldErrors.email }}</p>
        </div>

        <div class="re-field">
          <label class="ui-label" for="basic-phone">电话 <span class="re-required">*</span></label>
          <input
            id="basic-phone"
            class="ui-input"
            :class="{ 're-input--invalid': fieldErrors.phone }"
            type="tel"
            :value="modelValue.phone"
            placeholder="如：138-0013-8000"
            @input="update('phone', $event.target.value)"
            @blur="touch('phone')"
          />
          <p v-if="fieldErrors.phone" class="re-error">{{ fieldErrors.phone }}</p>
        </div>

        <div class="re-field">
          <label class="ui-label" for="basic-location">所在城市</label>
          <input
            id="basic-location"
            class="ui-input"
            type="text"
            :value="modelValue.location"
            placeholder="如：北京市朝阳区"
            @input="update('location', $event.target.value)"
          />
        </div>

        <div class="re-field">
          <label class="ui-label" for="basic-wechat">微信</label>
          <input
            id="basic-wechat"
            class="ui-input"
            type="text"
            :value="modelValue.wechat"
            placeholder="如：zhangsan_dev"
            @input="update('wechat', $event.target.value)"
          />
        </div>

        <div class="re-field">
          <label class="ui-label" for="basic-github">GitHub</label>
          <input
            id="basic-github"
            class="ui-input"
            type="text"
            :value="modelValue.github"
            placeholder="如：github.com/zhangsan"
            @input="update('github', $event.target.value)"
          />
        </div>

        <div class="re-field">
          <label class="ui-label" for="basic-website">个人网站</label>
          <input
            id="basic-website"
            class="ui-input"
            type="text"
            :value="modelValue.website"
            placeholder="如：zhangsan.com"
            @input="update('website', $event.target.value)"
          />
        </div>
      </div>
    </div>

    <!-- 折叠态摘要 -->
    <div v-else class="re-summary">
      <span class="re-summary__primary">{{ modelValue.name || '姓名' }}</span>
      <span class="re-summary__muted">{{ modelValue.title || '职位' }}</span>
      <div class="re-summary__tags">
        <span v-if="modelValue.email" class="ui-badge">{{ modelValue.email }}</span>
        <span v-if="modelValue.phone" class="ui-badge">{{ modelValue.phone }}</span>
        <span v-if="modelValue.location" class="ui-badge">{{ modelValue.location }}</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import AppIcon from '../../common/AppIcon.vue'
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

// ── 必填校验（失焦后提示）───────────────────────────────────────────────
const touched = reactive({})

function touch(field) {
  touched[field] = true
}

const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

const fieldErrors = computed(() => {
  const errors = {}
  if (touched.name && !String(props.modelValue?.name || '').trim()) {
    errors.name = '请填写姓名，简历顶部需要展示'
  }
  if (touched.phone && !String(props.modelValue?.phone || '').trim()) {
    errors.phone = '请填写联系电话，HR 通常通过电话沟通'
  }
  if (touched.email) {
    const email = String(props.modelValue?.email || '').trim()
    if (!email) {
      errors.email = '请填写邮箱，招聘方通常通过邮箱联系你'
    } else if (!EMAIL_PATTERN.test(email)) {
      errors.email = '邮箱格式不正确，例如 name@example.com'
    }
  }
  return errors
})

const missingCount = computed(() => {
  const value = props.modelValue || {}
  const required = ['name', 'email', 'phone']
  return required.filter((field) => !String(value[field] || '').trim()).length
})

const fileInput = ref(null)
const avatarUploading = ref(false)
const avatarMessage = ref('')
const avatarMessageType = ref('')
const avatarPreviewUrl = computed(() => resolveApiAssetUrl(props.modelValue.avatar))

function triggerUpload() {
  fileInput.value?.click()
}

async function onFileChange(e) {
  const file = e.target.files?.[0]
  e.target.value = ''

  if (!file || !file.type.startsWith('image/')) {
    avatarMessage.value = '请选择图片文件'
    avatarMessageType.value = 'error'
    return
  }

  if (!props.resumeId) {
    avatarMessage.value = '简历未初始化，暂时不能上传头像'
    avatarMessageType.value = 'error'
    return
  }

  avatarUploading.value = true
  avatarMessage.value = ''

  try {
    const response = await uploadResumeAvatar(props.resumeId, file)
    update('avatar', response?.avatarUrl || response?.avatar || '')
    avatarMessage.value = '头像上传成功'
    avatarMessageType.value = 'success'
  } catch (error) {
    avatarMessage.value = extractApiMessage(error, '头像上传失败')
    avatarMessageType.value = 'error'
  } finally {
    avatarUploading.value = false
  }
}

function removeAvatar() {
  update('avatar', '')
  avatarMessage.value = ''
  avatarMessageType.value = ''
}
</script>
