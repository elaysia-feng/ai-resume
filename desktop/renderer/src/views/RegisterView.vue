<template>
  <div class="auth-page">
    <!-- 左侧品牌区 -->
    <section class="auth-brand">
      <div class="auth-brand__inner">
        <div class="auth-brand__logo">
          <span class="auth-brand__mark ui-icon-tile ui-icon-tile--lg" aria-hidden="true">
            <AppIcon name="sparkle" :size="20" />
          </span>
          <span class="auth-brand__name">AI Resume Forge</span>
        </div>

        <p class="auth-brand__title ui-page-title">三步开通你的工作台</p>
        <p class="auth-brand__desc ui-page-subtitle">
          验证邮箱、设置密码后即可使用。简历与历史记录保存在本机；启用 AI 时，当前任务所需内容会发送到你配置的模型服务。
        </p>

        <ul class="auth-brand__points ui-panel ui-panel--flat">
          <li class="auth-brand__point">
            <AppIcon name="envelope" :size="18" />
            <span>邮箱验证码确认身份</span>
          </li>
          <li class="auth-brand__point">
            <AppIcon name="layout" :size="18" />
            <span>注册后进入工作台总览</span>
          </li>
          <li class="auth-brand__point">
            <AppIcon name="microphone" :size="18" />
            <span>随时用模拟面试检验简历</span>
          </li>
        </ul>
      </div>

      <!-- 极淡水印：用内置图标给大面积留白一点纹理，不依赖外部图片 -->
      <AppIcon name="file-text" class="auth-brand__watermark" :size="240" />
    </section>

    <!-- 右侧表单区 -->
    <section class="auth-panel">
      <div class="auth-card ui-panel">
        <header class="auth-card__head">
          <h1 class="ui-page-title">注册账号</h1>
          <p class="ui-page-subtitle">使用邮箱验证注册，三步完成</p>
        </header>

        <!-- 步骤进度 -->
        <ol class="auth-steps" :aria-label="`注册进度，当前第 ${activeStep} 步，共 3 步`">
          <li
            v-for="step in steps"
            :key="step.id"
            class="auth-step"
            :class="{
              'is-active': activeStep === step.id,
              'is-complete': activeStep > step.id,
            }"
          >
            <span class="auth-step__dot">
              <AppIcon v-if="activeStep > step.id" name="check" :size="14" />
              <span v-else>{{ step.id }}</span>
            </span>
            <span class="auth-step__label">{{ step.label }}</span>
          </li>
        </ol>

        <form class="auth-form" novalidate @submit.prevent="handleSubmit">
          <!-- 第 1 步：验证邮箱 -->
          <template v-if="activeStep === 1">
            <div class="ui-field">
              <label class="ui-label" for="email">邮箱地址</label>
              <input
                id="email"
                v-model="form.email"
                type="email"
                class="ui-input"
                placeholder="you@example.com"
                autocomplete="email"
                :disabled="countdown > 0"
              />
            </div>

            <div class="ui-field">
              <button
                type="button"
                class="ui-btn ui-btn--outline ui-btn--block"
                :disabled="countdown > 0 || sendingCode"
                @click="handleSendCode"
              >
                <AppIcon :name="countdown > 0 ? 'clock' : 'paper-plane-tilt'" :size="16" />
                <span v-if="sendingCode">发送中…</span>
                <span v-else-if="countdown > 0">{{ countdown }} 秒后可重发</span>
                <span v-else>发送验证码</span>
              </button>
            </div>

            <div v-if="codeSent" class="ui-callout ui-callout--accent">
              <AppIcon name="check-circle" :size="16" />
              <span>验证码已发送至 {{ form.email }}，请在 60 秒内查收并填写。</span>
            </div>
          </template>

          <!-- 第 2 步：输入验证码 -->
          <template v-else-if="activeStep === 2">
            <div class="ui-callout">
              <AppIcon name="envelope" :size="16" />
              <span>请输入发送至 <strong>{{ form.email }}</strong> 的 6 位验证码。</span>
            </div>

            <div class="ui-field">
              <label class="ui-label" for="code">验证码</label>
              <input
                id="code"
                v-model="form.code"
                type="text"
                class="ui-input"
                placeholder="6 位验证码"
                maxlength="6"
                autocomplete="one-time-code"
                inputmode="numeric"
              />
            </div>

            <button type="button" class="ui-btn ui-btn--ghost auth-back" @click="goBackToStep1">
              <AppIcon name="arrow-left" :size="16" />
              重新输入邮箱
            </button>
          </template>

          <!-- 第 3 步：设置用户名与密码 -->
          <template v-else>
            <div class="ui-field">
              <label class="ui-label" for="username">用户名</label>
              <input
                id="username"
                v-model="form.username"
                type="text"
                class="ui-input"
                placeholder="用于在应用内展示的名字"
                autocomplete="username"
              />
            </div>

            <div class="ui-field">
              <label class="ui-label" for="password">密码（至少 8 位）</label>
              <div class="auth-field__control">
                <input
                  id="password"
                  v-model="form.password"
                  :type="showPassword ? 'text' : 'password'"
                  class="ui-input auth-input--suffix"
                  placeholder="设置登录密码"
                  minlength="8"
                  autocomplete="new-password"
                />
                <button
                  type="button"
                  class="ui-icon-btn auth-field__suffix"
                  :aria-label="showPassword ? '隐藏密码' : '显示密码'"
                  @click="showPassword = !showPassword"
                >
                  <AppIcon :name="showPassword ? 'eye-slash' : 'eye'" :size="16" />
                </button>
              </div>
            </div>

            <div class="ui-field">
              <label class="ui-label" for="confirmPassword">确认密码</label>
              <div class="auth-field__control">
                <input
                  id="confirmPassword"
                  v-model="form.confirmPassword"
                  :type="showConfirmPassword ? 'text' : 'password'"
                  class="ui-input auth-input--suffix"
                  placeholder="再次输入密码"
                  autocomplete="new-password"
                />
                <button
                  type="button"
                  class="ui-icon-btn auth-field__suffix"
                  :aria-label="showConfirmPassword ? '隐藏密码' : '显示密码'"
                  @click="showConfirmPassword = !showConfirmPassword"
                >
                  <AppIcon :name="showConfirmPassword ? 'eye-slash' : 'eye'" :size="16" />
                </button>
              </div>
            </div>

            <button type="button" class="ui-btn ui-btn--ghost auth-back" @click="goBackToStep2">
              <AppIcon name="arrow-left" :size="16" />
              上一步
            </button>
          </template>

          <!-- 错误提示 -->
          <div v-if="validationError" class="ui-callout ui-callout--danger" role="alert">
            <AppIcon name="warning-circle" :size="16" />
            <span>{{ validationError }}</span>
          </div>
          <div v-if="errorMsg" class="ui-callout ui-callout--danger" role="alert">
            <AppIcon name="warning-circle" :size="16" />
            <span>{{ errorMsg }}</span>
          </div>

          <button
            v-if="activeStep === 1"
            type="submit"
            class="ui-btn ui-btn--primary ui-btn--block auth-submit"
            :disabled="!form.email.trim() || sendingCode"
          >
            下一步
          </button>
          <button
            v-else-if="activeStep === 2"
            type="submit"
            class="ui-btn ui-btn--primary ui-btn--block auth-submit"
            :disabled="form.code.length !== 6 || loading"
          >
            <AppIcon v-if="loading" name="spinner-gap" :size="16" spin />
            <span>{{ loading ? '验证中…' : '验证验证码' }}</span>
          </button>
          <button v-else type="submit" class="ui-btn ui-btn--primary ui-btn--block auth-submit" :disabled="loading">
            <AppIcon v-if="loading" name="spinner-gap" :size="16" spin />
            <span>{{ loading ? '注册中…' : '完成注册' }}</span>
          </button>
        </form>

        <p class="auth-switch">
          已有账号？
          <router-link class="auth-switch__link" to="/login">立即登录</router-link>
        </p>
      </div>
    </section>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { sendCode, verifyCode, setPassword } from '../api/auth.js';
