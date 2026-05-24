<template>
  <div class="auth-page">
    <!-- Animated gradient mesh background -->
    <div class="gradient-mesh" aria-hidden="true">
      <div class="mesh-blob mesh-blob-1"></div>
      <div class="mesh-blob mesh-blob-2"></div>
      <div class="mesh-blob mesh-blob-3"></div>
      <div class="mesh-blob mesh-blob-4"></div>
    </div>

    <!-- Floating decorative shapes -->
    <div class="floating-shapes" aria-hidden="true">
      <div class="shape shape-1"></div>
      <div class="shape shape-2"></div>
      <div class="shape shape-3"></div>
    </div>

    <!-- Auth card -->
    <div class="auth-card" :class="{ 'is-visible': isLoaded }">
      <!-- Header -->
      <div class="auth-header">
        <h1 class="auth-title">注册账号</h1>
        <p class="auth-subtitle">使用 QQ 邮箱验证注册</p>
      </div>

      <!-- Progress indicator -->
      <div class="progress-track" role="progressbar" :aria-valuenow="activeStep" aria-valuemin="1" aria-valuemax="3">
        <!-- Connecting lines -->
        <div class="progress-line">
          <div class="progress-line-fill" :style="{ width: activeStep >= 2 ? '100%' : '0%' }"></div>
        </div>
        <div class="progress-line">
          <div class="progress-line-fill" :style="{ width: activeStep >= 3 ? '100%' : '0%' }"></div>
        </div>

        <!-- Step 1 -->
        <div class="progress-step" :class="{ 'is-complete': activeStep > 1, 'is-active': activeStep === 1 }">
          <div class="step-circle">
            <svg v-if="activeStep > 1" class="step-check" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round">
              <polyline points="20 6 9 17 4 12"/>
            </svg>
            <span v-else>1</span>
          </div>
          <span class="step-label">验证邮箱</span>
        </div>

        <!-- Step 2 -->
        <div class="progress-step" :class="{ 'is-complete': activeStep > 2, 'is-active': activeStep === 2 }">
          <div class="step-circle">
            <svg v-if="activeStep > 2" class="step-check" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round">
              <polyline points="20 6 9 17 4 12"/>
            </svg>
            <span v-else>2</span>
          </div>
          <span class="step-label">输入验证码</span>
        </div>

        <!-- Step 3 -->
        <div class="progress-step" :class="{ 'is-active': activeStep === 3 }">
          <div class="step-circle">
            <span>3</span>
          </div>
          <span class="step-label">设置密码</span>
        </div>
      </div>

      <!-- Form -->
      <form class="auth-form" novalidate @submit.prevent="handleSubmit" :class="{ 'shake': shaking }">
        <!-- Step 1: Send code -->
        <div class="step-panel" :class="{ 'slide-out-left': activeStep > 1, 'slide-in-right': activeStep === 1 && slideIn }" v-show="activeStep === 1">
          <div class="form-group">
            <div class="floating-label-wrap">
              <input
                id="email"
                v-model="form.email"
                type="email"
                placeholder=" "
                autocomplete="email"
                :disabled="countdown > 0"
                @focus="labelFloat('email', true)"
                @blur="labelFloat('email', false)"
                :class="{ 'has-value': form.email }"
              />
              <label for="email" :class="{ 'floating': form.email || focusedFields.email }">邮箱地址</label>
            </div>
          </div>

          <div class="form-group">
            <button
              type="button"
              class="send-code-btn"
              :disabled="countdown > 0 || sendingCode"
              @click="handleSendCode"
            >
              <span v-if="sendingCode">
                <svg class="btn-spinner" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
                  <path d="M12 2v4M12 18v4M4.93 4.93l2.83 2.83M16.24 16.24l2.83 2.83M2 12h4M18 12h4M4.93 19.07l2.83-2.83M16.24 7.76l2.83-2.83"/>
                </svg>
                发送中
              </span>
              <span v-else-if="countdown > 0">{{ countdown }}s</span>
              <span v-else>发送验证码</span>
            </button>
          </div>

          <p v-if="codeSent" class="code-sent-msg">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
              <polyline points="20 6 9 17 4 12"/>
            </svg>
            验证码已发送至 {{ form.email }}
          </p>

          <button
            type="button"
            class="auth-btn"
            :disabled="!form.email.trim() || sendingCode"
            @click="advanceToStep2"
          >
            下一步
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M5 12h14M12 5l7 7-7 7"/>
            </svg>
          </button>
        </div>

        <!-- Step 2: Verify code -->
        <div class="step-panel" :class="{ 'slide-out-left': activeStep > 2, 'slide-in-right': activeStep === 2 && slideIn }" v-show="activeStep === 2">
          <p class="step-hint">请输入发送至 <strong>{{ form.email }}</strong> 的 6 位验证码</p>

          <div class="form-group">
            <div class="floating-label-wrap">
              <input
                id="code"
                v-model="form.code"
                type="text"
                placeholder=" "
                maxlength="6"
                autocomplete="one-time-code"
                inputmode="numeric"
                pattern="[0-9]*"
                @focus="labelFloat('code', true)"
                @blur="labelFloat('code', false)"
                :class="{ 'has-value': form.code }"
              />
              <label for="code" :class="{ 'floating': form.code || focusedFields.code }">6 位验证码</label>
            </div>
          </div>

          <button
            type="button"
            class="back-btn"
            @click="goBackToStep1"
          >
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M19 12H5M12 19l-7-7 7-7"/>
            </svg>
            重新输入邮箱
          </button>

          <button
            type="submit"
            class="auth-btn"
            :disabled="form.code.length !== 6 || loading"
          >
            <span v-if="loading">
              <svg class="btn-spinner" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
                <path d="M12 2v4M12 18v4M4.93 4.93l2.83 2.83M16.24 16.24l2.83 2.83M2 12h4M18 12h4M4.93 19.07l2.83-2.83M16.24 7.76l2.83-2.83"/>
              </svg>
              验证中...
            </span>
            <span v-else>验证验证码</span>
          </button>
        </div>

        <!-- Step 3: Set username/password -->
        <div class="step-panel" :class="{ 'slide-in-right': activeStep === 3 && slideIn }" v-show="activeStep === 3">
          <div class="form-group">
            <div class="floating-label-wrap">
              <input
                id="username"
                v-model="form.username"
                type="text"
                placeholder=" "
                autocomplete="username"
                @focus="labelFloat('username', true)"
                @blur="labelFloat('username', false)"
                :class="{ 'has-value': form.username }"
              />
              <label for="username" :class="{ 'floating': form.username || focusedFields.username }">用户名</label>
            </div>
          </div>

          <div class="form-group">
            <div class="floating-label-wrap">
              <input
                id="password"
                v-model="form.password"
                :type="showPassword ? 'text' : 'password'"
                placeholder=" "
                minlength="8"
                autocomplete="new-password"
                @focus="labelFloat('password', true)"
                @blur="labelFloat('password', false)"
                :class="{ 'has-value': form.password }"
              />
              <label for="password" :class="{ 'floating': form.password || focusedFields.password }">密码（至少 8 位）</label>
              <button type="button" class="toggle-password" @click="showPassword = !showPassword" :aria-label="showPassword ? '隐藏密码' : '显示密码'">
                <svg v-if="!showPassword" class="eye-icon" xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/>
                  <circle cx="12" cy="12" r="3"/>
                </svg>
                <svg v-else class="eye-icon eye-icon--closed" xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24"/>
                  <line x1="1" y1="1" x2="23" y2="23"/>
                </svg>
              </button>
            </div>
          </div>

          <div class="form-group">
            <div class="floating-label-wrap">
              <input
                id="confirmPassword"
                v-model="form.confirmPassword"
                :type="showConfirmPassword ? 'text' : 'password'"
                placeholder=" "
                autocomplete="new-password"
                @focus="labelFloat('confirmPassword', true)"
                @blur="labelFloat('confirmPassword', false)"
                :class="{ 'has-value': form.confirmPassword }"
              />
              <label for="confirmPassword" :class="{ 'floating': form.confirmPassword || focusedFields.confirmPassword }">确认密码</label>
              <button type="button" class="toggle-password" @click="showConfirmPassword = !showConfirmPassword" :aria-label="showConfirmPassword ? '隐藏密码' : '显示密码'">
                <svg v-if="!showConfirmPassword" class="eye-icon" xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/>
                  <circle cx="12" cy="12" r="3"/>
                </svg>
                <svg v-else class="eye-icon eye-icon--closed" xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24"/>
                  <line x1="1" y1="1" x2="23" y2="23"/>
                </svg>
              </button>
            </div>
          </div>

          <button
            type="button"
            class="back-btn"
            @click="goBackToStep2"
          >
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M19 12H5M12 19l-7-7 7-7"/>
            </svg>
            上一步
          </button>

          <button
            type="submit"
            class="auth-btn"
            :disabled="loading"
          >
            <span v-if="loading">
              <svg class="btn-spinner" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
                <path d="M12 2v4M12 18v4M4.93 4.93l2.83 2.83M16.24 16.24l2.83 2.83M2 12h4M18 12h4M4.93 19.07l2.83-2.83M16.24 7.76l2.83-2.83"/>
              </svg>
              注册中...
            </span>
            <span v-else>完成注册</span>
          </button>
        </div>

        <!-- Error message -->
        <p v-if="validationError" class="error-msg">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <circle cx="12" cy="12" r="10"/>
            <line x1="12" y1="8" x2="12" y2="12"/>
            <line x1="12" y1="16" x2="12.01" y2="16"/>
          </svg>
          {{ validationError }}
        </p>
        <p v-if="errorMsg" class="error-msg">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <circle cx="12" cy="12" r="10"/>
            <line x1="12" y1="8" x2="12" y2="12"/>
            <line x1="12" y1="16" x2="12.01" y2="16"/>
          </svg>
          {{ errorMsg }}
        </p>
      </form>

      <p class="auth-switch">
        已有账号？
        <router-link to="/login" class="auth-link">立即登录</router-link>
      </p>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { sendCode, verifyCode, setPassword } from '../api/auth.js';
