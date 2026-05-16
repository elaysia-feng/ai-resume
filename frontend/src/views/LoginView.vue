<template>
  <div class="auth-page">
    <!-- Animated gradient mesh background (cohesive with lobby) -->
    <div class="gradient-mesh" aria-hidden="true">
      <div class="mesh-blob mesh-blob-1"></div>
      <div class="mesh-blob mesh-blob-2"></div>
      <div class="mesh-blob mesh-blob-3"></div>
      <div class="mesh-blob mesh-blob-4"></div>
    </div>

    <div class="auth-card" :class="{ shake: isShaking }">
      <h1 class="auth-title">登录</h1>
      <p class="auth-subtitle">欢迎回到 AI Resume Forge</p>

      <!-- Mode toggle tabs -->
      <div class="mode-tabs">
        <button
          class="mode-tab"
          :class="{ active: loginMode === 'password' }"
          @click="loginMode = 'password'"
          type="button"
        >
          密码登录
        </button>
        <button
          class="mode-tab"
          :class="{ active: loginMode === 'code' }"
          @click="loginMode = 'code'"
          type="button"
        >
          验证码登录
        </button>
        <!-- Sliding indicator -->
        <div class="tab-indicator" :class="loginMode"></div>
      </div>

      <!-- Password login form -->
      <form v-if="loginMode === 'password'" class="auth-form" @submit.prevent="handleLogin">
        <div class="form-group">
          <div class="float-label">
            <input
              id="email"
              v-model="form.email"
              type="email"
              placeholder=" "
              required
              autocomplete="email"
              class="float-input"
            />
            <label for="email" class="float-label-text">邮箱</label>
          </div>
        </div>

        <div class="form-group">
          <div class="float-label">
            <input
              id="password"
              v-model="form.password"
              :type="showPassword ? 'text' : 'password'"
              placeholder=" "
              required
              autocomplete="current-password"
              class="float-input"
            />
            <label for="password" class="float-label-text">密码</label>
          </div>
          <button type="button" class="toggle-password" @click="showPassword = !showPassword" aria-label="切换密码可见性">
            <svg v-if="!showPassword" xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/>
              <circle cx="12" cy="12" r="3"/>
            </svg>
            <svg v-else xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="eye-off">
              <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24"/>
              <line x1="1" y1="1" x2="23" y2="23"/>
            </svg>
          </button>
        </div>

        <p v-if="errorMsg" class="error-msg" role="alert">{{ errorMsg }}</p>

        <button type="submit" class="auth-btn" :disabled="loading" :class="{ loading: loading }">
          <span v-if="loading" class="btn-spinner" aria-hidden="true"></span>
          <span v-else>登录</span>
        </button>
      </form>

      <!-- Code login form -->
      <form v-else class="auth-form" @submit.prevent="handleLogin">
        <div class="form-group">
          <div class="float-label">
            <input
              id="codeEmail"
              v-model="form.email"
              type="email"
              placeholder=" "
              required
              autocomplete="email"
              class="float-input"
            />
            <label for="codeEmail" class="float-label-text">邮箱</label>
          </div>
        </div>

        <div class="form-group">
          <div class="float-label">
            <input
              id="codeInput"
              v-model="form.code"
              type="text"
              placeholder=" "
              required
              maxlength="6"
              autocomplete="one-time-code"
              class="float-input"
            />
            <label for="codeInput" class="float-label-text">验证码</label>
          </div>
          <button
            type="button"
            class="send-code-btn"
            :disabled="countdown > 0 || sendingCode"
            @click="handleSendCode"
          >
            <span v-if="sendingCode">发送中...</span>
            <span v-else-if="countdown > 0">{{ countdown }}s</span>
            <span v-else>发送验证码</span>
          </button>
          <p v-if="codeSentMsg" class="code-sent-msg">{{ codeSentMsg }}</p>
        </div>

        <p v-if="errorMsg" class="error-msg" role="alert">{{ errorMsg }}</p>

        <button type="submit" class="auth-btn" :disabled="loading" :class="{ loading: loading }">
          <span v-if="loading" class="btn-spinner" aria-hidden="true"></span>
          <span v-else>登录</span>
        </button>
      </form>

      <p class="auth-switch">
        还没有账号？
        <router-link to="/register" class="auth-link">立即注册</router-link>
      </p>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from '../store/authStore.js';