import { useAuthStore } from '../store/authStore.js';
import AppIcon from '../components/common/AppIcon.vue';

const router = useRouter();
const authStore = useAuthStore();

const steps = [
  { id: 1, label: '验证邮箱' },
  { id: 2, label: '输入验证码' },
  { id: 3, label: '设置密码' },
];

const activeStep = ref(1);
const loading = ref(false);
const sendingCode = ref(false);
const countdown = ref(0);
const codeSent = ref(false);
const validationError = ref('');
const errorMsg = ref('');
const showPassword = ref(false);
const showConfirmPassword = ref(false);

let verifyToken = '';
let countdownTimer = null;

const form = reactive({
  email: '',
  code: '',
  username: '',
  password: '',
  confirmPassword: '',
});

/** 把接口报错翻译成「发生了什么 + 怎么办」 */
function describeError(err, fallback) {
  const status = err?.response?.status;
  const raw = err?.response?.data?.message || err?.message || '';

  if (status === 404 || status === 400) {
    return raw
      ? `这一步没能完成：${raw}。请确认邮箱与验证码填写正确后重试。`
      : '这一步没能完成。请确认邮箱与验证码填写正确后重试。';
  }
  if (status === 429) {
    return `请求太频繁了${raw ? `：${raw}` : ''}。请等待几十秒后再试一次。`;
  }
  if (/network|timeout|failed to fetch|ECONNREFUSED|网络/i.test(raw)) {
    return `连不上本地服务${raw ? `：${raw}` : ''}。请确认后端服务（:8080 与 :8000）已启动后重试。`;
  }
  return raw ? `${raw}` : fallback;
}

