<template>
  <!--
    访客首页：说明产品是什么、能做什么，并给出登录 / 注册入口。
    克制的信息密度，不做营销 hero，也不再放装饰性插画。
  -->
  <div class="lobby">
    <main class="lobby__inner">
      <div class="lobby__brand">
        <span class="lobby__mark" aria-hidden="true">
          <svg viewBox="0 0 48 48" width="32" height="32" fill="none">
            <rect width="48" height="48" rx="13" fill="currentColor" />
            <path d="M15 15h19v4H19v4h13v4H19v6h-4V15z" fill="var(--ui-bg-surface)" />
          </svg>
        </span>
        <span class="lobby__name">AI Resume Forge</span>
      </div>

      <h1 class="lobby__title">本地运行的简历与面试工作台</h1>
      <p class="lobby__desc">
        用 AI 对话起草简历、套用模板、练习面试，再用 5star 法则体检每一段经历。
        所有内容都在你自己的电脑上完成，简历数据不出本机。
      </p>

      <div class="lobby__actions">
        <router-link class="ui-btn ui-btn--primary ui-btn--lg" to="/register">注册账号</router-link>
        <router-link class="ui-btn ui-btn--outline ui-btn--lg" to="/login">登录</router-link>
      </div>
      <p class="lobby__hint">首次使用先注册，注册后即可进入工作台；已有账号直接登录。</p>

      <!-- 功能概览 -->
      <ul class="lobby__features">
        <li class="ui-card lobby__feature">
          <span class="lobby__feature-icon" aria-hidden="true">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round">
              <path d="M21 11.5a8.5 8.5 0 0 1-8.5 8.5c-1.6 0-3.1-.45-4.3-1.2L3 20.5l1.7-4.8a8.5 8.5 0 1 1 16.3-4.2z" />
            </svg>
          </span>
          <h2 class="lobby__feature-title">AI 对话写简历</h2>
          <p class="lobby__feature-desc">把项目经历说给 AI 听，它改写成简历语言，并按岗位 JD 调整侧重。</p>
        </li>

        <li class="ui-card lobby__feature">
          <span class="lobby__feature-icon" aria-hidden="true">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round">
              <path d="M4 4h16v16H4z" />
              <path d="M4 9h16" />
              <path d="M9 20V9" />
            </svg>
          </span>
          <h2 class="lobby__feature-title">模板市场</h2>
          <p class="lobby__feature-desc">多套版式模板可预览、可筛选，套用后随时导出 PDF。</p>
        </li>

        <li class="ui-card lobby__feature">
          <span class="lobby__feature-icon" aria-hidden="true">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round">
              <path d="M12 3a3 3 0 0 0-3 3v6a3 3 0 0 0 6 0V6a3 3 0 0 0-3-3z" />
              <path d="M19 11v1a7 7 0 0 1-14 0v-1" />
              <path d="M12 19v3" />
              <path d="M8 22h8" />
            </svg>
          </span>
          <h2 class="lobby__feature-title">模拟面试</h2>
          <p class="lobby__feature-desc">按目标岗位出题，逐轮点评应答思路与表达，并给出改进建议。</p>
        </li>

        <li class="ui-card lobby__feature">
          <span class="lobby__feature-icon" aria-hidden="true">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round">
              <path d="M12 3.5l2.6 5.6 6.1.8-4.5 4.2 1.2 6.1-5.4-3-5.4 3 1.2-6.1-4.5-4.2 6.1-.8z" />
            </svg>
          </span>
          <h2 class="lobby__feature-title">5star 法则评分</h2>
          <p class="lobby__feature-desc">按情境、任务、行动、结果、量化逐条体检，指出还缺哪一环。</p>
        </li>
      </ul>
    </main>
  </div>
</template>

<style scoped>
/* ========================================
   访客欢迎页：单列居中，信息克制
   ======================================== */
.lobby {
  min-height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: var(--ui-space-10) var(--ui-space-8);
  background: var(--ui-bg-app);
  color: var(--ui-fg-primary);
  font-family: var(--ui-font-sans);
}

.lobby__inner {
  width: 100%;
  max-width: 880px;
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
}

/* ---- 品牌 ---- */
.lobby__brand {
  display: inline-flex;
  align-items: center;
  gap: var(--ui-space-3);
  margin-bottom: var(--ui-space-6);
}

.lobby__mark {
  display: flex;
  color: var(--ui-accent);
}

.lobby__name {
  font-size: var(--ui-text-md);
  font-weight: 700;
  color: var(--ui-fg-primary);
}

/* ---- 标题与说明 ---- */
.lobby__title {
  margin: 0;
  font-size: var(--ui-text-3xl);
  font-weight: 700;
  line-height: var(--ui-line-tight);
  color: var(--ui-fg-primary);
  text-wrap: pretty;
}

.lobby__desc {
  margin: var(--ui-space-4) 0 0;
  max-width: 40rem;
  font-size: var(--ui-text-md);
  line-height: var(--ui-line-relaxed);
  color: var(--ui-fg-secondary);
  text-wrap: pretty;
}

/* ---- 入口 ---- */
.lobby__actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: var(--ui-space-3);
  margin-top: var(--ui-space-8);
}

.lobby__hint {
  margin: var(--ui-space-4) 0 0;
  font-size: var(--ui-text-base);
  color: var(--ui-fg-muted);
}

/* ---- 功能概览 ---- */
.lobby__features {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: var(--ui-space-4);
  width: 100%;
  margin: var(--ui-space-10) 0 0;
  padding: 0;
  list-style: none;
}

.lobby__feature {
  padding: var(--ui-space-5);
  text-align: left;
}

.lobby__feature-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  margin-bottom: var(--ui-space-3);
  border-radius: var(--ui-radius-md);
  background: var(--ui-accent-soft);
  color: var(--ui-fg-accent);
}

.lobby__feature-icon svg {
  width: 18px;
  height: 18px;
}

.lobby__feature-title {
  margin: 0;
  font-size: var(--ui-text-base);
  font-weight: 600;
  color: var(--ui-fg-primary);
}

.lobby__feature-desc {
  margin: var(--ui-space-2) 0 0;
  font-size: var(--ui-text-sm);
  line-height: var(--ui-line-normal);
  color: var(--ui-fg-secondary);
}

@media (max-width: 720px) {
  .lobby {
    padding: var(--ui-space-8) var(--ui-space-5);
  }

  .lobby__title {
    font-size: var(--ui-text-2xl);
  }

  .lobby__actions {
    width: 100%;
    flex-direction: column;
  }
}
</style>
