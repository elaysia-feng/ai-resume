<template>
  <div class="welcome">
    <aside class="welcome-brand">
      <div class="welcome-brand__logo">
        <AppBrandMark :size="42" />
        <span>AI Resume Forge</span>
      </div>

      <div class="welcome-brand__message">
        <p class="welcome-brand__eyebrow">求职准备工作台</p>
        <h1>创建本地身份</h1>
        <p>用昵称进入工作台。账号头像与简历中的姓名、照片分别保存，互不替代。</p>
      </div>

      <div class="welcome-brand__note">
        <AppIcon name="desktop" :size="18" />
        <span>这台电脑上的本地身份无需邮箱和密码。</span>
      </div>
    </aside>

    <main class="welcome-main">
      <div class="welcome__inner">
        <header class="welcome__head">
          <h2>你的账号身份</h2>
          <p>昵称会显示在应用侧边栏和个人档案中。</p>
        </header>

        <form class="welcome__form" novalidate @submit.prevent="handleSubmit">
          <section class="welcome__section">
            <div class="welcome__section-head">
              <div>
                <h3>账号头像</h3>
                <p>可选 · 仅用于应用内展示</p>
              </div>
              <span class="welcome__avatar-shell" aria-hidden="true">
                <img v-if="avatarPreviewUrl" :src="avatarPreviewUrl" alt="" class="welcome__avatar-image" />
                <span v-else class="welcome__avatar-text">{{ nicknameInitial }}</span>
              </span>
            </div>

            <div class="welcome__avatar-actions">
              <button type="button" class="ui-btn ui-btn--outline ui-btn--sm" :disabled="submitting" @click="handlePickAvatar">
                <AppIcon name="image" :size="16" />
                {{ avatarFile ? '重新选择头像' : '选择头像' }}
              </button>
              <button v-if="avatarFile" type="button" class="ui-btn ui-btn--ghost ui-btn--sm" :disabled="submitting" @click="removeAvatar">
                <AppIcon name="x" :size="16" />
                移除
              </button>
              <input ref="avatarInputRef" type="file" accept="image/*" class="hidden-input" @change="handleAvatarInputChange" />
            </div>
            <p class="ui-field__hint">{{ avatarTip }}</p>
          </section>

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
            <p class="ui-field__hint">1–32 个字符，用作应用内展示名。</p>
          </div>

          <div v-if="errorMsg" class="ui-callout ui-callout--danger" role="alert">
            <AppIcon name="warning-circle" :size="16" />
            <span>{{ errorMsg }}</span>
          </div>

          <div v-if="successMsg" class="ui-callout ui-callout--accent" role="status">
            <AppIcon name="check-circle" :size="16" />
            <span>{{ successMsg }}</span>
          </div>

          <button type="submit" class="ui-btn ui-btn--primary ui-btn--lg welcome-submit" :disabled="submitting">
            <AppIcon v-if="submitting" name="spinner-gap" :size="16" spin />
            <span>{{ submitting ? '正在创建…' : '进入应用' }}</span>
            <AppIcon v-if="!submitting" name="caret-right" :size="17" />
          </button>
        </form>
      </div>
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
import AppBrandMark from '../components/common/AppBrandMark.vue';

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
.welcome {
  display: grid;
  min-height: 100%;
  grid-template-columns: minmax(320px, 0.82fr) minmax(480px, 1.18fr);
  background: var(--ui-bg-surface);
  color: var(--ui-fg-primary);
}

.welcome-brand {
  display: flex;
  min-width: 0;
  flex-direction: column;
  justify-content: space-between;
  padding: 34px clamp(28px, 4vw, 64px) 30px;
  border-right: 1px solid var(--ui-grid-line-strong);
  background: var(--ui-bg-canvas);
}

.welcome-brand__logo {
  display: flex;
  align-items: center;
  gap: 12px;
  color: var(--ui-fg-primary);
  font-family: var(--ui-font-display);
  font-size: 18px;
  font-weight: 650;
  letter-spacing: -0.02em;
}

.welcome-brand__message {
  width: 100%;
  max-width: 440px;
  margin: auto 0;
  padding: 60px 0;
}

.welcome-brand__eyebrow {
  margin: 0 0 18px;
  color: var(--ui-fg-accent);
  font-size: 14px;
  font-weight: 600;
}

.welcome-brand__message h1 {
  margin: 0;
  font-family: var(--ui-font-display);
  font-size: clamp(34px, 3.5vw, 50px);
  font-weight: 650;
  letter-spacing: -0.025em;
  line-height: 1.15;
}

.welcome-brand__message p:last-child {
  max-width: 40ch;
  margin: 18px 0 0;
  color: var(--ui-fg-secondary);
  font-size: 16px;
  line-height: 1.75;
}

.welcome-brand__note {
  display: flex;
  align-items: center;
  gap: 10px;
  padding-top: 18px;
  border-top: 1px solid var(--ui-grid-line-strong);
  color: var(--ui-fg-secondary);
  font-size: 13px;
}

.welcome-brand__note .app-icon {
  flex: 0 0 auto;
  color: var(--ui-accent);
}

.welcome-main {
  display: flex;
  min-width: 0;
  align-items: center;
  justify-content: center;
  padding: 48px clamp(28px, 6vw, 96px);
  background: var(--ui-bg-surface);
}

.welcome__inner {
  width: 100%;
  max-width: 520px;
}

.welcome__head {
  padding-bottom: 22px;
  border-bottom: 1px solid var(--ui-grid-line);
}

.welcome__head h2 {
  margin: 0;
  font-size: 26px;
  font-weight: 650;
  letter-spacing: -0.02em;
}

.welcome__head p {
  margin: 8px 0 0;
  color: var(--ui-fg-secondary);
  font-size: 14px;
  line-height: 1.6;
}

.welcome__form {
  display: flex;
  flex-direction: column;
  gap: 24px;
  padding-top: 24px;
}

.welcome__section {
  padding-bottom: 20px;
  border-bottom: 1px solid var(--ui-grid-line);
}

.welcome__section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
}

.welcome__section-head h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
}

.welcome__section-head p {
  margin: 5px 0 0;
  color: var(--ui-fg-muted);
  font-size: 12px;
}

.welcome__avatar-shell {
  display: grid;
  width: 64px;
  height: 64px;
  flex: 0 0 auto;
  place-items: center;
  overflow: hidden;
  border: 1px solid var(--ui-accent-border);
  background: var(--ui-accent-soft);
  color: var(--ui-fg-accent);
  font-size: 24px;
  font-weight: 600;
}

.welcome__avatar-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.welcome__avatar-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 14px;
}

.welcome__section .ui-field__hint {
  margin: 10px 0 0;
  line-height: 1.55;
}

.welcome__form > .ui-field > .ui-label {
  margin-bottom: 0;
}

.hidden-input {
  display: none;
}

.welcome-submit {
  width: 100%;
  min-height: 54px;
  justify-content: space-between;
  padding-right: 18px;
  padding-left: 18px;
}

@media (max-width: 820px) {
  .welcome {
    grid-template-columns: minmax(0, 1fr);
  }

  .welcome-brand {
    min-height: 240px;
    padding: 24px;
    border-right: 0;
    border-bottom: 1px solid var(--ui-grid-line-strong);
  }

  .welcome-brand__message {
    padding: 28px 0;
  }

  .welcome-brand__note {
    padding-top: 12px;
  }

  .welcome-main {
    padding: 32px 24px;
  }
}
</style>