function advanceToStep2() {
  if (!form.email.trim()) {
    validationError.value = '还没填写邮箱。请先填写邮箱地址，再点击「下一步」。';
    return;
  }
  if (!codeSent.value) {
    validationError.value = '还没收到验证码。请先点击「发送验证码」，收到后再进入下一步。';
    return;
  }
  validationError.value = '';
  activeStep.value = 2;
}

function goBackToStep1() {
  validationError.value = '';
  errorMsg.value = '';
  activeStep.value = 1;
}

function goBackToStep2() {
  validationError.value = '';
  errorMsg.value = '';
  activeStep.value = 2;
}

async function handleSendCode() {
  validationError.value = '';
  errorMsg.value = '';
  if (!form.email.trim()) {
    errorMsg.value = '还没填写邮箱。请先填写邮箱地址，再点击「发送验证码」。';
    return;
  }
  sendingCode.value = true;
  try {
    await sendCode(form.email, 'register');
    codeSent.value = true;
    startCountdown(60);
  } catch (err) {
    errorMsg.value = describeError(err, '验证码发送失败，请稍后重试；若持续失败请检查邮箱地址是否正确。');
  } finally {
    sendingCode.value = false;
  }
}

/**
 * 注册主流程：验证邮箱 → 输入验证码 → 设置用户名与密码。
 * 桌面端不再走这条路（首次启动改为创建本地身份），这里保留给网页端使用。
 */
async function handleSubmit() {
  validationError.value = '';
  errorMsg.value = '';

  if (activeStep.value === 1) {
    advanceToStep2();
    return;
  }

  if (activeStep.value === 2) {
    if (!form.code.trim() || form.code.length !== 6) {
      validationError.value = '验证码长度不对。请填写邮箱收到的 6 位数字验证码。';
      return;
    }
    loading.value = true;
    try {
      const verifyData = await verifyCode(form.email, form.code);
      verifyToken = verifyData?.verifyToken || verifyData?.token || verifyToken;
      activeStep.value = 3;
    } catch (err) {
      errorMsg.value = describeError(err, '验证码校验失败。请检查验证码是否正确或已过期，必要时重新发送。');
    } finally {
      loading.value = false;
    }
    return;
  }

  if (activeStep.value === 3) {
    if (!form.username.trim()) {
      validationError.value = '用户名不能为空。请填写一个用于展示的名字。';
      return;
    }
    if (form.password !== form.confirmPassword) {
      validationError.value = '两次输入的密码不一致。请重新确认密码，两处填写要完全相同。';
      return;
    }
    if (form.password.length < 8) {
      validationError.value = '密码太短了。请把密码改成至少 8 位。';
      return;
    }

    loading.value = true;
    try {
      const authData = await setPassword(verifyToken, form.username, form.password);
      authStore.storeAuthResponse(authData);
      router.push('/');
    } catch (err) {
      errorMsg.value = describeError(err, '注册失败。请稍后重试，若一直失败请检查后端服务是否已启动。');
    } finally {
      loading.value = false;
    }
  }
}

