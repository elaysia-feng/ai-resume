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

        <h1 class="auth-brand__title">本地简历与面试工作台</h1>
        <p class="auth-brand__desc">
          简历撰写、模板套用、模拟面试与 AI 对话都在你自己的机器上完成，数据不出本机。
        </p>

        <ul class="auth-brand__points">
          <li class="auth-brand__point">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
              <path d="M4 6h16M4 12h16M4 18h9" />
            </svg>
            <span>简历生成与多版本迭代</span>
          </li>
          <li class="auth-brand__point">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
              <path d="M4 4h16v16H4z" />
              <path d="M4 9h16M9 20V9" />
            </svg>
            <span>模板套用，一键导出 PDF</span>
          </li>
          <li class="auth-brand__point">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
              <path d="M12 3a3 3 0 0 0-3 3v6a3 3 0 0 0 6 0V6a3 3 0 0 0-3-3z" />
              <path d="M19 11v1a7 7 0 0 1-14 0v-1M12 19v3" />
            </svg>
            <span>模拟面试与 AI 对话复盘</span>
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
          <h2 class="auth-card__title">登录</h2>
          <p class="auth-card__subtitle">使用邮箱登录，继续编辑你的简历</p>
        </header>

        <!-- 登录方式切换 -->
        <div class="auth-tabs" role="tablist" aria-label="登录方式">
          <button
            type="button"
            role="tab"
            class="auth-tab"
            :class="{ 'is-active': loginMode === 'password' }"
            :aria-selected="loginMode === 'password'"
            @click="loginMode = 'password'"
          >
            密码登录
          </button>
          <button
            type="button"
            role="tab"
            class="auth-tab"
            :class="{ 'is-active': loginMode === 'code' }"
            :aria-selected="loginMode === 'code'"
            @click="loginMode = 'code'"
          >
            验证码登录
          </button>
        </div>

        <form class="auth-form" @submit.prevent="handleLogin">
          <!-- 密码登录 -->
          <template v-if="loginMode === 'password'">
            <div class="auth-field">
              <label class="ui-label" for="email">邮箱</label>
              <input
                id="email"
                v-model="form.email"
                type="email"
                class="ui-input"
                placeholder="you@example.com"
                required
                autocomplete="email"
              />
            </div>

            <div class="auth-field">
              <label class="ui-label" for="password">密码</label>
              <div class="auth-field__control">
                <input
                  id="password"
                  v-model="form.password"
                  :type="showPassword ? 'text' : 'password'"
                  class="ui-input auth-field__input--suffix"
                  placeholder="请输入登录密码"
                  required
                  autocomplete="current-password"
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
          </template>

          <!-- 验证码登录 -->
          <template v-else>
            <div class="auth-field">
              <label class="ui-label" for="codeEmail">邮箱</label>
              <input
                id="codeEmail"
                v-model="form.email"
                type="email"
                class="ui-input"
                placeholder="you@example.com"
                required
                autocomplete="email"
              />
            </div>

            <div class="auth-field">
              <label class="ui-label" for="codeInput">验证码</label>
              <div class="auth-field__row">
                <input
                  id="codeInput"
                  v-model="form.code"
                  type="text"
                  class="ui-input"
                  placeholder="6 位验证码"
                  required
                  maxlength="6"
                  autocomplete="one-time-code"
                  inputmode="numeric"
                />
                <button
                  type="button"
                  class="ui-btn ui-btn--outline auth-field__row-btn"
                  :disabled="countdown > 0 || sendingCode"
                  @click="handleSendCode"
                >
                  <span v-if="sendingCode">发送中…</span>
                  <span v-else-if="countdown > 0">{{ countdown }} 秒后重发</span>
                  <span v-else>发送验证码</span>
                </button>
              </div>
              <p v-if="codeSentMsg" class="auth-hint auth-hint--success">{{ codeSentMsg }}</p>
            </div>
          </template>

          <p v-if="errorMsg" class="auth-alert" role="alert">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
              <circle cx="12" cy="12" r="9" />
              <path d="M12 8v5M12 16.5v.01" />
            </svg>
            <span>{{ errorMsg }}</span>
          </p>

          <button type="submit" class="ui-btn ui-btn--primary ui-btn--block auth-submit" :disabled="loading">
            <span v-if="loading" class="auth-spinner" aria-hidden="true"></span>
            <span>{{ loading ? '登录中…' : '登录' }}</span>
          </button>
        </form>

        <p class="auth-switch">
          还没有账号？
          <router-link class="auth-switch__link" to="/register">立即注册</router-link>
        </p>
      </div>
    </section>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useAuthStore } from '../store/authStore.js';
import { sendCode } from '../api/auth.js';

const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();

