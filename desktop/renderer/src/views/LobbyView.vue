<template>
  <!--
    访客首页：说明产品是什么、能做什么，并给出登录 / 注册入口。
    克制的信息密度，不做营销 hero，也不再放装饰性插画。
  -->
  <div class="lobby">
    <main class="lobby__inner">
      <div class="lobby__brand">
        <span class="lobby__mark" aria-hidden="true">
          <AppIcon name="file-text" :size="22" />
        </span>
        <span class="lobby__name">AI Resume Forge</span>
      </div>

      <h1 class="ui-page-title lobby__title">本地简历与求职准备工作台</h1>
      <p class="ui-page-subtitle lobby__desc">
        简历编辑、AI 对话、模板导出和模拟面试可以分别开始。简历与历史记录保存在本机；使用 AI 时，当前任务所需内容会发送到设置中配置的模型服务。
      </p>

      <div class="lobby__actions">
        <router-link class="ui-btn ui-btn--primary ui-btn--lg" to="/register">
          <AppIcon name="user-plus" :size="16" />
          注册账号
        </router-link>
        <router-link class="ui-btn ui-btn--outline ui-btn--lg" to="/login">
          <AppIcon name="sign-in" :size="16" />
          登录
        </router-link>
      </div>
      <p class="lobby__hint">首次使用先注册，注册后即可进入工作台；已有账号直接登录。</p>

      <!-- 功能概览 -->
      <ul class="lobby__features">
        <li class="ui-panel lobby__feature">
          <span class="ui-icon-tile lobby__feature-icon" aria-hidden="true">
            <AppIcon name="chat-circle" :size="18" />
          </span>
          <h2 class="lobby__feature-title">AI 对话写简历</h2>
          <p class="lobby__feature-desc">把项目经历说给 AI 听，它改写成简历语言，并按岗位 JD 调整侧重。</p>
        </li>

        <li class="ui-panel lobby__feature">
          <span class="ui-icon-tile lobby__feature-icon" aria-hidden="true">
            <AppIcon name="layout" :size="18" />
          </span>
          <h2 class="lobby__feature-title">模板市场</h2>
          <p class="lobby__feature-desc">多套版式模板可预览、可筛选，套用后随时导出 PDF。</p>
        </li>

        <li class="ui-panel lobby__feature">
          <span class="ui-icon-tile lobby__feature-icon" aria-hidden="true">
            <AppIcon name="microphone" :size="18" />
          </span>
          <h2 class="lobby__feature-title">模拟面试</h2>
          <p class="lobby__feature-desc">按目标岗位出题，逐轮点评应答思路与表达，并给出改进建议。</p>
        </li>

        <li class="ui-panel lobby__feature">
          <span class="ui-icon-tile lobby__feature-icon" aria-hidden="true">
            <AppIcon name="star" :size="18" />
          </span>
          <h2 class="lobby__feature-title">5star 法则评分</h2>
          <p class="lobby__feature-desc">按情境、任务、行动、结果、量化逐条体检，指出还缺哪一环。</p>
        </li>
      </ul>
    </main>
  </div>
</template>

<script setup>
import AppIcon from '../components/common/AppIcon.vue';
</script>

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
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  border-radius: var(--ui-radius-lg);
  background: var(--ui-accent-soft);
  border: 1px solid var(--ui-accent-border);
  color: var(--ui-fg-accent);
}

.lobby__name {
  font-size: var(--ui-text-md);
  font-weight: 700;
  letter-spacing: var(--ui-tracking-snug);
  color: var(--ui-fg-primary);
}

/* ---- 标题与说明 ---- */
.lobby__title {
  text-wrap: pretty;
}

.lobby__desc {
  margin-top: var(--ui-space-4);
  max-width: 40rem;
  font-size: var(--ui-text-md);
  line-height: var(--ui-line-relaxed);
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
  margin-bottom: var(--ui-space-3);
}

.lobby__feature-title {
  margin: 0;
  font-size: var(--ui-text-base);
  font-weight: 600;
  letter-spacing: var(--ui-tracking-snug);
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

  .lobby__actions {
    width: 100%;
    flex-direction: column;
  }
}
</style>
