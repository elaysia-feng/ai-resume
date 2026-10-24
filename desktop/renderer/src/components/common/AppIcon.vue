<template>
  <svg
    class="app-icon"
    :class="{ 'app-icon--spin': spin }"
    viewBox="0 0 256 256"
    :width="size"
    :height="size"
    fill="currentColor"
    aria-hidden="true"
    focusable="false"
  >
    <path v-for="(d, i) in pathList" :key="i" :d="d" />
  </svg>
</template>

<script setup>
import { computed } from 'vue';
import { ICON_PATHS } from '../../icons/paths.js';

const props = defineProps({
  /** 图标名，取 src/icons/paths.js 中的 key */
  name: { type: String, required: true },
  /** 像素尺寸，同时作用于宽高 */
  size: { type: [String, Number], default: 20 },
  /** 旋转动画（用于加载态） */
  spin: { type: Boolean, default: false },
});

const pathList = computed(() => ICON_PATHS[props.name] || []);
</script>

<style scoped>
.app-icon {
  display: block;
  flex-shrink: 0;
}

.app-icon--spin {
  animation: app-icon-spin 900ms linear infinite;
}

@keyframes app-icon-spin {
  to {
    transform: rotate(360deg);
  }
}

@media (prefers-reduced-motion: reduce) {
  .app-icon--spin {
    animation-duration: 2400ms;
  }
}
</style>