import { useAuthStore } from '../store/authStore.js';
import { gsap } from 'gsap';

const router = useRouter();
const authStore = useAuthStore();

const isLoaded = ref(false);
const activeStep = ref(1);
const slideIn = ref(true);
const shaking = ref(false);
const loading = ref(false);
const sendingCode = ref(false);
const countdown = ref(0);
const codeSent = ref(false);
const validationError = ref('');
const errorMsg = ref('');
const showPassword = ref(false);
const showConfirmPassword = ref(false);

const focusedFields = reactive({
  email: false,
  code: false,
  username: false,
  password: false,
  confirmPassword: false,
});

let verifyToken = '';
let countdownTimer = null;

const form = reactive({
  email: '',
  code: '',
  username: '',
  password: '',
  confirmPassword: '',
});

function labelFloat(field, value) {
  focusedFields[field] = value;
}

onMounted(() => {
  requestAnimationFrame(() => {
    setTimeout(() => {
      isLoaded.value = true;
    }, 80);
  });
});

function triggerShake() {
  shaking.value = true;
  setTimeout(() => { shaking.value = false; }, 600);
}

// ── Premium button interactions ──────────────────────────────────────────────
function createRipple(btn, e) {
  const rect = btn.getBoundingClientRect()
  const size = Math.max(rect.width, rect.height) * 1.4
  const x = e.clientX - rect.left - size / 2
  const y = e.clientY - rect.top - size / 2
  const ripple = document.createElement('span')
  ripple.style.cssText = `position:absolute;border-radius:50%;width:${size}px;height:${size}px;left:${x}px;top:${y}px;background:rgba(255,255,255,0.3);pointer-events:none;transform:scale(0)`
  btn.appendChild(ripple)
  gsap.to(ripple, { scale: 2.2, opacity: 0, duration: 0.6, ease: 'power2.out', onComplete: () => ripple.remove() })
}

