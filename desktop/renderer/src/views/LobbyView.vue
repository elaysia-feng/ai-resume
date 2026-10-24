<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { gsap } from 'gsap'

const router = useRouter()
const containerRef = ref(null)
const logoRef = ref(null)
const headlineRef = ref(null)
const taglineRef = ref(null)
const ctaRef = ref(null)
const featuresRef = ref(null)
const orb1Ref = ref(null)
const orb2Ref = ref(null)
const orb3Ref = ref(null)

function goToRegister() {
  router.push('/register')
}

function goToLogin() {
  router.push('/login')
}

onMounted(() => {
  const tl = gsap.timeline({ defaults: { ease: 'power3.out' } })

  // Set initial states
  gsap.set([logoRef.value, taglineRef.value, ctaRef.value, featuresRef.value], {
    opacity: 0, y: 30
  })
  gsap.set(orb1Ref.value, { opacity: 0, scale: 0.5 })
  gsap.set(orb2Ref.value, { opacity: 0, scale: 0.5 })
  gsap.set(orb3Ref.value, { opacity: 0, scale: 0.5 })

  // Split headline chars manually
  const headline = headlineRef.value
  const text = headline.textContent
  headline.textContent = ''
  const chars = text.split('').map(char => {
    const span = document.createElement('span')
    span.textContent = char === ' ' ? '\u00A0' : char
    span.style.display = 'inline-block'
    span.style.opacity = '0'
    span.style.transform = 'translateY(40px) rotateX(-40deg)'
    headline.appendChild(span)
    return span
  })

  // Orb entrance
  tl.to(orb1Ref.value, { opacity: 1, scale: 1, duration: 2, ease: 'power2.out' }, 0)
    .to(orb2Ref.value, { opacity: 1, scale: 1, duration: 2.2, ease: 'power2.out' }, 0.3)
    .to(orb3Ref.value, { opacity: 1, scale: 1, duration: 1.8, ease: 'power2.out' }, 0.15)

  // Logo — gentle float
  tl.to(logoRef.value, { opacity: 1, y: 0, duration: 0.8 }, 0.4)
  gsap.to(logoRef.value, { y: -6, duration: 3.5, repeat: -1, yoyo: true, ease: 'sine.inOut', delay: 2 })

  // Headline chars stagger
  tl.to(chars, {
    opacity: 1,
    y: 0,
    rotateX: 0,
    stagger: 0.04,
    duration: 0.6,
    ease: 'back.out(1.2)'
  }, 0.6)

  // Tagline
  tl.to(taglineRef.value, { opacity: 1, y: 0, duration: 0.7 }, 1.1)

  // CTA buttons
  tl.to(ctaRef.value, { opacity: 1, y: 0, duration: 0.7 }, 1.3)

  // Features
  tl.to(featuresRef.value, { opacity: 1, y: 0, duration: 0.7 }, 1.5)

  // Ambient orb floating animation (continuous)
  gsap.to(orb1Ref.value, {
    y: '-=30', x: '+=20', duration: 6, repeat: -1, yoyo: true, ease: 'sine.inOut'
  })
  gsap.to(orb2Ref.value, {
    y: '+=25', x: '-=15', duration: 8, repeat: -1, yoyo: true, ease: 'sine.inOut', delay: 1
  })
  gsap.to(orb3Ref.value, {
    y: '+=20', x: '+=25', duration: 7, repeat: -1, yoyo: true, ease: 'sine.inOut', delay: 2
  })

  // Mouse parallax on orbs
  const onMouseMove = (e) => {
    const { clientX, clientY } = e
    const cx = window.innerWidth / 2
    const cy = window.innerHeight / 2
    const dx = (clientX - cx) / cx
    const dy = (clientY - cy) / cy

    gsap.to(orb1Ref.value, { x: dx * 20, y: dy * 15, duration: 1, ease: 'power1.out', overwrite: 'auto' }, 0)
    gsap.to(orb2Ref.value, { x: dx * -15, y: dy * -12, duration: 1.2, ease: 'power1.out', overwrite: 'auto' }, 0)
    gsap.to(orb3Ref.value, { x: dx * 12, y: dy * 18, duration: 1.1, ease: 'power1.out', overwrite: 'auto' }, 0)
  }

  window.addEventListener('mousemove', onMouseMove)

  // ── Button interactions ──────────────────────────────────────────
  const btnPrimary = document.querySelector('.btn-primary')
  const btnSecondary = document.querySelector('.btn-secondary')

  // Ripple factory — appends to .btn-content (the overflow:hidden wrapper)
  function createRipple(btn, e) {
    const content = btn.querySelector('.btn-content')
    const rect = content.getBoundingClientRect()
    const size = Math.max(rect.width, rect.height) * 1.6
    const x = e.clientX - rect.left - size / 2
    const y = e.clientY - rect.top - size / 2

    const ripple = document.createElement('span')
    ripple.className = 'ripple'
    ripple.style.cssText = `width:${size}px;height:${size}px;left:${x}px;top:${y}px`
    content.appendChild(ripple)

    gsap.fromTo(ripple,
      { scale: 0, opacity: 0.5 },
      { scale: 2.5, opacity: 0, duration: 0.7, ease: 'power2.out',
        onComplete: () => ripple.remove() }
    )
  }

  // Primary button — magnetic pull + elastic squish
  if (btnPrimary) {
    btnPrimary.addEventListener('mousemove', (e) => {
      const rect = btnPrimary.getBoundingClientRect()
      const cx = rect.left + rect.width / 2
      const cy = rect.top + rect.height / 2
      const dx = (e.clientX - cx) / (rect.width / 2)
      const dy = (e.clientY - cy) / (rect.height / 2)

      gsap.to(btnPrimary, {
        x: dx * 6,
        y: dy * 4,
        scale: 1.03,
        duration: 0.3,
        ease: 'power2.out',
        overwrite: 'auto'
      })

      // Glow trail — tracking cursor position
      gsap.to(btnPrimary, {
        '--glow-x': `${e.clientX - rect.left}px`,
        '--glow-y': `${e.clientY - rect.top}px`,
        duration: 0.2
      })
    })

    btnPrimary.addEventListener('mouseleave', () => {
      gsap.to(btnPrimary, {
        x: 0, y: 0, scale: 1,
        duration: 0.6, ease: 'elastic.out(1, 0.5)'
      })
    })

    btnPrimary.addEventListener('mousedown', (e) => {
      gsap.to(btnPrimary, { scale: 0.96, duration: 0.1, ease: 'power2.in' })
      createRipple(btnPrimary, e)
    })

    btnPrimary.addEventListener('mouseup', () => {
      gsap.to(btnPrimary, { scale: 1.03, duration: 0.4, ease: 'elastic.out(1, 0.4)' })
    })
  }

  // Secondary button — subtle lift + shimmer border
  if (btnSecondary) {
    btnSecondary.addEventListener('mousemove', (e) => {
      const rect = btnSecondary.getBoundingClientRect()
      const cx = rect.left + rect.width / 2
      const cy = rect.top + rect.height / 2
      const dx = (e.clientX - cx) / (rect.width / 2)
      const dy = (e.clientY - cy) / (rect.height / 2)

      gsap.to(btnSecondary, {
        x: dx * 3,
        y: dy * 2,
        scale: 1.02,
        borderColor: 'rgba(110, 231, 183, 0.6)',
        boxShadow: '0 8px 32px rgba(16,217,160,0.2)',
        duration: 0.25,
        ease: 'power2.out',
        overwrite: 'auto'
      })
    })

    btnSecondary.addEventListener('mouseleave', () => {
      gsap.to(btnSecondary, {
        x: 0, y: 0, scale: 1,
        borderColor: 'rgba(255,255,255,0.12)',
        boxShadow: 'none',
        duration: 0.5, ease: 'power2.out'
      })
    })

    btnSecondary.addEventListener('mousedown', (e) => {
      gsap.to(btnSecondary, { scale: 0.97, duration: 0.1, ease: 'power2.in' })
      createRipple(btnSecondary, e)
    })

    btnSecondary.addEventListener('mouseup', () => {
      gsap.to(btnSecondary, { scale: 1.02, duration: 0.35, ease: 'elastic.out(1, 0.5)' })
    })
  }

  // Arrow icon float on primary button
  const arrow = document.querySelector('.btn-arrow')
  if (arrow) {
    btnPrimary.addEventListener('mouseenter', () => {
      gsap.to(arrow, { x: 5, y: -2, rotation: 8, duration: 0.35, ease: 'back.out(2)' })
    })
    btnPrimary.addEventListener('mouseleave', () => {
      gsap.to(arrow, { x: 0, y: 0, rotation: 0, duration: 0.4, ease: 'power2.in' })
    })
  }

  // Feature pills — subtle entrance stagger on scroll
  const pills = document.querySelectorAll('.feature-pill')
  pills.forEach((pill, i) => {
    gsap.fromTo(pill,
      { opacity: 0, y: 10 },
      {
        opacity: 1, y: 0, duration: 0.5, delay: 1.6 + i * 0.1,
        ease: 'power2.out'
      }
    )
  })
})
</script>

