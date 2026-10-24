<template>
  <!--
    /dashboard 的内容容器。
    侧边栏、顶栏、用户信息与退出登录统一由应用壳（src/App.vue）提供，
    这里只承载子路由内容，不再重复画一套导航。
  -->
  <div class="dashboard-layout ui-scroll">
    <router-view />
  </div>
</template>

<style scoped>
/*
  滚动方案：内容区高度 = 视口 − 顶栏 − 壳层内容区上下内边距，
  滚动由本容器承担（.ui-scroll 提供细滚动条），壳层 .app-content 不再产生第二条滚动条。
*/
.dashboard-layout {
  height: calc(100vh - var(--ui-header-height) - var(--ui-space-8) * 2);
  overflow-y: auto;
  overflow-x: hidden;
}

/* 窄窗口下壳层内边距收窄，这里同步，避免底部留白 */
@media (max-width: 1100px) {
  .dashboard-layout {
    height: calc(100vh - var(--ui-header-height) - var(--ui-space-6) * 2);
  }
}
</style>
