<template>
  <div class="welcome">
    <main class="welcome__inner ui-panel">
      <header class="welcome__head">
        <span class="welcome__mark ui-icon-tile ui-icon-tile--lg" aria-hidden="true">
          <AppIcon name="sparkle" :size="20" />
        </span>
        <h1 class="ui-page-title">创建本地身份</h1>
        <p class="ui-page-subtitle">
          填一个昵称就能开始用。昵称和头像属于「账号身份」，会显示在侧边栏和个人档案里；
          简历中的姓名与照片仍要在每份简历里单独填写，两者互不影响。
        </p>
      </header>

      <form class="welcome__form" novalidate @submit.prevent="handleSubmit">
        <!-- 账号头像 -->
        <section class="welcome__section">
          <div class="ui-section-head">
            <h2 class="ui-section-head__title">账号头像</h2>
            <p class="ui-section-head__hint">可选</p>
          </div>

          <div class="welcome__avatar">
            <span class="avatar-shell" aria-hidden="true">
              <img v-if="avatarPreviewUrl" :src="avatarPreviewUrl" alt="" class="avatar-image" />
              <span v-else class="avatar-text">{{ nicknameInitial }}</span>
            </span>
            <div class="avatar-actions">
              <div class="avatar-actions__row">
                <button type="button" class="ui-btn ui-btn--outline ui-btn--sm" :disabled="submitting" @click="handlePickAvatar">
                  <AppIcon name="image" :size="16" />
                  {{ avatarFile ? '重新选择头像' : '选择头像' }}
                </button>
                <button v-if="avatarFile" type="button" class="ui-btn ui-btn--ghost ui-btn--sm" :disabled="submitting" @click="removeAvatar">
                  <AppIcon name="x" :size="16" />
                  移除
                </button>
              </div>
              <p class="ui-field__hint">{{ avatarTip }}</p>
            </div>
            <input ref="avatarInputRef" type="file" accept="image/*" class="hidden-input" @change="handleAvatarInputChange" />
          </div>
        </section>

        <!-- 昵称 -->
        <div class="ui-field">
          <label class="ui-label" for="localDisplayName">昵称</label>
          <input
            id="localDisplayName"
            v-model="displayName"
            type="text"
            class="ui-input"
            maxlength="32"
            placeholder="例如：小王"
            autocomplete="nickname"
            :disabled="submitting"
          />
          <p class="ui-field__hint">1-32 个字符，用作应用内的展示名。</p>
        </div>

        <!-- 失败提示 -->
        <div v-if="errorMsg" class="ui-callout ui-callout--danger" role="alert">
          <AppIcon name="warning-circle" :size="16" />
          <span>{{ errorMsg }}</span>
        </div>

        <!-- 成功提示 -->
        <div v-if="successMsg" class="ui-callout ui-callout--accent" role="status">
          <AppIcon name="check-circle" :size="16" />
          <span>{{ successMsg }}</span>
        </div>

        <button type="submit" class="ui-btn ui-btn--primary ui-btn--lg ui-btn--block" :disabled="submitting">
          <AppIcon v-if="submitting" name="spinner-gap" :size="16" spin />
          <span>{{ submitting ? '正在创建…' : '进入应用' }}</span>
        </button>

        <div class="ui-callout">
          <AppIcon name="desktop" :size="16" />
          <span>
            本地身份只保存在这台电脑上，不绑定邮箱、不需要密码；
            下次打开应用会直接进入工作台。
          </span>
        </div>
      </form>
    </main>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, ref } from 'vue';
import { useRouter } from 'vue-router';
import { createLocalProfile } from '../api/localProfile.js';
import { extractApiMessage } from '../api/request.js';
import { useAuthStore } from '../store/authStore.js';
import AppIcon from '../components/common/AppIcon.vue';

const router = useRouter();
const authStore = useAuthStore();

const displayName = ref('');
const avatarFile = ref(null);
const avatarPreviewUrl = ref('');
const avatarTip = ref('可选。这张图只用于展示应用内的账号身份，不是简历里的照片。');
const submitting = ref(false);
const errorMsg = ref('');
const successMsg = ref('');
const avatarInputRef = ref(null);
// 桌面端用原生选择框选中了文件、但渲染层读不到字节时置位，下一次点击改走系统文件框
const needSystemFileBox = ref(false);

