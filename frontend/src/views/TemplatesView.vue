<template>
  <div class="templates-page">
    <h1 class="page-title">简历模板</h1>

    <div class="templates-grid">
      <div
        v-for="template in templates"
        :key="template.value"
        class="template-card"
        :class="{ active: selected === template.value }"
        @click="selected = template.value"
      >
        <div class="template-preview">
          <div class="preview-lines">
            <div class="line title"></div>
            <div class="line subtitle"></div>
            <div class="line content"></div>
            <div class="line content short"></div>
          </div>
        </div>
        <span class="template-name">{{ template.label }}</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'

const selected = ref('classic')

const templates = [
  { value: 'classic', label: '经典' },
  { value: 'modern', label: '现代' },
  { value: 'creative', label: '创意' },
]
</script>

<style scoped>
.templates-page {
  max-width: 1200px;
}

.page-title {
  font-size: 24px;
  font-weight: 700;
  color: #1a1a2e;
  margin: 0 0 24px;
}

.templates-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 24px;
}

.template-card {
  background: #fff;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.08);
  cursor: pointer;
  transition: all 0.2s;
  border: 2px solid transparent;
}

.template-card:hover {
  box-shadow: 0 4px 20px rgba(0,0,0,0.12);
}

.template-card.active {
  border-color: #6ee7b7;
}

.template-preview {
  background: #f9fafb;
  border-radius: 8px;
  padding: 24px;
  margin-bottom: 16px;
}

.preview-lines {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.line {
  background: #e5e7eb;
  border-radius: 4px;
}

.line.title {
  height: 20px;
  width: 60%;
}

.line.subtitle {
  height: 14px;
  width: 40%;
}

.line.content {
  height: 10px;
  width: 100%;
}

.line.content.short {
  width: 75%;
}

.template-name {
  font-size: 16px;
  font-weight: 600;
  color: #1a1a2e;
  display: block;
  text-align: center;
}
</style>
