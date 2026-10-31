<template>
  <!--
    /dashboard 的内容容器。
    全局导航与用户操作由应用壳统一提供，各页面自己的标题和工具栏贴近工作内容。
    这里只承载子路由内容，不再重复画一套导航。
  -->
  <div class="dashboard-layout">
    <router-view v-slot="{ Component, route }">
      <component :is="Component" :key="route.name" />
    </router-view>
  </div>
</template>

<style scoped>
/*
  滚动方案：内容区高度 = 视口 − 壳层内容区上下内边距，
  滚动由本容器承担（.ui-scroll 提供细滚动条），壳层 .app-content 不再产生第二条滚动条。
*/
.dashboard-layout {
  min-height: 100%;
}
</style>