// Electron 环境才有原生文件选择框
const canPickFiles = computed(
  () => typeof window !== 'undefined' && typeof window.desktop?.pickFiles === 'function'
);

const nicknameInitial = computed(() => (displayName.value.trim() || '本').charAt(0).toUpperCase());

function fileNameOf(path) {
  const parts = String(path).split(/[\\/]/);
  return parts[parts.length - 1] || String(path);
}

/**
 * 把本地路径读成图片文件。
 *
 * <p>渲染层默认没有读本地文件的权限（Electron 沙箱 + webSecurity），
 * 这里只做尽力而为：读得到就用，读不到就退回系统文件框。</p>
 */
async function readLocalImageFile(path) {
  try {
    const url = path.startsWith('file://') ? path : `file:///${String(path).replace(/\\/g, '/')}`;
    const response = await fetch(url);
    if (!response.ok) {
      return null;
    }
    const blob = await response.blob();
    if (!blob.type.startsWith('image/')) {
      return null;
    }
    return new File([blob], fileNameOf(path), { type: blob.type });
  } catch {
    return null;
  }
}

function applyAvatarFile(file) {
  if (avatarPreviewUrl.value) {
    URL.revokeObjectURL(avatarPreviewUrl.value);
  }
  avatarFile.value = file;
  avatarPreviewUrl.value = URL.createObjectURL(file);
  avatarTip.value = `已选择头像：${file.name}。`;
}

function removeAvatar() {
  if (avatarPreviewUrl.value) {
    URL.revokeObjectURL(avatarPreviewUrl.value);
  }
  avatarFile.value = null;
  avatarPreviewUrl.value = '';
  avatarTip.value = '可选。这张图只用于展示应用内的账号身份，不是简历里的照片。';
}

async function handlePickAvatar() {
  errorMsg.value = '';

  if (needSystemFileBox.value) {
    // 上一轮用原生选择框选中了文件，但读不到字节：这次改走系统文件框
    needSystemFileBox.value = false;
    avatarInputRef.value?.click();
    return;
  }

  if (!canPickFiles.value) {
    avatarInputRef.value?.click();
    return;
  }

  let pickedPaths = [];
  try {
    const result = await window.desktop.pickFiles();
    pickedPaths = result?.paths ?? [];
  } catch {
    pickedPaths = [];
  }

  const path = pickedPaths[0];
  if (!path) {
    return;
  }

  const file = await readLocalImageFile(path);
  if (file) {
    applyAvatarFile(file);
    return;
  }

  needSystemFileBox.value = true;
  avatarTip.value = `已选中「${fileNameOf(path)}」。桌面端读不到本地图片字节，请再点一次「选择头像」，改用系统文件框上传这张图片。`;
}

function handleAvatarInputChange(event) {
  const file = event.target.files?.[0];
  event.target.value = '';

  if (!file) {
    return;
  }
  if (!file.type.startsWith('image/')) {
    avatarTip.value = '请选择图片文件（png / jpg / webp 等）。';
    return;
  }

  applyAvatarFile(file);
}

/** 把接口报错翻译成「发生了什么 + 怎么办」 */
function describeError(error) {
  const status = error?.response?.status;
  const raw = extractApiMessage(error, '');

  if (status === 400) {
    return `没能创建本地身份：${raw || '昵称不合法'}。请确认昵称是 1-32 个字符后重试。`;
  }
  if (/network|timeout|failed to fetch|ECONNREFUSED|网络/i.test(raw)) {
    return `连不上本地服务${raw ? `：${raw}` : ''}。请确认后端服务（:8080 与 :8000）已启动后重试。`;
  }
  return raw || '创建本地身份失败，请稍后重试。';
}

