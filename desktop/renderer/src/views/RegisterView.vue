<template>
  <div class="auth-page">
    <!-- 左侧品牌区 -->
    <section class="auth-brand">
      <div class="auth-brand__inner">
        <div class="auth-brand__logo">
          <span class="auth-brand__mark" aria-hidden="true">
            <svg viewBox="0 0 48 48" width="32" height="32" fill="none">
              <rect width="48" height="48" rx="13" fill="currentColor" />
              <path d="M15 15h19v4H19v4h13v4H19v6h-4V15z" fill="var(--ui-bg-surface)" />
            </svg>
          </span>
          <span class="auth-brand__name">AI Resume Forge</span>
        </div>

        <h1 class="auth-brand__title">三步开通你的工作台</h1>
        <p class="auth-brand__desc">
          验证邮箱、设置密码即可开始。账号只保存在本机，简历与面试记录不会上传到外部服务。
        </p>

        <ul class="auth-brand__points">
          <li class="auth-brand__point">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
              <path d="M4 6h16M4 12h16M4 18h9" />
            </svg>
            <span>邮箱验证码确认身份</span>
          </li>
          <li class="auth-brand__point">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
              <path d="M4 4h16v16H4z" />
              <path d="M4 9h16M9 20V9" />
            </svg>
            <span>注册后直接进入模板市场</span>
          </li>
          <li class="auth-brand__point">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
              <path d="M12 3a3 3 0 0 0-3 3v6a3 3 0 0 0 6 0V6a3 3 0 0 0-3-3z" />
              <path d="M19 11v1a7 7 0 0 1-14 0v-1M12 19v3" />
            </svg>
            <span>随时用模拟面试检验简历</span>
          </li>
        </ul>
      </div>

      <!-- 纯 SVG 装饰，不依赖外部图片 -->
      <svg class="auth-brand__art" viewBox="0 0 300 200" fill="none" aria-hidden="true">
        <rect x="18" y="14" width="168" height="176" rx="14" stroke="currentColor" stroke-width="1.5" opacity="0.3" />
        <rect x="42" y="44" width="84" height="10" rx="5" fill="currentColor" opacity="0.22" />
        <rect x="42" y="66" width="120" height="7" rx="3.5" fill="currentColor" opacity="0.14" />
        <rect x="42" y="82" width="104" height="7" rx="3.5" fill="currentColor" opacity="0.14" />
        <rect x="42" y="106" width="60" height="7" rx="3.5" fill="currentColor" opacity="0.14" />
        <rect x="42" y="122" width="112" height="7" rx="3.5" fill="currentColor" opacity="0.14" />
        <circle cx="196" cy="60" r="26" stroke="currentColor" stroke-width="1.5" opacity="0.3" />
        <path d="M196 50c1.8 5.2 4.8 8.2 10 10-5.2 1.8-8.2 4.8-10 10-1.8-5.2-4.8-8.2-10-10 5.2-1.8 8.2-4.8 10-10z" fill="currentColor" opacity="0.45" />
        <path d="M236 126h44M236 138h30" stroke="currentColor" stroke-width="2" stroke-linecap="round" opacity="0.25" />
        <circle cx="252" cy="166" r="18" stroke="currentColor" stroke-width="1.5" opacity="0.22" />
      </svg>
    </section>

    <!-- 右侧表单区 -->
    <section class="auth-panel">
      <div class="auth-card ui-card">
        <header class="auth-card__head">
          <h2 class="auth-card__title">注册账号</h2>
          <p class="auth-card__subtitle">使用邮箱验证注册，三步完成</p>
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
              <svg v-if="activeStep > step.id" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                <path d="M20 6 9 17l-5-5" />
              </svg>
              <span v-else>{{ step.id }}</span>
            </span>
            <span class="auth-step__label">{{ step.label }}</span>
          </li>
        </ol>

        <form class="auth-form" novalidate @submit.prevent="handleSubmit">
          <!--
            直接注册（不走邮箱验证码）
            桌面端是自包含应用，用户没配 SMTP 邮件服务时验证码根本发不出来
            （后端返回 "Could not parse mail"）。这条路径让应用开箱即用：
            用户名 + 邮箱 + 密码直接建号，邮箱只作为账号标识，不做校验。
          -->
          <template v-if="directMode">
            <p class="auth-note">
              直接创建账号，<strong>不需要邮箱验证码</strong>。邮箱只作为账号标识，不会收到任何邮件。
            </p>

            <div class="auth-field">
              <label class="ui-label" for="directUsername">用户名</label>
              <input
                id="directUsername"
                v-model="form.username"
                type="text"
                class="ui-input"
                placeholder="用于在应用内展示的名字"
                autocomplete="username"
              />
            </div>

            <div class="auth-field">
              <label class="ui-label" for="directEmail">邮箱地址</label>
              <input
                id="directEmail"
                v-model="form.email"
                type="email"
                class="ui-input"
                placeholder="you@example.com"
                autocomplete="email"
              />
            </div>

            <div class="auth-field">
              <label class="ui-label" for="directPassword">密码（至少 8 位）</label>
              <input
                id="directPassword"
                v-model="form.password"
                type="password"
                class="ui-input"
                placeholder="设置登录密码"
                minlength="8"
                autocomplete="new-password"
              />
            </div>

            <div class="auth-field">
              <label class="ui-label" for="directConfirmPassword">确认密码</label>
              <input
                id="directConfirmPassword"
                v-model="form.confirmPassword"
                type="password"
                class="ui-input"
                placeholder="再次输入密码"
                autocomplete="new-password"
              />
            </div>

            <button type="button" class="ui-btn ui-btn--ghost auth-back" @click="directMode = false">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                <path d="M19 12H5M12 19l-7-7 7-7" />
              </svg>
              返回邮箱验证注册
            </button>
          </template>

          <!-- 第 1 步：验证邮箱 -->
          <template v-else-if="activeStep === 1">
            <div class="auth-field">
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

            <div class="auth-field">
              <button
                type="button"
                class="ui-btn ui-btn--outline ui-btn--block"
                :disabled="countdown > 0 || sendingCode"
                @click="handleSendCode"
              >
                <span v-if="sendingCode">发送中…</span>
                <span v-else-if="countdown > 0">{{ countdown }} 秒后可重发</span>
                <span v-else>发送验证码</span>
              </button>
            </div>

            <p v-if="codeSent" class="auth-hint auth-hint--success">
              验证码已发送至 {{ form.email }}，请在 60 秒内查收并填写。
            </p>

            <p class="auth-hint">
              收不到验证码？桌面端默认不配置邮件服务，可直接用用户名和密码注册。
            </p>
            <button
              type="button"
              class="ui-btn ui-btn--ghost ui-btn--block"
              @click="directMode = true; errorMsg = ''; validationError = ''"
            >
              用用户名和密码直接注册
            </button>
          </template>

          <!-- 第 2 步：输入验证码 -->
          <template v-else-if="activeStep === 2">
            <p class="auth-note">请输入发送至 <strong>{{ form.email }}</strong> 的 6 位验证码。</p>

            <div class="auth-field">
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
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                <path d="M19 12H5M12 19l-7-7 7-7" />
              </svg>
              重新输入邮箱
            </button>
          </template>

          <!-- 第 3 步：设置用户名与密码 -->
          <template v-else>
            <div class="auth-field">
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

            <div class="auth-field">
              <label class="ui-label" for="password">密码（至少 8 位）</label>
              <div class="auth-field__control">
                <input
                  id="password"
                  v-model="form.password"
                  :type="showPassword ? 'text' : 'password'"
                  class="ui-input auth-field__input--suffix"
                  placeholder="设置登录密码"
                  minlength="8"
                  autocomplete="new-password"
                />
                <button
                  type="button"
                  class="auth-field__suffix"
                  :aria-label="showPassword ? '隐藏密码' : '显示密码'"
                  @click="showPassword = !showPassword"
                >
                  <svg v-if="!showPassword" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                    <path d="M2 12s3.6-7 10-7 10 7 10 7-3.6 7-10 7-10-7-10-7z" />
                    <circle cx="12" cy="12" r="3" />
                  </svg>
                  <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                    <path d="M10.6 6.2A9.9 9.9 0 0 1 12 6c6.4 0 10 6 10 6a17.6 17.6 0 0 1-2.6 3.3M6.3 6.7A17.4 17.4 0 0 0 2 12s3.6 7 10 7a9.8 9.8 0 0 0 4.2-.9" />
                    <path d="M3 3l18 18" />
                  </svg>
                </button>
              </div>
            </div>

            <div class="auth-field">
              <label class="ui-label" for="confirmPassword">确认密码</label>
              <div class="auth-field__control">
                <input
                  id="confirmPassword"
                  v-model="form.confirmPassword"
                  :type="showConfirmPassword ? 'text' : 'password'"
                  class="ui-input auth-field__input--suffix"
                  placeholder="再次输入密码"
                  autocomplete="new-password"
                />
                <button
                  type="button"
                  class="auth-field__suffix"
                  :aria-label="showConfirmPassword ? '隐藏密码' : '显示密码'"
                  @click="showConfirmPassword = !showConfirmPassword"
                >
                  <svg v-if="!showConfirmPassword" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                    <path d="M2 12s3.6-7 10-7 10 7 10 7-3.6 7-10 7-10-7-10-7z" />
                    <circle cx="12" cy="12" r="3" />
                  </svg>
                  <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                    <path d="M10.6 6.2A9.9 9.9 0 0 1 12 6c6.4 0 10 6 10 6a17.6 17.6 0 0 1-2.6 3.3M6.3 6.7A17.4 17.4 0 0 0 2 12s3.6 7 10 7a9.8 9.8 0 0 0 4.2-.9" />
                    <path d="M3 3l18 18" />
                  </svg>
                </button>
              </div>
            </div>

            <button type="button" class="ui-btn ui-btn--ghost auth-back" @click="goBackToStep2">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                <path d="M19 12H5M12 19l-7-7 7-7" />
              </svg>
              上一步
            </button>
          </template>

          <!-- 错误提示 -->
          <p v-if="validationError" class="auth-alert" role="alert">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
              <circle cx="12" cy="12" r="9" />
              <path d="M12 8v5M12 16.5v.01" />
            </svg>
            <span>{{ validationError }}</span>
          </p>
          <p v-if="errorMsg" class="auth-alert" role="alert">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
              <circle cx="12" cy="12" r="9" />
              <path d="M12 8v5M12 16.5v.01" />
            </svg>
            <span>{{ errorMsg }}</span>
          </p>

          <button
            v-if="directMode"
            type="submit"
            class="ui-btn ui-btn--primary ui-btn--block auth-submit"
            :disabled="loading || !form.username.trim() || !form.email.trim() || form.password.length < 8"
          >
            <span v-if="loading" class="auth-spinner" aria-hidden="true"></span>
            <span>{{ loading ? '创建中…' : '创建账号' }}</span>
          </button>
          <button
            v-else-if="activeStep === 1"
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
            <span v-if="loading" class="auth-spinner" aria-hidden="true"></span>
            <span>{{ loading ? '验证中…' : '验证验证码' }}</span>
          </button>
          <button v-else type="submit" class="ui-btn ui-btn--primary ui-btn--block auth-submit" :disabled="loading">
            <span v-if="loading" class="auth-spinner" aria-hidden="true"></span>
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
// 直接注册模式：桌面端未配置邮件服务时，验证码这条路走不通，改用用户名+密码直接建号
const directMode = ref(false);

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
 * 直接注册：用户名 + 邮箱 + 密码建号，不做邮箱验证。
 * 桌面端是自包含应用，默认没有邮件服务，验证码注册不可用；
 * 这条路径保证应用开箱就能建号使用。
 */