function setupMagneticBtn(btn) {
  if (!btn) return
  btn.addEventListener('mousemove', (e) => {
    const rect = btn.getBoundingClientRect()
    const dx = (e.clientX - rect.left - rect.width / 2) / (rect.width / 2)
    const dy = (e.clientY - rect.top - rect.height / 2) / (rect.height / 2)
    gsap.to(btn, { x: dx * 5, y: dy * 3, scale: 1.02, duration: 0.22, ease: 'power2.out', overwrite: 'auto' })
  })
  btn.addEventListener('mouseleave', () => {
    gsap.to(btn, { x: 0, y: 0, scale: 1, duration: 0.55, ease: 'elastic.out(1, 0.5)' })
  })
  btn.addEventListener('mousedown', (e) => {
    gsap.to(btn, { scale: 0.97, duration: 0.1, ease: 'power2.in' })
    createRipple(btn, e)
  })
  btn.addEventListener('mouseup', () => {
    gsap.to(btn, { scale: 1.02, duration: 0.35, ease: 'elastic.out(1, 0.4)' })
  })
}

onMounted(() => {
  requestAnimationFrame(() => {
    setTimeout(() => {
      isLoaded.value = true;
    }, 80);
  });

  // Setup magnetic + ripple on all major buttons
  setupMagneticBtn(document.querySelector('.auth-btn'))
  setupMagneticBtn(document.querySelector('.back-btn'))
  setupMagneticBtn(document.querySelector('.send-code-btn'))

  // Auth link hover
  const authLink = document.querySelector('.auth-link')
  if (authLink) {
    authLink.addEventListener('mouseenter', () => gsap.to(authLink, { scale: 1.05, duration: 0.2, ease: 'power2.out' }))
    authLink.addEventListener('mouseleave', () => gsap.to(authLink, { scale: 1, duration: 0.25 }))
  }
})

