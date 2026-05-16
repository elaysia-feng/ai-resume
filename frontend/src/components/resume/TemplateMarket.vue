<template>
  <section class="template-market">
    <h2 class="section-title">选择模板</h2>
    <div class="templates-grid">
      <button
        v-for="tpl in templates"
        :key="tpl.id"
        class="template-card"
        :class="{ selected: modelValue === tpl.id }"
        @click="emit('update:modelValue', tpl.id)"
      >
        <div class="template-preview" :style="{ background: tpl.bg }">
          <div class="preview-shell">
            <div class="preview-header" :style="{ background: tpl.header }"></div>
            <div class="preview-body" :class="tpl.layout">
              <div v-if="tpl.sidebar" class="preview-sidebar"></div>
              <div class="preview-main">
                <div class="preview-line title" :style="{ width: tpl.line1 }"></div>
                <div class="preview-line subtitle" :style="{ width: tpl.line2 }"></div>
                <div class="preview-line" :style="{ width: tpl.line3 }"></div>
                <div class="preview-line short" :style="{ width: tpl.line4 }"></div>
              </div>
            </div>
          </div>
        </div>
        <span class="template-name">{{ tpl.name }}</span>
        <span v-if="modelValue === tpl.id" class="selected-badge">已选择</span>
      </button>
    </div>
  </section>
</template>

<script setup>
defineProps({
  modelValue: {
    type: String,
    default: null,
  },
})

const emit = defineEmits(['update:modelValue'])

const templates = [
  {
    id: 'classic',
    name: '经典',
    bg: 'linear-gradient(180deg, #ffffff 0%, #f8fbff 100%)',
    header: '#1d4f91',
    layout: 'layout-classic',
    sidebar: false,
    line1: '80%',
    line2: '55%',
    line3: '75%',
    line4: '45%',
  },
  {
    id: 'modern',
    name: '现代',
    bg: 'linear-gradient(180deg, #f6fbfa 0%, #eef7f5 100%)',
    header: '#0f766e',
    layout: 'layout-modern',
    sidebar: true,
    line1: '65%',
    line2: '70%',
    line3: '60%',
    line4: '50%',
  },
  {
    id: 'creative',
    name: '编辑感',
    bg: 'linear-gradient(180deg, #fffdf8 0%, #fff6e7 100%)',
    header: '#24364a',
    layout: 'layout-creative',
    sidebar: true,
    line1: '70%',
    line2: '40%',
    line3: '80%',
    line4: '60%',
  },
]
</script>

<style scoped>
.template-market {
  padding: 24px;
}

.section-title {
  font-size: 18px;
  font-weight: 700;
  color: #1a1a2e;
  margin: 0 0 20px;
}

.templates-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}

.template-card {
  position: relative;
  border: 2px solid #e2e8f0;
  border-radius: 12px;
  background: #fff;
  padding: 12px;
  cursor: pointer;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}

.template-card:hover {
  border-color: #22c55e;
  box-shadow: 0 4px 16px rgba(34, 197, 94, 0.15);
}

.template-card.selected {
  border-color: #22c55e;
  background: #dcfce7;
}

.template-preview {
  width: 100%;
  aspect-ratio: 3 / 4;
  border-radius: 8px;
  padding: 10px;
}

.preview-shell {
  width: 100%;
  height: 100%;
  overflow: hidden;
  border-radius: 6px;
  background: rgba(255, 255, 255, 0.82);
  border: 1px solid rgba(148, 163, 184, 0.16);
  display: flex;
  flex-direction: column;
}

.preview-header {
  height: 18%;
  width: 100%;
}

.preview-body {
  flex: 1;
  padding: 8px;
  display: grid;
  gap: 7px;
}

.layout-classic {
  grid-template-columns: 1fr;
}

.layout-modern,
.layout-creative {
  grid-template-columns: 28% 1fr;
}

.preview-sidebar {
  border-radius: 4px;
  background: rgba(15, 23, 42, 0.08);
}

.preview-main {
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.preview-line {
  height: 6px;
  background: rgba(0, 0, 0, 0.12);
  border-radius: 3px;
}

.preview-line.title {
  height: 8px;
}

.preview-line.subtitle {
  height: 5px;
}

.preview-line.short {
  width: 55%;
}

.template-name {
  font-size: 13px;
  font-weight: 600;
  color: #374151;
}

.selected-badge {
  position: absolute;
  top: 8px;
  right: 8px;
  background: #22c55e;
  color: #fff;
  font-size: 10px;
  font-weight: 700;
  padding: 2px 7px;
  border-radius: 20px;
}
</style>