import { sendCode } from '../api/auth.js';
import { gsap } from 'gsap';

const router = useRouter();
const authStore = useAuthStore();

const loginMode = ref('password');
const loading = ref(false);
const sendingCode = ref(false);
const countdown = ref(0);
const codeSentMsg = ref('');
const errorMsg = ref('');
const showPassword = ref(false);
const isShaking = ref(false);
let countdownTimer = null;

const form = reactive({
  email: '',
  password: '',
  code: '',
});

async function handleSendCode() {
  if (!form.email.trim()) {
    errorMsg.value = '请输入邮箱地址';
    return;
  }
  sendingCode.value = true;
  errorMsg.value = '';
  codeSentMsg.value = '';
  try {
    await sendCode(form.email, 'login');
    codeSentMsg.value = '验证码已发送';
    startCountdown(60);
  } catch (err) {
    errorMsg.value = err?.response?.data?.message || err.message || '发送失败，请稍后重试';
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
    router.push('/');
  } catch (err) {
    errorMsg.value = err?.response?.data?.message || err.message || '登录失败';
    triggerShake();
  } finally {
    loading.value = false;
  }
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

function triggerShake() {
  isShaking.value = true;
  setTimeout(() => {
    isShaking.value = false;
  }, 600);
}

// ── Premium button interactions ──────────────────────────────────────────────
onMounted(() => {
  // Ripple factory
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

  // Auth submit button — magnetic + elastic
  const authBtn = document.querySelector('.auth-btn')
  if (authBtn) {
    authBtn.addEventListener('mousemove', (e) => {
      const rect = authBtn.getBoundingClientRect()
      const dx = (e.clientX - rect.left - rect.width / 2) / (rect.width / 2)
      const dy = (e.clientY - rect.top - rect.height / 2) / (rect.height / 2)
      gsap.to(authBtn, { x: dx * 5, y: dy * 3, scale: 1.02, duration: 0.25, ease: 'power2.out', overwrite: 'auto' })
    })
    authBtn.addEventListener('mouseleave', () => {
      gsap.to(authBtn, { x: 0, y: 0, scale: 1, duration: 0.55, ease: 'elastic.out(1, 0.5)' })
    })
    authBtn.addEventListener('mousedown', (e) => {
      gsap.to(authBtn, { scale: 0.97, duration: 0.1, ease: 'power2.in' })
      createRipple(authBtn, e)
    })
    authBtn.addEventListener('mouseup', () => {
      gsap.to(authBtn, { scale: 1.02, duration: 0.35, ease: 'elastic.out(1, 0.4)' })
    })
  }

  // Send code button
  const sendBtn = document.querySelector('.send-code-btn')
  if (sendBtn) {
    sendBtn.addEventListener('mousemove', (e) => {
      const rect = sendBtn.getBoundingClientRect()
      gsap.to(sendBtn, {
        x: ((e.clientX - rect.left - rect.width / 2) / rect.width) * 4,
        y: ((e.clientY - rect.top - rect.height / 2) / rect.height) * 3,
        scale: 1.03, duration: 0.2, ease: 'power2.out', overwrite: 'auto'
      })
    })
    sendBtn.addEventListener('mouseleave', () => {
      gsap.to(sendBtn, { x: 0, y: 0, scale: 1, duration: 0.45, ease: 'elastic.out(1, 0.6)' })
    })
    sendBtn.addEventListener('mousedown', (e) => {
      gsap.to(sendBtn, { scale: 0.95, duration: 0.08 })
      createRipple(sendBtn, e)
    })
    sendBtn.addEventListener('mouseup', () => {
      gsap.to(sendBtn, { scale: 1.03, duration: 0.3, ease: 'elastic.out(1, 0.4)' })
    })
  }

  // Mode tabs — smooth press feedback
  document.querySelectorAll('.mode-tab').forEach(tab => {
    tab.addEventListener('mousedown', () => {
      gsap.to(tab, { scale: 0.93, duration: 0.1, ease: 'power2.in' })
    })
    tab.addEventListener('mouseup', () => {
      gsap.to(tab, { scale: 1, duration: 0.25, ease: 'elastic.out(1, 0.5)' })
    })
    tab.addEventListener('mouseleave', () => {
      gsap.to(tab, { scale: 1, duration: 0.2 })
    })
  })

  // Auth link — underline draw
  const authLink = document.querySelector('.auth-link')
  if (authLink) {
    authLink.addEventListener('mouseenter', () => gsap.to(authLink, { scale: 1.05, duration: 0.2, ease: 'power2.out' }))
    authLink.addEventListener('mouseleave', () => gsap.to(authLink, { scale: 1, duration: 0.25 }))
  }

  // Card subtle float on mousemove (parallax lift)
  const card = document.querySelector('.auth-card')
  if (card) {
    document.addEventListener('mousemove', (e) => {
      const cx = window.innerWidth / 2; const cy = window.innerHeight / 2
      const dx = (e.clientX - cx) / cx; const dy = (e.clientY - cy) / cy
      gsap.to(card, { rotateY: dx * 3, rotateX: -dy * 2, duration: 1.2, ease: 'power1.out' })
    })
  }
})
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Playfair+Display:wght@400;500;600;700&family=Inter:wght@300;400;500;600;700&family=JetBrains+Mono:wght@400;500&display=swap');

/* ---- Page shell ---- */
.auth-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
  background: var(--color-bg, #fafaf9);
  font-family: var(--font-body, 'Inter', -apple-system, sans-serif);
}

/* ---- Atmospheric vignette behind card ---- */
.gradient-mesh::after {
  content: '';
  position: absolute;
  inset: 0;
  background: radial-gradient(ellipse 60% 50% at 50% 50%, rgba(110, 231, 183, 0.05) 0%, transparent 70%);
  pointer-events: none;
}

/* ---- Animated gradient mesh background ---- */
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
  opacity: 0.6;
  animation: mesh-drift 20s ease-in-out infinite alternate;
}