function startCountdown(seconds) {
  countdown.value = seconds;
  clearInterval(countdownTimer);
  countdownTimer = setInterval(() => {
    countdown.value--;
    if (countdown.value <= 0) {
      clearInterval(countdownTimer);
    }
  }, 1000);
}
</script>

<style scoped>
/* ========================================
   认证页骨架：左侧品牌区 + 右侧表单区
   ======================================== */
.auth-page {
  display: grid;
  grid-template-columns: minmax(320px, 1fr) minmax(400px, 520px);
  min-height: 100%;
  background: var(--ui-bg-app);
  color: var(--ui-fg-primary);
  font-family: var(--ui-font-sans);
}

/* ---- 左侧品牌区 ---- */
.auth-brand {
  position: relative;
  display: flex;
  align-items: center;
  overflow: hidden;
  background: var(--ui-bg-canvas);
  background-image: var(--ui-bg-app-wash);
  border-right: 1px solid var(--ui-border-hairline);
}

.auth-brand__inner {
  position: relative;
  z-index: 1;
  width: 100%;
  max-width: 520px;
  margin: 0 auto;
  padding: var(--ui-space-12) var(--ui-space-10);
}

.auth-brand__logo {
  display: flex;
  align-items: center;
  gap: var(--ui-space-3);
  margin-bottom: var(--ui-space-8);
}

/* 品牌标识：图标底座本身就是品牌色块 */
.auth-brand__mark {
  display: inline-flex;
}

.auth-brand__name {
  font-family: var(--ui-font-display);
  font-size: var(--ui-text-lg);
  font-weight: 650;
  letter-spacing: var(--ui-tracking-snug);
  color: var(--ui-fg-primary);
}

/* 品牌主张比卡片标题大一档，但仍是桌面应用的信息密度 */
.auth-brand__title {
  font-size: var(--ui-text-3xl);
}

.auth-brand__desc {
  margin-bottom: var(--ui-space-8);
}

/* 能力清单：白底子卡片压在灰底品牌区上，形成一层厚度 */
.auth-brand__points {
  list-style: none;
  margin: 0;
  padding: var(--ui-space-5);
  display: flex;
  flex-direction: column;
  gap: var(--ui-space-4);
  border-color: var(--ui-border-hairline);
}

.auth-brand__point {
  display: flex;
  align-items: center;
  gap: var(--ui-space-3);
  font-size: var(--ui-text-base);
  color: var(--ui-fg-secondary);
}

.auth-brand__point .app-icon {
  color: var(--ui-fg-accent);
}

/* 极淡水印：压在大面积留白上，不抢文案 */
.auth-brand__watermark {
  position: absolute;
  right: -32px;
  bottom: -40px;
  z-index: 0;
  color: var(--ui-fg-primary);
  opacity: 0.05;
  pointer-events: none;
}

/* ---- 右侧表单区 ---- */
.auth-panel {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: var(--ui-space-10) var(--ui-space-8);
  background: var(--ui-bg-app);
  background-image: var(--ui-bg-app-wash);
}

/* 表单卡片：细边框 + 分层阴影，比画布浮起来一层 */
.auth-card {
  width: 100%;
  max-width: 420px;
  padding: var(--ui-space-8);
  border-color: var(--ui-border-hairline);
  box-shadow: var(--ui-shadow-lg);
}

.auth-card__head {
  margin-bottom: var(--ui-space-6);
}

