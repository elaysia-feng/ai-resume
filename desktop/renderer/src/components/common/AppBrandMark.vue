<template>
  <span class="app-brand-mark" :style="{ '--app-brand-mark-size': `${props.size}px` }" aria-hidden="true">
    <svg class="app-brand-mark__glyph" viewBox="0 0 5 5" focusable="false">
      <path d="M2 0h1v1H2z M1 1h1v1H1z M3 1h1v1H3z M0 2h1v1H0z M4 2h1v1H4z M0 3h5v1H0z M0 4h1v1H0z M4 4h1v1H4z" />
    </svg>
  </span>
</template>

<script setup>
const props = defineProps({
  size: { type: [String, Number], default: 32 },
});
</script>

<style scoped>
.app-brand-mark {
  display: grid;
  width: var(--app-brand-mark-size);
  height: var(--app-brand-mark-size);
  flex: 0 0 auto;
  place-items: center;
  border: 1px solid var(--ui-accent-border);
  border-radius: var(--ui-radius-sm);
  background: var(--ui-accent);
  color: var(--ui-accent-fg);
}

.app-brand-mark__glyph {
  display: block;
  width: 72%;
  height: 72%;
  fill: currentColor;
  shape-rendering: crispEdges;
}
</style>