<template>
  <div class="lobby" ref="containerRef">
    <!-- Ambient floating orbs -->
    <div class="orb orb-1" ref="orb1Ref" aria-hidden="true"></div>
    <div class="orb orb-2" ref="orb2Ref" aria-hidden="true"></div>
    <div class="orb orb-3" ref="orb3Ref" aria-hidden="true"></div>

    <!-- Subtle grid pattern -->
    <div class="grid-pattern" aria-hidden="true"></div>

    <!-- Content -->
    <main class="lobby-content">

      <!-- Logo icon -->
      <div class="logo-wrap" ref="logoRef">
        <svg width="56" height="56" viewBox="0 0 48 48" fill="none" xmlns="http://www.w3.org/2000/svg">
          <rect width="48" height="48" rx="13" fill="#6ee7b7"/>
          <path d="M14 16h20v3H17v5h15v3H17v7h-3V16z" fill="white"/>
          <circle cx="36" cy="32" r="6" fill="white" opacity="0.2"/>
          <path d="M33 32l2.5 2.5L39 29" stroke="white" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
        </svg>
      </div>

      <!-- Headline with char-split animation -->
      <h1 class="headline" ref="headlineRef">Resume Forge</h1>

      <!-- Tagline -->
      <p class="tagline" ref="taglineRef">AI-powered resumes that get you hired</p>

      <!-- CTA buttons -->
      <div class="cta-group" ref="ctaRef">
        <button class="btn btn-primary" @click="goToRegister">
          <span class="btn-content">
            Get Started
            <svg class="btn-arrow" width="16" height="16" viewBox="0 0 16 16" fill="none">
              <path d="M3 8h10M9 4l4 4-4 4" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
          </span>
        </button>
        <button class="btn btn-secondary" @click="goToLogin">
          <span class="btn-content">Sign In</span>
        </button>
      </div>

      <!-- Feature pills -->
      <div class="feature-strip" ref="featuresRef">
        <div class="feature-pill">
          <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
            <path d="M8 2L2 5l6 3 6-3-6-3z" stroke="#6ee7b7" stroke-width="1.2" stroke-linecap="round" stroke-linejoin="round"/>
            <path d="M2 10l6 3 6-3M2 7.5l6 3 6-3" stroke="#6ee7b7" stroke-width="1.2" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
          <span>AI-Powered</span>
        </div>
        <div class="pill-sep" aria-hidden="true"></div>
        <div class="feature-pill">
          <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
            <rect x="2" y="2" width="12" height="12" rx="2.5" stroke="#6ee7b7" stroke-width="1.2"/>
            <path d="M5 7h6M5 10h4" stroke="#6ee7b7" stroke-width="1.2" stroke-linecap="round"/>
          </svg>
          <span>Professional Templates</span>
        </div>
        <div class="pill-sep" aria-hidden="true"></div>
        <div class="feature-pill">
          <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
            <circle cx="8" cy="8" r="5.5" stroke="#6ee7b7" stroke-width="1.2"/>
            <circle cx="8" cy="8" r="2.5" stroke="#6ee7b7" stroke-width="1.2"/>
          </svg>
          <span>Real-time Preview</span>
        </div>
        <div class="pill-sep" aria-hidden="true"></div>
        <div class="feature-pill">
          <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
            <path d="M13 9v3a1.5 1.5 0 01-1.5 1.5H4.5A1.5 1.5 0 013 12V9M11 4.5L8 3 5 4.5M5.5 4.5V7.5" stroke="#6ee7b7" stroke-width="1.2" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
          <span>Export Anywhere</span>
        </div>
      </div>
    </main>
  </div>
