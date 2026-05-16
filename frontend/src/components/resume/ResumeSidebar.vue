<template>
  <nav class="sidebar" :class="{ expanded }" @mouseenter="expanded = true" @mouseleave="expanded = false">
    <button
      v-for="btn in buttons"
      :key="btn.event"
      class="sidebar-btn"
      :class="{ active: activeBtn === btn.event }"
      :title="btn.label"
      @click="handleClick(btn.event)"
    >
      <span class="icon">{{ btn.icon }}</span>
      <span class="label">{{ btn.label }}</span>
    </button>
  </nav>
</template>

<script setup>
import { ref } from 'vue'

const props = defineProps({
  activeBtn: {
    type: String,
    default: null,
  },
})

const emit = defineEmits(['template', 'style', 'settings', 'add-block', 'preview'])

const expanded = ref(false)

const buttons = [
  { event: 'template', label: 'Template', icon: '\uD83D\uDCCB' },
  { event: 'style',    label: 'Style',    icon: '\uD83C\uDFA8' },
  { event: 'settings', label: 'Settings',  icon: '\u2699\uFE0F' },
  { event: 'add-block',label: 'Add Block', icon: '\u2795' },
  { event: 'preview',  label: 'Preview',   icon: '\uD83D\uDC41\u200D\uFE0F' },
]

function handleClick(event) {
  emit(event)
}
</script>

<style scoped>
.sidebar {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 8px;
  width: 60px;
  min-height: 100%;
  background: #ffffff;
  border-right: 1px solid #e5e7eb;
  padding: 16px 8px;
  transition: width 0.25s ease;
  overflow: hidden;
}

.sidebar.expanded {
  width: 200px;
}

.sidebar-btn {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
  padding: 12px;
  border: 1px solid transparent;
  border-radius: 8px;
  background: transparent;
  color: #6b7280;
  cursor: pointer;
  font-size: 15px;
  white-space: nowrap;
  transition: background 0.2s ease, color 0.2s ease;
  text-align: left;
}

.sidebar-btn:hover {
  background: #f3f4f6;
  color: #374151;
}

.sidebar-btn.active {
  background: #22c55e;
  color: #ffffff;
}

.icon {
  font-size: 18px;
  flex-shrink: 0;
  width: 24px;
  text-align: center;
}

.label {
  opacity: 0;
  transition: opacity 0.2s ease;
  font-size: 14px;
  font-weight: 500;
}

.sidebar.expanded .label {
  opacity: 1;
}
</style>