.mesh-blob-1 {
  width: 600px;
  height: 600px;
  background: radial-gradient(circle, rgba(110, 231, 183, 0.18) 0%, transparent 70%);
  top: -200px;
  left: -150px;
  animation-duration: 18s;
}

.mesh-blob-2 {
  width: 500px;
  height: 500px;
  background: radial-gradient(circle, rgba(52, 211, 153, 0.15) 0%, transparent 70%);
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
  left: 60%;
  transform: translate(-50%, -50%);
  animation-duration: 25s;
  animation-delay: -10s;
}

.mesh-blob-4 {
  width: 350px;
  height: 350px;
  background: radial-gradient(circle, rgba(110, 231, 183, 0.08) 0%, transparent 70%);
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

/* ---- Card ---- */
.auth-card {
  position: relative;
  z-index: 1;
  width: 100%;
  max-width: 420px;
  background: var(--color-surface, #ffffff);
  border-radius: var(--radius-lg, 16px);
  padding: 44px 40px;
  border: 1px solid rgba(0, 0, 0, 0.05);
  border-top: 1px solid rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  box-shadow:
    var(--shadow-lg, 0 12px 32px rgba(0, 0, 0, 0.10)),
    0 4px 8px rgba(0, 0, 0, 0.06),
    inset 0 1px 0 rgba(255, 255, 255, 0.8);

  /* Entrance animation — 200ms delay for deliberate feel */
  opacity: 0;
  transform: scale(0.95);
  animation: card-in var(--duration-slower, 500ms) var(--ease-out-expo, cubic-bezier(0.16, 1, 0.3, 1)) 200ms forwards;
}

@keyframes card-in {
  to {
    opacity: 1;
    transform: scale(1);
  }
}

/* ---- Shake animation on error ---- */
.auth-card.shake {
  animation: shake 0.5s ease-in-out;
}

@keyframes shake {
  0%, 100% { transform: translateX(0) scale(1); }
  15%       { transform: translateX(-8px) scale(1); }
  30%       { transform: translateX(8px) scale(1); }
  45%       { transform: translateX(-8px) scale(1); }
  60%       { transform: translateX(8px) scale(1); }
  75%       { transform: translateX(-4px) scale(1); }
  90%       { transform: translateX(4px) scale(1); }
}

/* ---- Typography ---- */
.auth-title {
  font-family: var(--font-display, 'Playfair Display', Georgia, serif);
  font-size: var(--text-2xl, 1.5rem);
  font-weight: 700;
  color: var(--color-text, #1a1a1a);
  margin: 0 0 6px;
  text-align: center;
  letter-spacing: -0.01em;
}

.auth-subtitle {
  font-family: var(--font-body, 'Inter', sans-serif);
  font-size: var(--text-sm, 0.875rem);
  color: var(--color-text-secondary, #6b7280);
  text-align: center;
  margin: 0 0 28px;
}

/* ---- Mode tabs ---- */
.mode-tabs {
  display: flex;
  position: relative;
  background: var(--color-accent-soft, #d1fae5);
  border-radius: var(--radius-md, 10px);
  padding: 4px;
  margin-bottom: 28px;
  border: 1px solid rgba(110, 231, 183, 0.08);
}

/* Sliding indicator */
.tab-indicator {
  position: absolute;
  top: 4px;
  left: 4px;
  width: calc(50% - 4px);
  height: calc(100% - 8px);
  background: var(--color-accent, #6ee7b7);
  border-radius: calc(var(--radius-md, 10px) - 2px);
  box-shadow: 0 2px 8px rgba(110, 231, 183, 0.3);
  transition: transform var(--duration-normal, 300ms) var(--ease-out-expo, cubic-bezier(0.16, 1, 0.3, 1));
}

.tab-indicator.code {
  transform: translateX(100%);
}

.mode-tab {
  flex: 1;
  padding: 9px 12px;
  border: none;
  border-radius: calc(var(--radius-md, 10px) - 2px);
  font-family: var(--font-body, 'Inter', sans-serif);
  font-size: var(--text-sm, 0.875rem);
  font-weight: 600;
  cursor: pointer;
  background: transparent;
  color: var(--color-text-secondary, #6b7280);
  transition: color var(--duration-fast, 150ms);
  position: relative;
  z-index: 1;
}

.mode-tab.active {
  color: #fff;
}

.mode-tab:not(.active):hover {
  color: var(--color-text, #374151);
}

/* ---- Floating label inputs ---- */
.auth-form {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.form-group {
  display: flex;
  flex-direction: column;
  position: relative;
}

.float-label {
  position: relative;
}

.float-input {
  width: 100%;
  padding: 22px 14px 8px;
  border: 1.5px solid var(--color-border, #e5e5e5);
  border-radius: var(--radius-md, 10px);
  font-family: var(--font-body, 'Inter', sans-serif);
  font-size: var(--text-base, 1rem);
  color: var(--color-text, #1a1a1a);
  background: var(--color-surface, #ffffff);
  outline: none;
  transition:
    border-color var(--duration-fast, 150ms),
    box-shadow var(--duration-fast, 150ms);
  box-sizing: border-box;
}

.float-input:focus {
  border-color: var(--color-accent, #6ee7b7);
  box-shadow: 0 0 0 3px rgba(110, 231, 183, 0.15);
}

.float-label-text {
  position: absolute;
  left: 14px;
  top: 50%;
  transform: translateY(-50%);
  font-family: var(--font-body, 'Inter', sans-serif);
  font-size: var(--text-base, 1rem);
  color: var(--color-text-secondary, #9ca3af);
  pointer-events: none;
  transition:
    transform var(--duration-normal, 300ms) var(--ease-out-expo, cubic-bezier(0.16, 1, 0.3, 1)),
    font-size var(--duration-normal, 300ms) var(--ease-out-expo, cubic-bezier(0.16, 1, 0.3, 1)),
    color var(--duration-normal, 300ms) var(--ease-out-expo, cubic-bezier(0.16, 1, 0.3, 1));
  transform-origin: left top;
  background: transparent;
}

.float-input:focus ~ .float-label-text,
.float-input:not(:placeholder-shown) ~ .float-label-text {
  transform: translateY(-100%) scale(0.75);
  color: var(--color-accent, #6ee7b7);
}

.float-input:focus ~ .float-label-text {
  color: var(--color-accent, #6ee7b7);
}

/* ---- Password toggle ---- */
.password-field {
  position: relative;
  display: flex;
}

.toggle-password {
  position: absolute;
  right: 12px;
  top: 50%;
  transform: translateY(-50%);
  background: none;
  border: none;
  cursor: pointer;
  color: var(--color-text-secondary, #9ca3af);
  padding: 4px;
  display: flex;
  align-items: center;
  transition: color var(--duration-fast, 150ms);
}

.toggle-password:hover {
  color: var(--color-accent, #6ee7b7);
}

.toggle-password svg {
  transition: transform var(--duration-normal, 300ms) var(--ease-out-expo, cubic-bezier(0.16, 1, 0.3, 1));
}

.toggle-password:active svg {
  transform: rotate(25deg) scale(0.9);
}

/* ---- Send code button ---- */
.send-code-btn {
  margin-top: 8px;
  padding: 10px 16px;
  background: var(--color-accent, #6ee7b7);
  color: #fff;
  border: none;
  border-radius: var(--radius-md, 10px);
  font-family: var(--font-body, 'Inter', sans-serif);
  font-size: var(--text-sm, 0.875rem);
  font-weight: 600;
  cursor: pointer;
  white-space: nowrap;
  transition:
    background var(--duration-fast, 150ms),
    transform var(--duration-fast, 150ms),
    box-shadow var(--duration-fast, 150ms);
  box-shadow: 0 2px 8px rgba(110, 231, 183, 0.25);
}

.send-code-btn:hover:not(:disabled) {
  background: var(--color-accent-hover, #34d399);
  transform: translateY(-1px);
  box-shadow: 0 4px 14px rgba(110, 231, 183, 0.35);
}

.send-code-btn:active:not(:disabled) {
  transform: scale(0.97);
}

.send-code-btn:disabled {
  opacity: 0.55;
  cursor: not-allowed;
  box-shadow: none;
}

.code-sent-msg {
  font-size: var(--text-xs, 0.75rem);
  color: var(--color-success, #22c55e);
  margin: 6px 0 0;
  font-weight: 500;
}

/* ---- Error message ---- */
.error-msg {
  color: var(--color-error, #ef4444);
  font-size: var(--text-sm, 0.875rem);
  margin: 0;
  padding: 10px 14px;
  background: #fef2f2;
  border-radius: var(--radius-md, 10px);
  border: 1px solid #fecaca;
  font-weight: 500;
}

/* ---- Submit button (light style) ---- */
.auth-btn {
  width: 100%;
  padding: 13px;
  background: #ffffff;
  color: #6ee7b7;
  border: 1.5px solid #6ee7b7;
  border-radius: var(--radius-md, 10px);
  font-family: var(--font-body, 'Inter', sans-serif);
  font-size: var(--text-base, 1rem);
  font-weight: 600;
  cursor: pointer;
  transition:
    background var(--duration-fast, 150ms),
    color var(--duration-fast, 150ms),
    transform var(--duration-fast, 150ms),
    box-shadow var(--duration-fast, 150ms),
    opacity var(--duration-fast, 150ms);
  margin-top: 4px;
  box-shadow: 0 2px 8px rgba(110, 231, 183, 0.08);
  position: relative;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 48px;
}

/* Gradient shimmer sweep on hover */
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
  box-shadow: 0 6px 20px rgba(110, 231, 183, 0.30);
  transform: translateY(-1px);
}

.auth-btn:active:not(:disabled) {
  transform: scale(0.97);
  box-shadow: 0 2px 8px rgba(110, 231, 183, 0.20);
}

.auth-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  background: #f3f4f6;
  color: #9ca3af;
  border-color: #d1d5db;
  box-shadow: none;
}

/* ---- Spinner ---- */
.btn-spinner {
  display: inline-block;
  width: 18px;
  height: 18px;
  border: 2.5px solid rgba(255, 255, 255, 0.35);
  border-top-color: #fff;
  border-radius: 50%;
  animation: spin 0.65s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

/* ---- Auth switch link ---- */
.auth-switch {
  text-align: center;
  font-size: var(--text-sm, 0.875rem);
  color: var(--color-text-secondary, #6b7280);
  margin: 24px 0 0;
  font-family: var(--font-body, 'Inter', sans-serif);
}

.auth-link {
  color: var(--color-accent, #6ee7b7);
  text-decoration: none;
  font-weight: 600;
  transition: color var(--duration-fast, 150ms);
}

.auth-link:hover {
  color: var(--color-accent-hover, #34d399);
  text-decoration: underline;
}

/* ---- Responsive ---- */
@media (max-width: 480px) {
  .auth-card {
    margin: 0 16px;
    padding: 36px 28px;
  }
}
</style>