</template>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Playfair+Display:wght@400;500;600;700&family=Inter:wght@300;400;500;600;700&display=swap');

/* === Page shell: deep dark background === */
.lobby {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #09090e;
  position: relative;
  overflow: hidden;
  font-family: 'Inter', -apple-system, sans-serif;
}

/* === Ambient orbs === */
.orb {
  position: absolute;
  border-radius: 50%;
  pointer-events: none;
  will-change: transform, opacity;
}

.orb-1 {
  width: 600px;
  height: 600px;
  background: radial-gradient(circle, rgba(110, 231, 183, 0.22) 0%, transparent 65%);
  top: -200px;
  left: -150px;
  filter: blur(60px);
}

.orb-2 {
  width: 500px;
  height: 500px;
  background: radial-gradient(circle, rgba(52, 211, 153, 0.15) 0%, transparent 65%);
  bottom: -100px;
  right: -80px;
  filter: blur(70px);
}

.orb-3 {
  width: 350px;
  height: 350px;
  background: radial-gradient(circle, rgba(110, 231, 183, 0.12) 0%, transparent 65%);
  top: 40%;
  right: 20%;
  filter: blur(50px);
}

/* === Subtle grid === */
.grid-pattern {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(255,255,255,0.025) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255,255,255,0.025) 1px, transparent 1px);
  background-size: 60px 60px;
  pointer-events: none;
  mask-image: radial-gradient(ellipse 70% 70% at 50% 50%, black 30%, transparent 80%);
}