/* ---- 步骤进度 ---- */
.auth-steps {
  list-style: none;
  display: flex;
  align-items: flex-start;
  gap: var(--ui-space-2);
  margin: 0 0 var(--ui-space-6);
  padding: 0;
}

.auth-step {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--ui-space-2);
  min-width: 0;
  position: relative;
}

.auth-step:not(:last-child)::after {
  content: '';
  position: absolute;
  top: 13px;
  left: 50%;
  width: 100%;
  height: 1px;
  background: var(--ui-border);
}

.auth-step.is-complete:not(:last-child)::after {
  background: var(--ui-accent-border);
}

.auth-step__dot {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 26px;
  height: 26px;
  border-radius: var(--ui-radius-full);
  border: 1px solid var(--ui-border-strong);
  background: var(--ui-bg-surface);
  color: var(--ui-fg-muted);
  font-size: var(--ui-text-xs);
  font-weight: 600;
}

.auth-step.is-active .auth-step__dot {
  border-color: var(--ui-accent);
  color: var(--ui-fg-accent);
  box-shadow: var(--ui-ring);
}

.auth-step.is-complete .auth-step__dot {
  border-color: var(--ui-accent-border);
  background: var(--ui-accent-soft);
  color: var(--ui-fg-accent);
}

.auth-step__label {
  font-size: var(--ui-text-xs);
  color: var(--ui-fg-muted);
  white-space: nowrap;
}

.auth-step.is-active .auth-step__label {
  color: var(--ui-fg-accent);
  font-weight: 600;
}

/* ---- 表单 ---- */
.auth-form {
  display: flex;
  flex-direction: column;
  gap: var(--ui-space-5);
}

/* .ui-field 自带 gap，标签再加 margin 会把间距撑成两倍 */
.ui-field > .ui-label {
  margin-bottom: 0;
}

.auth-field__control {
  position: relative;
  display: flex;
  align-items: center;
}

/* 给右侧图标按钮让出位置 */
.auth-input--suffix {
  padding-right: 2.75rem;
}

.auth-field__suffix {
  position: absolute;
  right: var(--ui-space-1);
  top: 50%;
  transform: translateY(-50%);
}

.auth-back {
  align-self: flex-start;
  padding-left: 0;
  padding-right: var(--ui-space-3);
  color: var(--ui-fg-secondary);
}

/* ---- 提示信息 ---- */
.ui-callout .app-icon {
  flex-shrink: 0;
  margin-top: 2px;
}

/* ---- 提交按钮 ---- */
.auth-submit {
  min-height: 2.75rem;
}

/* ---- 底部切换 ---- */
.auth-switch {
  margin: var(--ui-space-6) 0 0;
  font-size: var(--ui-text-sm);
  color: var(--ui-fg-secondary);
  text-align: center;
}

.auth-switch__link {
  color: var(--ui-fg-accent);
  font-weight: 600;
}

.auth-switch__link:hover {
  text-decoration: underline;
}

/* ========================================
   窄窗口（1280 以下或分栏放不下）：两栏上下堆叠
   ======================================== */
@media (max-width: 1024px) {
  .auth-page {
    grid-template-columns: 1fr;
  }

  .auth-brand {
    border-right: none;
    border-bottom: 1px solid var(--ui-border-hairline);
  }

  .auth-brand__inner {
    max-width: none;
    padding: var(--ui-space-8) var(--ui-space-6);
  }

  .auth-brand__logo {
    margin-bottom: var(--ui-space-5);
  }

  .auth-brand__title {
    font-size: var(--ui-text-2xl);
  }

  .auth-brand__desc {
    margin-bottom: var(--ui-space-5);
    font-size: var(--ui-text-base);
  }

  .auth-brand__watermark {
    display: none;
  }

  .auth-panel {
    padding: var(--ui-space-8) var(--ui-space-6);
  }
}

@media (max-width: 640px) {
  .auth-brand__points {
    display: none;
  }

  .auth-card {
    padding: var(--ui-space-6);
  }
}
</style>