const loginMode = ref('password');
const loading = ref(false);
const sendingCode = ref(false);
const countdown = ref(0);
const codeSentMsg = ref('');
const errorMsg = ref('');
const showPassword = ref(false);
let countdownTimer = null;

const form = reactive({
  email: '',
  password: '',
  code: '',
});

/** 把接口报错翻译成「发生了什么 + 怎么办」 */
function describeError(err, fallback) {
  const status = err?.response?.status;
  const raw = err?.response?.data?.message || err?.message || '';

  if (status === 401 || status === 403) {
    return raw
      ? `账号校验没通过：${raw}。请核对邮箱与密码（或验证码）后重试。`
      : '账号校验没通过。请核对邮箱与密码（或验证码）后重试，也可以改用「验证码登录」。';
  }
  if (status === 429) {
    return `请求太频繁了${raw ? `：${raw}` : ''}。请等待几十秒后再试一次。`;
  }
  if (/network|timeout|failed to fetch|ECONNREFUSED|网络/i.test(raw)) {
    return `连不上本地服务${raw ? `：${raw}` : ''}。请确认后端服务（:8080 与 :8000）已启动后重试。`;
  }
  return raw ? `${raw}` : fallback;
}

async function handleSendCode() {
  if (!form.email.trim()) {
    errorMsg.value = '还没填写邮箱。请先填写邮箱地址，再点击「发送验证码」。';
    return;
  }
  sendingCode.value = true;
  errorMsg.value = '';
  codeSentMsg.value = '';
  try {
    await sendCode(form.email, 'login');
    codeSentMsg.value = '验证码已发送，请在 60 秒内到邮箱查收。';
    startCountdown(60);
  } catch (err) {
    errorMsg.value = describeError(err, '验证码发送失败，请稍后重试；若持续失败请检查邮箱地址是否正确。');
  } finally {
    sendingCode.value = false;
  }
}

async function handleLogin() {
  if (loading.value) return;
  loading.value = true;
  errorMsg.value = '';
  try {
    if (loginMode.value === 'password') {
      await authStore.login({ email: form.email, password: form.password, loginMode: 'password' });
    } else {
      await authStore.loginByCode(form.email, form.code);
    }
    router.push(resolveRedirect());
  } catch (err) {
    errorMsg.value = describeError(err, '登录失败。请稍后重试，若一直失败请检查后端服务是否已启动。');
  } finally {
    loading.value = false;
  }
}

/** 优先回到路由守卫带来的目标页，否则回到工作台 */
function resolveRedirect() {
  const redirect = route.query.redirect;
  if (typeof redirect === 'string' && redirect.startsWith('/') && !redirect.startsWith('//')) {
    return redirect;
  }
  return '/';
}

function startCountdown(seconds) {
  countdown.value = seconds;
  clearInterval(countdownTimer);
  countdownTimer = setInterval(() => {
    countdown.value--;
    if (countdown.value <= 0) {
      clearInterval(countdownTimer);
      codeSentMsg.value = '';
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

/* ---- 登录方式切换 ---- */
.auth-tabs {
  display: flex;
  gap: var(--ui-space-1);
  padding: var(--ui-space-1);
  margin-bottom: var(--ui-space-6);
  background: var(--ui-bg-sunken);
  border: 1px solid var(--ui-border);
  border-radius: var(--ui-radius-md);
}

.auth-tab {
  flex: 1;
  padding: var(--ui-space-2) var(--ui-space-3);
  border: none;
  border-radius: calc(var(--ui-radius-md) - 4px);
  background: transparent;
  color: var(--ui-fg-secondary);
  font-family: var(--ui-font-sans);
  font-size: var(--ui-text-base);
  font-weight: 500;
  cursor: pointer;
  transition: background var(--ui-duration-fast) var(--ui-ease-in-out),
    color var(--ui-duration-fast) var(--ui-ease-in-out);
}

.auth-tab:hover {
  color: var(--ui-fg-primary);
}

.auth-tab:focus-visible {
  outline: none;
  box-shadow: var(--ui-ring);
}

.auth-tab.is-active {
  background: var(--ui-bg-surface);
  color: var(--ui-fg-accent);
  font-weight: 600;
  box-shadow: var(--ui-shadow-xs);
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

.auth-field__row {
  display: flex;
  align-items: center;
  gap: var(--ui-space-2);
}

.auth-field__row .ui-input {
  flex: 1;
  min-width: 0;
}

.auth-field__row-btn {
  flex-shrink: 0;
  white-space: nowrap;
}

/* ---- 提示信息 ---- */
.auth-hint {
  margin: var(--ui-space-2) 0 0;
  font-size: var(--ui-text-xs);
  color: var(--ui-fg-muted);
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