/* === Content === */
.lobby-content {
  position: relative;
  z-index: 2;
  text-align: center;
  padding: 4rem 2rem;
  max-width: 680px;
  width: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
}

/* === Logo === */
.logo-wrap {
  margin-bottom: 2.5rem;
  filter: drop-shadow(0 0 30px rgba(110, 231, 183, 0.4));
}

/* === Headline === */
.headline {
  font-family: 'Playfair Display', Georgia, serif;
  font-size: clamp(3.5rem, 9vw, 7rem);
  font-weight: 700;
  color: #ffffff;
  line-height: 1.0;
  letter-spacing: -0.03em;
  margin: 0 0 1.5rem;
  perspective: 600px;
}

/* === Tagline === */
.tagline {
  font-size: clamp(1rem, 2vw, 1.2rem);
  font-weight: 400;
  color: rgba(255, 255, 255, 0.45);
  margin: 0 0 3rem;
  letter-spacing: 0.04em;
  font-family: 'Inter', sans-serif;
}

/* === CTA group === */
.cta-group {
  display: flex;
  align-items: center;
  gap: 1rem;
  margin-bottom: 3.5rem;
  flex-wrap: wrap;
  justify-content: center;
}

/* === Buttons === */
.btn {
  font-family: 'Inter', -apple-system, sans-serif;
  font-size: 0.9375rem;
  font-weight: 600;
  padding: 0.8125rem 1.75rem;
  border-radius: 8px;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  border: none;
  transition: transform 0.2s cubic-bezier(0.34, 1.2, 0.64, 1), box-shadow 0.2s ease, background 0.15s ease;
  text-decoration: none;
  position: relative;
}

