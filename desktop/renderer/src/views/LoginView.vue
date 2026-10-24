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

        <p class="auth-brand__title ui-page-title">本地简历与面试工作台</p>
        <p class="auth-brand__desc ui-page-subtitle">
          简历撰写、模板套用、模拟面试与 AI 对话都在你自己的机器上完成，数据不出本机。
        </p>

        <ul class="auth-brand__points ui-panel ui-panel--flat">
          <li class="auth-brand__point">
            <AppIcon name="file-text" :size="18" />
            <span>简历生成与多版本迭代</span>
          </li>
          <li class="auth-brand__point">
            <AppIcon name="layout" :size="18" />
            <span>模板套用，一键导出 PDF</span>
          </li>
          <li class="auth-brand__point">
            <AppIcon name="microphone" :size="18" />
            <span>模拟面试与 AI 对话复盘</span>
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
          <h1 class="ui-page-title">登录</h1>
          <p class="ui-page-subtitle">使用邮箱登录，继续编辑你的简历</p>
        </header>

        <!-- 登录方式切换 -->
        <div class="ui-segmented auth-modes" role="tablist" aria-label="登录方式">
          <button
            type="button"
            role="tab"
            class="ui-segmented__item"
            :class="{ 'is-active': loginMode === 'password' }"
            :aria-selected="loginMode === 'password'"
            @click="loginMode = 'password'"
          >
            密码登录
          </button>
          <button
            type="button"
            role="tab"
            class="ui-segmented__item"
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
            <div class="ui-field">
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

            <div class="ui-field">
              <label class="ui-label" for="password">密码</label>
              <div class="auth-field__control">
                <input
                  id="password"
                  v-model="form.password"
                  :type="showPassword ? 'text' : 'password'"
                  class="ui-input auth-input--suffix"
                  placeholder="请输入登录密码"
                  required
                  autocomplete="current-password"
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
          </template>

          <!-- 验证码登录 -->
          <template v-else>
            <div class="ui-field">
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

            <div class="ui-field">
              <label class="ui-label" for="codeInput">验证码</label>
              <div class="auth-code-row">
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
                  class="ui-btn ui-btn--outline auth-code-row__btn"
                  :disabled="countdown > 0 || sendingCode"
                  @click="handleSendCode"
                >
                  <AppIcon :name="countdown > 0 ? 'clock' : 'paper-plane-tilt'" :size="16" />
                  <span v-if="sendingCode">发送中…</span>
                  <span v-else-if="countdown > 0">{{ countdown }} 秒后重发</span>
                  <span v-else>发送验证码</span>
                </button>
              </div>
              <div v-if="codeSentMsg" class="ui-callout ui-callout--accent">
                <AppIcon name="check-circle" :size="16" />
                <span>{{ codeSentMsg }}</span>
              </div>
            </div>
          </template>

          <div v-if="errorMsg" class="ui-callout ui-callout--danger" role="alert">
            <AppIcon name="warning-circle" :size="16" />
            <span>{{ errorMsg }}</span>
          </div>

          <button type="submit" class="ui-btn ui-btn--primary ui-btn--block auth-submit" :disabled="loading">
            <AppIcon v-if="loading" name="spinner-gap" :size="16" spin />
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
import AppIcon from '../components/common/AppIcon.vue';

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
  box-shadow: var(--ui-shadow-lg), var(--ui-highlight-top);
}

.auth-card__head {
  margin-bottom: var(--ui-space-6);
}

/* ---- 登录方式切换：复用分段控件 ---- */
.auth-modes {
  display: flex;
  width: 100%;
  margin-bottom: var(--ui-space-6);
}

.auth-modes .ui-segmented__item {
  flex: 1;
  justify-content: center;
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

.auth-code-row {
  display: flex;
  align-items: center;
  gap: var(--ui-space-2);
}

.auth-code-row .ui-input {
  flex: 1;
  min-width: 0;
}

.auth-code-row__btn {
  flex-shrink: 0;
  white-space: nowrap;
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
