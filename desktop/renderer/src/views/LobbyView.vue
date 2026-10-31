<template>
  <div class="lobby">
    <main class="lobby__inner">
      <header class="lobby__top">
        <div class="lobby__brand">
          <AppBrandMark :size="36" />
          <span>AI Resume Forge</span>
        </div>
        <nav class="lobby__actions" aria-label="账号入口">
          <router-link class="ui-btn ui-btn--outline" to="/login">登录</router-link>
          <router-link class="ui-btn ui-btn--primary" to="/register">注册账号</router-link>
        </nav>
      </header>

      <section class="lobby__lead">
        <div class="lobby__lead-copy">
          <h1>本地简历与求职准备工作台</h1>
          <p>
            简历编辑、AI 对话、模板导出和模拟面试都可以分别开始。
            简历与历史记录保存在本机；使用 AI 时，当前任务所需内容会发送到设置中配置的模型服务。
          </p>
        </div>
        <aside class="lobby__note">
          <AppIcon name="desktop" :size="19" />
          <p>本机数据与模型服务边界清楚。AI 修改建议由你审阅后再决定是否应用。</p>
        </aside>
      </section>

      <section class="lobby__work" aria-labelledby="lobby-work-title">
        <header class="lobby__work-head">
          <h2 id="lobby-work-title">工作入口</h2>
          <p>每项能力都可独立使用。</p>
        </header>

        <article class="lobby__feature">
          <span class="lobby__feature-icon" aria-hidden="true"><AppIcon name="file-text" :size="21" /></span>
          <div>
            <h3>简历编辑与导出</h3>
            <p>逐模块整理经历，保存版本，并预览模板和导出文件。</p>
          </div>
        </article>

        <article class="lobby__feature">
          <span class="lobby__feature-icon" aria-hidden="true"><AppIcon name="chat-circle" :size="21" /></span>
          <div>
            <h3>AI 对话与岗位分析</h3>
            <p>围绕简历和目标岗位交流，查看分析与修改建议。</p>
          </div>
        </article>

        <article class="lobby__feature">
          <span class="lobby__feature-icon" aria-hidden="true"><AppIcon name="microphone" :size="21" /></span>
          <div>
            <h3>模拟面试</h3>
            <p>选择简历与岗位场景，逐轮练习并查看反馈。</p>
          </div>
        </article>
      </section>
    </main>
  </div>
</template>
<script setup>
import AppIcon from '../components/common/AppIcon.vue';
import AppBrandMark from '../components/common/AppBrandMark.vue';
</script>
<style scoped>
.lobby {
  min-height: 100%;
  background: var(--ui-bg-app);
  color: var(--ui-fg-primary);
}

.lobby__inner {
  width: 100%;
  max-width: 1280px;
  margin: 0 auto;
  padding: 28px 56px 48px;
}

.lobby__top {
  display: flex;
  min-height: 56px;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  border-bottom: 1px solid var(--ui-grid-line-strong);
}

.lobby__brand {
  display: inline-flex;
  min-width: 0;
  align-items: center;
  gap: 11px;
  color: var(--ui-fg-primary);
  font-family: var(--ui-font-display);
  font-size: 17px;
  font-weight: 650;
  letter-spacing: -0.02em;
}

.lobby__actions {
  display: flex;
  flex: 0 0 auto;
  align-items: center;
  gap: 10px;
}

.lobby__lead {
  display: grid;
  grid-template-columns: minmax(0, 1.45fr) minmax(240px, 0.7fr);
  align-items: end;
  gap: 48px;
  padding: 72px 0 64px;
  border-bottom: 1px solid var(--ui-grid-line-strong);
}

.lobby__lead-copy h1 {
  max-width: 17ch;
  margin: 0;
  font-family: var(--ui-font-display);
  font-size: clamp(38px, 4.5vw, 56px);
  font-weight: 650;
  letter-spacing: -0.03em;
  line-height: 1.12;
}

.lobby__lead-copy p {
  max-width: 72ch;
  margin: 20px 0 0;
  color: var(--ui-fg-secondary);
  font-size: 16px;
  line-height: 1.75;
}

.lobby__note {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 18px 20px;
  border: 1px solid var(--ui-grid-line);
  background: var(--ui-bg-canvas);
}

.lobby__note .app-icon {
  flex: 0 0 auto;
  margin-top: 2px;
  color: var(--ui-accent);
}

.lobby__note p {
  margin: 0;
  color: var(--ui-fg-secondary);
  font-size: 14px;
  line-height: 1.65;
}

.lobby__work {
  padding-top: 34px;
}

.lobby__work-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 20px;
  padding-bottom: 15px;
  border-bottom: 1px solid var(--ui-grid-line-strong);
}

.lobby__work-head h2 {
  margin: 0;
  font-size: 19px;
  font-weight: 650;
}

.lobby__work-head p {
  margin: 0;
  color: var(--ui-fg-muted);
  font-size: 13px;
}

.lobby__feature {
  display: grid;
  grid-template-columns: 56px minmax(0, 1fr);
  align-items: center;
  gap: 18px;
  min-height: 104px;
  border-bottom: 1px solid var(--ui-grid-line);
}

.lobby__feature-icon {
  display: grid;
  width: 38px;
  height: 38px;
  place-items: center;
  border: 1px solid var(--ui-grid-line);
  color: var(--ui-fg-primary);
}

.lobby__feature h3 {
  margin: 0;
  font-size: 17px;
  font-weight: 600;
}

.lobby__feature p {
  margin: 5px 0 0;
  color: var(--ui-fg-secondary);
  font-size: 14px;
  line-height: 1.5;
}

@media (max-width: 780px) {
  .lobby__inner {
    padding: 22px 24px 36px;
  }

  .lobby__lead {
    grid-template-columns: minmax(0, 1fr);
    gap: 24px;
    padding: 44px 0;
  }

  .lobby__work-head {
    align-items: flex-start;
    flex-direction: column;
    gap: 5px;
  }

  .lobby__actions {
    gap: 6px;
  }
}

@media (max-width: 480px) {
  .lobby__brand span:last-child {
    font-size: 15px;
  }

  .lobby__actions .ui-btn {
    padding-right: 10px;
    padding-left: 10px;
  }
}
</style>