.btn-content {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  position: relative;
  z-index: 1;
  overflow: hidden;
}

.btn-primary {
  background: #ffffff;
  color: #6ee7b7;
  border: 1.5px solid #6ee7b7;
  box-shadow: 0 2px 12px rgba(110, 231, 183, 0.12);
  position: relative;
}

.btn-primary:hover {
  transform: translateY(-3px) scale(1.02);
  box-shadow: 0 8px 30px rgba(110, 231, 183, 0.3), 0 0 0 1px rgba(110, 231, 183, 0.4);
  background: #6ee7b7;
  color: #fff;
}

.btn-primary:active {
  transform: translateY(-1px) scale(0.99);
}

/* Shimmer sweep */
.btn-primary::after {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(105deg, transparent 30%, rgba(255,255,255,0.2) 50%, transparent 70%);
  transform: translateX(-120%);
  transition: transform 0.5s ease;
}

.btn-primary:hover::after {
  transform: translateX(120%);
}

.btn-secondary {
  background: rgba(110, 231, 183, 0.07);
  color: rgba(110, 231, 183, 0.9);
  border: 1px solid rgba(110, 231, 183, 0.25);
  backdrop-filter: blur(8px);
}

.btn-secondary:hover {
  transform: translateY(-2px);
  background: rgba(110, 231, 183, 0.12);
  border-color: rgba(110, 231, 183, 0.7);
  box-shadow: 0 4px 20px rgba(110, 231, 183, 0.25);
}

.btn-secondary:active {
  transform: translateY(0) scale(0.98);
}

.btn-arrow {
  transition: transform 0.2s ease;
}

/* === Ripple === */
.ripple {
  position: absolute;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.35);
  pointer-events: none;
  transform: scale(0);
  animation: none;
}

/* === Glow tracking pseudo === */
.btn-primary::before {
  content: '';
  position: absolute;
  width: 120px;
  height: 120px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(255,255,255,0.18) 0%, transparent 70%);
  left: calc(var(--glow-x, 50%) - 60px);
  top: calc(var(--glow-y, 50%) - 60px);
  pointer-events: none;
  opacity: 0;
  transition: opacity 0.2s ease;
}
.btn-primary:hover::before {
  background: radial-gradient(circle, rgba(16,217,160,0.18) 0%, transparent 70%);
  opacity: 1;
}

.btn-primary:hover .btn-arrow {
  transform: translateX(3px);
}

/* === Feature strip === */
.feature-strip {
  display: flex;
  align-items: center;
  gap: 0;
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 100px;
  padding: 0.5rem 0.5rem;
  background: rgba(255, 255, 255, 0.03);
  backdrop-filter: blur(12px);
}

.feature-pill {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  padding: 0.3rem 0.875rem;
}

.feature-pill span {
  font-size: 0.8rem;
  font-weight: 500;
  color: rgba(255, 255, 255, 0.5);
  white-space: nowrap;
}

.pill-sep {
  width: 1px;
  height: 16px;
  background: rgba(255, 255, 255, 0.1);
  flex-shrink: 0;
}

/* === Responsive === */
@media (max-width: 640px) {
  .feature-strip {
    flex-direction: column;
    border-radius: 16px;
    gap: 0.25rem;
    padding: 0.5rem;
  }

  .pill-sep {
    width: 40px;
    height: 1px;
  }

  .feature-pill {
    padding: 0.25rem 0.5rem;
  }

  .cta-group {
    flex-direction: column;
    width: 100%;
  }

  .btn {
    width: 100%;
    justify-content: center;
  }

  .headline {
    font-size: clamp(2.8rem, 14vw, 5rem);
  }

  .lobby-content {
    padding: 3rem 1.5rem;
  }
}
</style>