function advanceToStep2() {
  if (!form.email.trim()) {
    validationError.value = '请输入邮箱地址';
    triggerShake();
    return;
  }
  if (!codeSent.value) {
    validationError.value = '请先发送验证码';
    triggerShake();
    return;
  }
  validationError.value = '';
  slideIn.value = false;
  setTimeout(() => {
    activeStep.value = 2;
    slideIn.value = true;
  }, 50);
}

function goBackToStep1() {
  slideIn.value = false;
  setTimeout(() => {
    activeStep.value = 1;
    slideIn.value = true;
  }, 50);
}

function goBackToStep2() {
  slideIn.value = false;
  setTimeout(() => {
    activeStep.value = 2;
    slideIn.value = true;
  }, 50);
}

async function handleSendCode() {
  validationError.value = '';
  errorMsg.value = '';
  if (!form.email.trim()) {
    errorMsg.value = '请输入邮箱地址';
    triggerShake();
    return;
  }
  sendingCode.value = true;
  try {
    await sendCode(form.email, 'register');
    codeSent.value = true;
    startCountdown(60);
  } catch (err) {
    errorMsg.value = err?.response?.data?.message || err.message || '发送失败，请稍后重试';
    triggerShake();
  } finally {
    sendingCode.value = false;
  }
}