async function handleDirectRegister() {
  validationError.value = '';
  errorMsg.value = '';

  if (!form.username.trim()) {
    errorMsg.value = '还没填写用户名。';
    return;
  }
  if (!form.email.trim()) {
    errorMsg.value = '还没填写邮箱地址。';
    return;
  }
  if (form.password.length < 8) {
    errorMsg.value = '密码至少 8 位。';
    return;
  }
  if (form.password !== form.confirmPassword) {
    errorMsg.value = '两次输入的密码不一致。';
    return;
  }

  loading.value = true;
  try {
    await authStore.register({
      username: form.username.trim(),
      email: form.email.trim(),
      password: form.password,
    });
    router.push({ name: 'Home' });
  } catch (err) {
    errorMsg.value = describeError(err, '注册失败，请稍后重试。若提示用户名或邮箱已存在，请换一个再试。');
  } finally {
    loading.value = false;
  }
}

async function handleSubmit() {
  validationError.value = '';
  errorMsg.value = '';

  if (directMode.value) {
    await handleDirectRegister();
    return;
  }

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
  grid-template-columns: minmax(300px, 1fr) minmax(360px, 480px);
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
  background: linear-gradient(160deg, var(--ui-bg-canvas), var(--ui-bg-app));
  border-right: 1px solid var(--ui-border);
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

.auth-brand__mark {
  display: flex;
  color: var(--ui-accent);
}

.auth-brand__name {
  font-size: var(--ui-text-lg);
  font-weight: 700;
  color: var(--ui-fg-primary);
}

.auth-brand__title {
  margin: 0 0 var(--ui-space-3);
  font-size: var(--ui-text-3xl);
  font-weight: 700;
  line-height: var(--ui-line-tight);
  color: var(--ui-fg-primary);
  text-wrap: pretty;
}

.auth-brand__desc {
  margin: 0 0 var(--ui-space-8);
  font-size: var(--ui-text-md);
  line-height: var(--ui-line-relaxed);
  color: var(--ui-fg-secondary);
  text-wrap: pretty;
}

.auth-brand__points {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: var(--ui-space-3);
}

.auth-brand__point {
  display: flex;
  align-items: center;
  gap: var(--ui-space-3);
  font-size: var(--ui-text-base);
  color: var(--ui-fg-secondary);
}

.auth-brand__point svg {
  width: 18px;
  height: 18px;
  flex-shrink: 0;
  color: var(--ui-fg-accent);
}

/* 纯 SVG 装饰：压在文案下层，不遮挡文字 */
.auth-brand__art {
  position: absolute;
  right: 0;
  bottom: 0;
  z-index: 0;
  width: 320px;
  height: auto;
  color: var(--ui-fg-primary);
  opacity: 0.5;
  pointer-events: none;
}

/* ---- 右侧表单区 ---- */
.auth-panel {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: var(--ui-space-10) var(--ui-space-8);
  background: var(--ui-bg-app);
}

.auth-card {
  width: 100%;
  max-width: 420px;
  padding: var(--ui-space-8);
  background: var(--ui-bg-surface);
}

.auth-card__head {
  margin-bottom: var(--ui-space-6);
}

.auth-card__title {
  margin: 0 0 var(--ui-space-2);
  font-size: var(--ui-text-2xl);
  font-weight: 700;
  color: var(--ui-fg-primary);
}

.auth-card__subtitle {
  margin: 0;
  font-size: var(--ui-text-base);
  color: var(--ui-fg-secondary);
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

.auth-step__dot svg {
  width: 13px;
  height: 13px;
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

.auth-field {
  display: flex;
  flex-direction: column;
}

.auth-field__control {
  position: relative;
  display: flex;
  align-items: center;
}

.auth-field__input--suffix {
  padding-right: 2.5rem;
}

.auth-field__suffix {
  position: absolute;
  right: var(--ui-space-2);
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  padding: 0;
  border: none;
  border-radius: var(--ui-radius-sm);
  background: transparent;
  color: var(--ui-fg-muted);
  cursor: pointer;
  transition: color var(--ui-duration-fast) var(--ui-ease-in-out),
    background var(--ui-duration-fast) var(--ui-ease-in-out);
}

.auth-field__suffix svg {
  width: 18px;
  height: 18px;
}

.auth-field__suffix:hover {
  color: var(--ui-fg-accent);
  background: var(--ui-bg-hover);
}

.auth-field__suffix:focus-visible {
  outline: none;
  box-shadow: var(--ui-ring);
}

.auth-back {
  align-self: flex-start;
  padding-left: 0;
  padding-right: var(--ui-space-3);
  color: var(--ui-fg-secondary);
}

.auth-back svg {
  width: 16px;
  height: 16px;
}

.auth-note {
  margin: 0;
  font-size: var(--ui-text-sm);
  color: var(--ui-fg-secondary);
  word-break: break-word;
}

/* ---- 提示信息 ---- */
.auth-hint {
  margin: 0;
  font-size: var(--ui-text-xs);
  color: var(--ui-fg-muted);
  word-break: break-word;
}

.auth-hint--success {
  color: var(--ui-success);
}

.auth-alert {
  display: flex;
  align-items: flex-start;
  gap: var(--ui-space-2);
  margin: 0;
  padding: var(--ui-space-3);
  border: 1px solid var(--ui-danger-border);
  border-radius: var(--ui-radius-md);
  background: var(--ui-danger-soft);
  color: var(--ui-danger);
  font-size: var(--ui-text-sm);
  line-height: var(--ui-line-normal);
}

.auth-alert svg {
  width: 16px;
  height: 16px;
  flex-shrink: 0;
  margin-top: 2px;
}

.auth-submit {
  min-height: 2.75rem;
}

/* ---- 提交中动画 ---- */
.auth-spinner {
  display: inline-block;
  width: 16px;
  height: 16px;
  border: 2px solid var(--ui-accent-fg);
  border-top-color: transparent;
  border-radius: 50%;
  animation: auth-spin 0.7s linear infinite;
}

@keyframes auth-spin {
  to {
    transform: rotate(360deg);
  }
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
    border-bottom: 1px solid var(--ui-border);
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

  .auth-brand__art {
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
