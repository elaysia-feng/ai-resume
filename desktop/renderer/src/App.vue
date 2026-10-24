<template>
  <div id="app-root">
    <!-- Subtle decorative gradient blob -->
    <div class="bg-blob" aria-hidden="true"></div>

    <!-- Clean white navbar with green accents -->
    <header class="app-nav">
      <div class="nav-inner">
        <router-link to="/" class="nav-logo">
          <span class="logo-icon">🌿</span>
          <span class="logo-text">AI Resume Forge</span>
        </router-link>
        <nav class="nav-links">
          <router-link to="/dashboard" class="nav-link">我的简历</router-link>
          <router-link to="/dashboard/templates" class="nav-link">模板市场</router-link>
          <a href="#" class="nav-link">帮助中心</a>
        </nav>
        <button class="nav-cta">开始使用</button>
      </div>
    </header>

    <!-- Main content area -->
    <main class="app-main">
      <router-view />
    </main>
  </div>
</template>

<script>
export default {
  name: 'App'
}
</script>

<style scoped>
@import './styles/green-theme.css';

#app-root {
  position: relative;
  min-height: 100vh;
  background: #ffffff;
  overflow-x: hidden;
}

/* ========================================
   DECORATIVE BACKGROUND BLOB
   ======================================== */
.bg-blob {
  position: fixed;
  top: -200px;
  right: -200px;
  width: 600px;
  height: 600px;
  background: radial-gradient(circle, rgba(34, 197, 94, 0.08) 0%, transparent 60%);
  border-radius: 50%;
  pointer-events: none;
  z-index: 0;
  filter: blur(40px);
}

/* ========================================
   NAVBAR — CLEAN WHITE WITH GREEN
   ======================================== */
.app-nav {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 100;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border-bottom: 1px solid rgba(34, 197, 94, 0.15);
  box-shadow: 0 1px 12px rgba(0, 0, 0, 0.06);
}

.nav-inner {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 1.5rem;
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

/* ========================================
   LOGO
   ======================================== */
.nav-logo {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  text-decoration: none;
  transition: transform 0.2s ease;
}

.nav-logo:hover {
  transform: scale(1.03);
  text-decoration: none;
}

.logo-icon {
  font-size: 1.5rem;
  filter: drop-shadow(0 0 6px rgba(34, 197, 94, 0.5));
}

.logo-text {
  font-size: 1.125rem;
  font-weight: 700;
  color: #16a34a;
  letter-spacing: 0.01em;
  text-shadow: 0 1px 2px rgba(34, 197, 94, 0.2);
}

/* ========================================
   NAV LINKS — GREEN HOVER UNDERLINE
   ======================================== */
.nav-links {
  display: flex;
  align-items: center;
  gap: 0.25rem;
}

.nav-link {
  position: relative;
  background: none;
  border: none;
  cursor: pointer;
  padding: 0.5rem 0.875rem;
  font-size: 0.875rem;
  font-weight: 500;
  color: #6b7280;
  border-radius: 6px;
  text-decoration: none;
  transition: color 0.2s ease;
}

.nav-link::after {
  content: '';
  position: absolute;
  bottom: 4px;
  left: 0.875rem;
  right: 0.875rem;
  height: 2px;
  background: #22c55e;
  border-radius: 1px;
  transform: scaleX(0);
  transform-origin: left;
  transition: transform 0.2s ease;
}

.nav-link:hover {
  color: #16a34a;
}

.nav-link:hover::after {
  transform: scaleX(1);
}

.nav-link.router-link-active {
  color: #16a34a;
  font-weight: 600;
}

.nav-link.router-link-active::after {
  transform: scaleX(1);
}

/* ========================================
   CTA BUTTON — SOLID GREEN
   ======================================== */
.nav-cta {
  background: #22c55e;
  border: none;
  border-radius: 7px;
  cursor: pointer;
  padding: 0.5rem 1.25rem;
  font-size: 0.875rem;
  font-weight: 600;
  color: #ffffff;
  transition: background 0.2s ease, box-shadow 0.2s ease, transform 0.2s ease;
  box-shadow: 0 2px 8px rgba(34, 197, 94, 0.3);
}

.nav-cta:hover {
  background: #16a34a;
  box-shadow: 0 4px 16px rgba(34, 197, 94, 0.4);
  transform: translateY(-1px);
}

/* ========================================
   MAIN CONTENT
   ======================================== */
.app-main {
  position: relative;
  z-index: 1;
  min-height: 100vh;
  padding-top: 64px;
  background: #ffffff;
}

/* ========================================
   RESPONSIVE
   ======================================== */
@media (max-width: 768px) {
  .nav-links .nav-link:nth-child(n+3) {
    display: none;
  }

  .nav-cta {
    padding: 0.4rem 0.875rem;
    font-size: 0.8125rem;
  }
}

@media (max-width: 480px) {
  .nav-inner {
    padding: 0 1rem;
  }

  .logo-text {
    font-size: 1rem;
  }

  .nav-links {
    gap: 0;
  }

  .nav-link {
    padding: 0.5rem 0.625rem;
    font-size: 0.8125rem;
  }
}
</style>