async function handleSubmit() {
  errorMsg.value = '';
  successMsg.value = '';

  const nickname = displayName.value.trim();
  if (!nickname) {
    errorMsg.value = '还没填昵称。请先填写一个在应用内展示的名字，再点击「进入应用」。';
    return;
  }
  if (nickname.length > 32) {
    errorMsg.value = `昵称最多 32 个字符，当前是 ${nickname.length} 个。请改短一点再试。`;
    return;
  }

  submitting.value = true;
  try {
    // 先创建（或复用）本地身份，拿到 token
    const authData = await createLocalProfile(nickname);
    authStore.storeAuthResponse(authData);

    const existingName = authData?.username || '';
    if (existingName && existingName !== nickname) {
      // 本机已经有本地身份了，昵称以服务端为准
      displayName.value = existingName;
      successMsg.value = `本地身份已存在（昵称「${existingName}」），正在进入应用…`;
    } else {
      successMsg.value = '本地身份已创建，正在进入应用…';
    }

    // 再把账号头像传上去（这个接口需要 token，所以放在拿 token 之后）
    if (avatarFile.value) {
      try {
        await authStore.uploadAvatar(avatarFile.value);
      } catch (avatarError) {
        errorMsg.value = `身份已创建，但头像上传失败：${extractApiMessage(avatarError, '未知原因')}。可以点「移除」后直接进入应用，之后在「个人档案」里重新上传。`;
        return;
      }
    }

    await router.push('/dashboard');
  } catch (error) {
    errorMsg.value = describeError(error);
  } finally {
    submitting.value = false;
  }
}

onBeforeUnmount(() => {
  if (avatarPreviewUrl.value) {
    URL.revokeObjectURL(avatarPreviewUrl.value);
  }
});
</script>

<style scoped>
/* ========================================
   本地身份引导页：单列居中卡片，访客页整页铺满
   ======================================== */
.welcome {
  min-height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: var(--ui-space-10) var(--ui-space-8);
  background: var(--ui-bg-app);
  background-image: var(--ui-bg-app-wash);
  color: var(--ui-fg-primary);
  font-family: var(--ui-font-sans);
}

/* 居中卡片：细边框 + 分层阴影，比画布浮起来一层 */
.welcome__inner {
  width: 100%;
  max-width: 520px;
  padding: var(--ui-space-8);
  border-color: var(--ui-border-hairline);
  box-shadow: var(--ui-shadow-lg);
}

/* ---- 标题区 ---- */
.welcome__head {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
}

.welcome__mark {
  display: inline-flex;
  margin-bottom: var(--ui-space-4);
}

/* ---- 表单 ---- */
.welcome__form {
  display: flex;
  flex-direction: column;
  gap: var(--ui-space-5);
  margin-top: var(--ui-space-6);
}

/* .ui-field 自带 gap，标签再加 margin 会把间距撑成两倍 */
.ui-field > .ui-label {
  margin-bottom: 0;
}

/* ---- 账号头像 ---- */
.welcome__section {
  display: flex;
  flex-direction: column;
}

.welcome__avatar {
  display: flex;
  align-items: center;
  gap: var(--ui-space-4);
}

/* 头像保持圆形：选图后是照片，没选图时是昵称首字 */
.avatar-shell {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  width: 72px;
  height: 72px;
  border-radius: var(--ui-radius-full);
  background: var(--ui-accent-soft);
  border: 1px solid var(--ui-accent-border);
  color: var(--ui-fg-accent);
  font-size: var(--ui-text-2xl);
  font-weight: 600;
  overflow: hidden;
}

.avatar-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-actions {
  min-width: 0;
  flex: 1;
}

.avatar-actions__row {
  display: flex;
  flex-wrap: wrap;
  gap: var(--ui-space-2);
}

.avatar-tip {
  margin: var(--ui-space-2) 0 0;
  font-size: var(--ui-text-xs);
  line-height: var(--ui-line-normal);
  color: var(--ui-fg-muted);
  text-wrap: pretty;
}

.hidden-input {
  display: none;
}

/* ---- 提示条 ---- */
.ui-callout .app-icon {
  flex-shrink: 0;
  margin-top: 2px;
}

@media (max-width: 720px) {
  .welcome {
    padding: var(--ui-space-8) var(--ui-space-5);
  }

  .welcome__inner {
    padding: var(--ui-space-6);
  }
}
</style>