async function handleSubmit() {
  validationError.value = '';
  errorMsg.value = '';

  if (activeStep.value === 1) {
    advanceToStep2();
    return;
  }

  if (activeStep.value === 2) {
    if (!form.code.trim() || form.code.length !== 6) {
      validationError.value = '请输入 6 位验证码';
      triggerShake();
      return;
    }
    loading.value = true;
    try {
      const verifyData = await verifyCode(form.email, form.code);
      verifyToken = verifyData?.verifyToken || verifyData?.token || verifyToken;
      slideIn.value = false;
      setTimeout(() => {
        activeStep.value = 3;
        slideIn.value = true;
      }, 50);
    } catch (err) {
      errorMsg.value = err?.response?.data?.message || err.message || '验证失败，请稍后重试';
      triggerShake();
    } finally {
      loading.value = false;
    }
    return;
  }

  if (activeStep.value === 3) {
    if (!form.username.trim()) {
      validationError.value = '用户名不能为空';
      triggerShake();
      return;
    }
    if (form.password !== form.confirmPassword) {
      validationError.value = '两次密码输入不一致';
      triggerShake();
      return;
    }
    if (form.password.length < 8) {
      validationError.value = '密码至少 8 位';
      triggerShake();
      return;
    }

    loading.value = true;
    try {
      const authData = await setPassword(verifyToken, form.username, form.password);
      authStore.storeAuthResponse(authData);
      router.push('/');
    } catch (err) {
      errorMsg.value = err?.response?.data?.message || err.message || '注册失败，请稍后重试';
      triggerShake();
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
@import url('https://fonts.googleapis.com/css2?family=Playfair+Display:wght@400;500;600;700&family=Inter:wght@300;400;500;600;700&family=JetBrains+Mono:wght@400;500&display=swap');

/* ─── Page ─────────────────────────────────────────────────────── */
.auth-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
  background: var(--color-bg);
  font-family: var(--font-body);
  padding: var(--space-6) var(--space-4);
}

/* ─── Gradient mesh ────────────────────────────────────────────── */
.gradient-mesh {
  position: absolute;
  inset: 0;
  z-index: 0;
  overflow: hidden;
  pointer-events: none;
}

.mesh-blob {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  opacity: 0.55;
  animation: mesh-drift 20s ease-in-out infinite alternate;
}

.mesh-blob-1 {
  width: 600px;
  height: 600px;
  background: radial-gradient(circle, rgba(110, 231, 183, 0.20) 0%, transparent 70%);
  top: -200px;
  left: -150px;
  animation-duration: 18s;
}

.mesh-blob-2 {
  width: 500px;
  height: 500px;
  background: radial-gradient(circle, rgba(52, 211, 153, 0.16) 0%, transparent 70%);
  bottom: -150px;
  right: -100px;
  animation-duration: 22s;
  animation-delay: -5s;
}

.mesh-blob-3 {
  width: 400px;
  height: 400px;
  background: radial-gradient(circle, rgba(110, 231, 183, 0.10) 0%, transparent 70%);
  top: 50%;
  left: 65%;
  transform: translate(-50%, -50%);
  animation-duration: 25s;
  animation-delay: -10s;
}

.mesh-blob-4 {
  width: 350px;
  height: 350px;
  background: radial-gradient(circle, rgba(251, 191, 36, 0.12) 0%, transparent 70%);
  top: 30%;
  right: 15%;
  transform: translate(-50%, -50%);
  animation-duration: 20s;
  animation-delay: -8s;
}

@keyframes mesh-drift {
  0%   { transform: translate(0, 0) scale(1); }
  33%  { transform: translate(30px, -20px) scale(1.05); }
  66%  { transform: translate(-20px, 30px) scale(0.95); }
  100% { transform: translate(10px, 10px) scale(1.02); }
}

/* ─── Floating shapes ─────────────────────────────────────────── */
.floating-shapes {
  position: absolute;
  inset: 0;
  z-index: 1;
  pointer-events: none;
}

.shape {
  position: absolute;
  border-radius: 50%;
  background: linear-gradient(135deg, rgba(110, 231, 183, 0.12), rgba(52, 211, 153, 0.08));
  animation: float 12s ease-in-out infinite;
}

.shape-1 {
  width: 180px;
  height: 180px;
  top: 12%;
  right: 8%;
  animation-duration: 14s;
}

.shape-2 {
  width: 120px;
  height: 120px;
  bottom: 20%;
  left: 5%;
  animation-duration: 10s;
  animation-delay: -3s;
  background: linear-gradient(135deg, rgba(52, 211, 153, 0.10), rgba(110, 231, 183, 0.06));
}

.shape-3 {
  width: 80px;
  height: 80px;
  top: 30%;
  left: 12%;
  animation-duration: 16s;
  animation-delay: -7s;
}

@keyframes float {
  0%, 100% { transform: translateY(0) rotate(0deg); }
  25%       { transform: translateY(-18px) rotate(5deg); }
  50%       { transform: translateY(-8px) rotate(-3deg); }
  75%       { transform: translateY(-24px) rotate(3deg); }
}

/* ─── Card ─────────────────────────────────────────────────────── */
.auth-card {
  position: relative;
  z-index: 2;
  width: 100%;
  max-width: 440px;
  background: var(--color-surface);
  border: 1px solid rgba(229, 229, 229, 0.6);
  border-top: 1px solid rgba(255, 255, 255, 0.8);
  border-radius: var(--radius-xl);
  padding: var(--space-10) var(--space-10);
  box-shadow: var(--shadow-xl), 0 0 80px rgba(110, 231, 183, 0.06);
  backdrop-filter: blur(20px);
  opacity: 0;
  transform: scale(0.95) translateY(16px);
  transition:
    opacity 0.6s cubic-bezier(0.16, 1, 0.3, 1) 200ms,
    transform 0.6s cubic-bezier(0.16, 1, 0.3, 1) 200ms;
}

.auth-card.is-visible {
  opacity: 1;
  transform: scale(1) translateY(0);
}

/* ─── Header ──────────────────────────────────────────────────── */
.auth-header {
  text-align: center;
  margin-bottom: var(--space-8);
}

.auth-title {
  font-family: var(--font-display);
  font-size: var(--text-3xl);
  font-weight: 700;
  color: var(--color-text);
  margin: 0 0 var(--space-2);
  letter-spacing: -0.02em;
  line-height: 1.2;
}

.auth-subtitle {
  font-size: var(--text-sm);
  color: var(--color-text-secondary);
  margin: 0;
  font-weight: 400;
}

/* ─── Progress track ──────────────────────────────────────────── */
.progress-track {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0;
  margin-bottom: var(--space-10);
  position: relative;
}

.progress-line {
  position: absolute;
  height: 2px;
  background: var(--color-border);
  top: 16px;
  z-index: 0;
}

.progress-line:first-of-type {
  left: calc(50% - 72px);
  width: 72px;
}

.progress-line:last-of-type {
  left: calc(50% + 0px);
  width: 72px;
}

.progress-line-fill {
  height: 100%;
  background: var(--color-accent);
  box-shadow: 0 0 8px rgba(110, 231, 183, 0.5);
  transition: width 0.6s cubic-bezier(0.16, 1, 0.3, 1);
}

.progress-step {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-2);
  z-index: 1;
  flex: 1;
  max-width: 100px;
}

.step-circle {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  border: 2px solid var(--color-border);
  background: var(--color-surface);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: var(--text-xs);
  font-weight: 700;
  color: var(--color-text-secondary);
  transition: border-color 0.4s, background 0.4s, color 0.4s, box-shadow 0.4s;
}

.progress-step.is-active .step-circle {
  border-color: var(--color-accent);
  color: var(--color-accent);
  background: var(--color-surface);
  box-shadow: 0 0 0 4px rgba(110, 231, 183, 0.12);
  animation: pulse-ring 2s cubic-bezier(0.4, 0, 0.6, 1) infinite;
}

.progress-step.is-complete .step-circle {
  border-color: var(--color-accent);
  background: var(--color-accent);
  color: #fff;
  box-shadow: inset 0 1px 3px rgba(0, 0, 0, 0.15), 0 2px 8px rgba(110, 231, 183, 0.3);
}

.step-label {
  font-size: 11px;
  font-weight: 500;
  color: var(--color-text-secondary);
  text-align: center;
  white-space: nowrap;
}

.progress-step.is-active .step-label {
  color: var(--color-accent);
  font-weight: 600;
}

@keyframes pulse-ring {
  0%, 100% { box-shadow: 0 0 0 4px rgba(110, 231, 183, 0.12); }
  50%       { box-shadow: 0 0 0 7px rgba(110, 231, 183, 0.06); }
}

/* ─── Step panels ─────────────────────────────────────────────── */
.step-panel {
  transition:
    opacity 0.4s cubic-bezier(0.16, 1, 0.3, 1),
    transform 0.4s cubic-bezier(0.16, 1, 0.3, 1);
}

.step-panel.slide-out-left {
  opacity: 0;
  transform: translateX(-32px);
}

.step-panel.slide-in-right {
  opacity: 1;
  transform: translateX(0);
}

.step-hint {
  font-size: var(--text-sm);
  color: var(--color-text-secondary);
  margin: 0 0 var(--space-6);
  text-align: center;
  line-height: 1.5;
}

.step-hint strong {
  color: var(--color-text);
  font-weight: 600;
}

/* ─── Form ─────────────────────────────────────────────────────── */
.auth-form {
  display: flex;
  flex-direction: column;
  gap: var(--space-5);
}

.auth-form.shake {
  animation: shake 0.5s cubic-bezier(0.36, 0.07, 0.19, 0.97);
}

@keyframes shake {
  0%, 100% { transform: translateX(0); }
  15%       { transform: translateX(-6px); }
  30%       { transform: translateX(5px); }
  45%       { transform: translateX(-5px); }
  60%       { transform: translateX(4px); }
  75%       { transform: translateX(-3px); }
  90%       { transform: translateX(2px); }
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

/* ─── Floating label inputs ───────────────────────────────────── */
.floating-label-wrap {
  position: relative;
}

.floating-label-wrap input {
  width: 100%;
  padding: var(--space-4) var(--space-4);
  padding-top: calc(var(--space-4) + 8px);
  border: 1.5px solid var(--color-border);
  border-radius: var(--radius-md);
  font-size: var(--text-base);
  font-family: var(--font-body);
  color: var(--color-text);
  background: var(--color-bg);
  outline: none;
  transition: border-color 0.2s, background 0.2s, box-shadow 0.2s;
  box-sizing: border-box;
}

.floating-label-wrap input:focus {
  border-color: var(--color-accent);
  background: var(--color-surface);
  box-shadow: 0 0 0 3px rgba(110, 231, 183, 0.10);
}

.floating-label-wrap input:disabled {
  background: #f3f4f6;
  cursor: not-allowed;
  color: var(--color-text-secondary);
}

.floating-label-wrap input.has-value,
.floating-label-wrap input:not(:placeholder-shown) {
  background: var(--color-surface);
}

.floating-label-wrap label {
  position: absolute;
  left: var(--space-4);
  top: 50%;
  transform: translateY(-50%);
  font-size: var(--text-base);
  color: var(--color-text-secondary);
  pointer-events: none;
  transition: top 0.2s cubic-bezier(0.16, 1, 0.3, 1), font-size 0.2s, color 0.2s, font-weight 0.2s;
  background: transparent;
  padding: 0 2px;
}

.floating-label-wrap label.floating {
  top: 8px;
  transform: translateY(0);
  font-size: 11px;
  color: var(--color-accent);
  font-weight: 600;
}

.toggle-password {
  position: absolute;
  right: var(--space-3);
  top: 50%;
  transform: translateY(-50%);
  background: none;
  border: none;
  cursor: pointer;
  color: var(--color-text-secondary);
  padding: var(--space-1);
  display: flex;
  align-items: center;
  border-radius: var(--radius-sm);
  transition: color 0.15s;
}

.toggle-password:hover {
  color: var(--color-accent);
}

.eye-icon {
  transition: opacity 0.15s;
}

.eye-icon--closed {
  opacity: 0.7;
}

/* ─── Send code button (light style) ──────────────────────────── */
.send-code-btn {
  width: 100%;
  padding: 12px 16px;
  background: #ffffff;
  color: #6ee7b7;
  border: 1.5px solid #6ee7b7;
  border-radius: var(--radius-md);
  font-size: 0.875rem;
  font-weight: 600;
  font-family: var(--font-body);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  transition: background 0.2s, color 0.2s, transform 0.15s, box-shadow 0.2s;
  box-shadow: 0 2px 8px rgba(110, 231, 183, 0.08);
  position: relative;
  overflow: hidden;
}

.send-code-btn::before {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(105deg, transparent 40%, rgba(110, 231, 183, 0.06) 50%, transparent 60%);
  transform: translateX(-100%);
  transition: transform 0.5s ease;
}

.send-code-btn:hover:not(:disabled)::before {
  transform: translateX(100%);
}

.send-code-btn:hover:not(:disabled) {
  background: #6ee7b7;
  color: #fff;
  transform: translateY(-1px);
  box-shadow: 0 4px 14px rgba(110, 231, 183, 0.25);
}

.send-code-btn:active:not(:disabled) {
  transform: scale(0.97);
  box-shadow: 0 1px 4px rgba(110, 231, 183, 0.15);
}

.send-code-btn:disabled {
  opacity: 0.55;
  cursor: not-allowed;
  background: #f3f4f6;
  color: #9ca3af;
  border-color: #d1d5db;
  box-shadow: none;
}

/* ─── Back button ─────────────────────────────────────────────── */
.back-btn {
  background: none;
  border: none;
  cursor: pointer;
  color: var(--color-text-secondary);
  font-size: var(--text-sm);
  font-family: var(--font-body);
  display: flex;
  align-items: center;
  gap: var(--space-1);
  padding: 0;
  transition: color 0.15s;
}

.back-btn:hover {
  color: var(--color-accent);
}

/* ─── Primary button (light style) ─────────────────────────────── */
.auth-btn {
  width: 100%;
  padding: 14px;
  background: #ffffff;
  color: #6ee7b7;
  border: 1.5px solid #6ee7b7;
  border-radius: var(--radius-md);
  font-size: 0.9375rem;
  font-weight: 600;
  font-family: var(--font-body);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-2);
  transition:
    background 0.2s,
    color 0.2s,
    transform 0.15s cubic-bezier(0.34, 1.56, 0.64, 1),
    box-shadow 0.2s;
  box-shadow: 0 2px 8px rgba(110, 231, 183, 0.08);
  position: relative;
  overflow: hidden;
}

.auth-btn::before {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(105deg, transparent 40%, rgba(110, 231, 183, 0.06) 50%, transparent 60%);
  transform: translateX(-100%);
  transition: transform 0.5s ease;
}

.auth-btn:hover:not(:disabled)::before {
  transform: translateX(100%);
}

.auth-btn:hover:not(:disabled) {
  background: #6ee7b7;
  color: #fff;
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(110, 231, 183, 0.3);
}

.auth-btn:active:not(:disabled) {
  transform: scale(0.97);
  box-shadow: 0 2px 8px rgba(110, 231, 183, 0.2);
}

.auth-btn:disabled {
  opacity: 0.45;
  cursor: not-allowed;
  background: #f3f4f6;
  color: #9ca3af;
  border-color: #d1d5db;
  box-shadow: none;
}

/* ─── Spinner ─────────────────────────────────────────────────── */
.btn-spinner {
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to   { transform: rotate(360deg); }
}

/* ─── Messages ─────────────────────────────────────────────────── */
.code-sent-msg {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  font-size: var(--text-xs);
  color: var(--color-success);
  margin: 0;
  font-weight: 500;
}

.error-msg {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  color: var(--color-error);
  font-size: var(--text-sm);
  margin: 0;
  padding: var(--space-3) var(--space-4);
  background: #fef2f2;
  border-radius: var(--radius-md);
  border: 1px solid #fecaca;
  font-weight: 500;
}

/* ─── Auth switch ─────────────────────────────────────────────── */
.auth-switch {
  text-align: center;
  font-size: var(--text-sm);
  color: var(--color-text-secondary);
  margin: var(--space-6) 0 0;
  font-family: var(--font-body);
}

.auth-link {
  color: var(--color-accent);
  text-decoration: none;
  font-weight: 600;
  transition: opacity 0.15s;
}

.auth-link:hover {
  opacity: 0.8;
  text-decoration: underline;
}

/* ─── Responsive ──────────────────────────────────────────────── */
@media (max-width: 480px) {
  .auth-card {
    padding: var(--space-8) var(--space-6);
    border-radius: var(--radius-lg);
  }

  .progress-track {
    margin-bottom: var(--space-8);
  }

  .progress-line:first-of-type {
    left: calc(50% - 60px);
    width: 60px;
  }

  .progress-line:last-of-type {
    left: calc(50% + 0px);
    width: 60px;
  }

  .step-circle {
    width: 28px;
    height: 28px;
    font-size: 10px;
  }

  .step-label {
    font-size: 10px;
  }

  .auth-title {
    font-size: var(--text-2xl);
  }
}
</style>
