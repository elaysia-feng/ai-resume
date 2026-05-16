<template>
  <div class="preview-panel">
    <header class="preview-banner">
      <div class="banner-left">
        <span class="banner-dot"></span>
        <span class="banner-text">Preview Mode</span>
      </div>
      <button class="close-btn" @click="emit('close')">
        <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
          <path d="M12 4L4 12M4 4L12 12" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/>
        </svg>
        Close
      </button>
    </header>

    <div class="preview-scroll">
      <div class="a4-sheet">
        <div v-if="resumeData" class="resume-content">
          <div v-if="resumeData.name" class="r-name">{{ resumeData.name }}</div>
          <div v-if="resumeData.title" class="r-title">{{ resumeData.title }}</div>
          <div v-if="resumeData.contact" class="r-contact">{{ resumeData.contact }}</div>

          <template v-if="resumeData.sections">
            <div v-for="(section, key) in resumeData.sections" :key="key" class="r-section">
              <div class="r-section-title">{{ section.title }}</div>
              <div v-for="(item, idx) in section.items" :key="idx" class="r-item">
                <div v-if="item.header" class="r-item-header">{{ item.header }}</div>
                <div v-if="item.sub" class="r-item-sub">{{ item.sub }}</div>
                <div v-if="item.body" class="r-item-body">{{ item.body }}</div>
              </div>
            </div>
          </template>

          <div v-if="!resumeData" class="r-empty">
            No resume data to preview.
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
defineProps({
  resumeData: {
    type: Object,
    default: null,
  },
})

const emit = defineEmits(['close'])
</script>

<style scoped>
.preview-panel {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: linear-gradient(135deg, #0a0f0d 0%, #0d1410 50%, #0a0f0d 100%);
}

.preview-banner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 24px;
  background: rgba(13, 20, 16, 0.85);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  border-bottom: 1px solid rgba(57, 255, 20, 0.15);
  position: relative;
  flex-shrink: 0;
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.4);
}

.preview-banner::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 2px;
  background: linear-gradient(90deg, #39ff14, #00ffff, #39ff14, #00ff88, #39ff14);
  background-size: 300% 100%;
  animation: gradient-shift 4s ease infinite;
}

@keyframes gradient-shift {
  0%, 100% { background-position: 0% 50%; }
  50% { background-position: 100% 50%; }
}

.banner-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.banner-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #39ff14;
  box-shadow: 0 0 8px #39ff14, 0 0 16px rgba(57, 255, 20, 0.6), 0 0 24px rgba(57, 255, 20, 0.3);
  animation: dot-pulse 2s ease-in-out infinite;
}

@keyframes dot-pulse {
  0%, 100% { box-shadow: 0 0 8px #39ff14, 0 0 16px rgba(57, 255, 20, 0.6); }
  50% { box-shadow: 0 0 14px #39ff14, 0 0 28px rgba(57, 255, 20, 0.8), 0 0 40px rgba(57, 255, 20, 0.4); }
}

.banner-text {
  font-size: 14px;
  font-weight: 600;
  color: #e0ffe8;
  letter-spacing: 0.05em;
  text-shadow: 0 0 12px rgba(57, 255, 20, 0.5);
}

.close-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 7px 14px;
  border: 1px solid rgba(57, 255, 20, 0.35);
  border-radius: 6px;
  background: rgba(57, 255, 20, 0.06);
  color: #39ff14;
  cursor: pointer;
  font-size: 13px;
  transition: all 0.3s ease;
  position: relative;
  overflow: hidden;
}

.close-btn::before {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, rgba(57, 255, 20, 0.15), rgba(0, 255, 136, 0.1));
  opacity: 0;
  transition: opacity 0.3s ease;
}

.close-btn:hover {
  color: #b0ff80;
  border-color: rgba(57, 255, 20, 0.7);
  box-shadow: 0 0 16px rgba(57, 255, 20, 0.4), 0 0 8px rgba(57, 255, 20, 0.2), inset 0 0 12px rgba(57, 255, 20, 0.05);
  transform: translateY(-1px);
}

.close-btn:hover::before {
  opacity: 1;
}

.preview-scroll {
  flex: 1;
  overflow-y: auto;
  padding: 32px;
  display: flex;
  justify-content: center;
  background: radial-gradient(ellipse at center, rgba(57, 255, 20, 0.03) 0%, transparent 70%);
}

.preview-scroll::-webkit-scrollbar {
  width: 6px;
}
.preview-scroll::-webkit-scrollbar-track {
  background: rgba(57, 255, 20, 0.05);
}
.preview-scroll::-webkit-scrollbar-thumb {
  background: rgba(57, 255, 20, 0.25);
  border-radius: 3px;
}
.preview-scroll::-webkit-scrollbar-thumb:hover {
  background: rgba(57, 255, 20, 0.45);
}

/* A4 sheet: 210mm x 297mm scaled to screen */
.a4-sheet {
  width: 595px;
  min-height: 842px;
  background: linear-gradient(180deg, #ffffff 0%, #f6fff8 100%);
  box-shadow:
    0 0 20px rgba(57, 255, 20, 0.3),
    0 0 60px rgba(57, 255, 20, 0.12),
    0 8px 40px rgba(0, 0, 0, 0.5),
    0 2px 8px rgba(0, 0, 0, 0.3);
  border-radius: 8px;
  padding: 48px 52px;
  box-sizing: border-box;
  position: relative;
  border: 1px solid rgba(57, 255, 20, 0.15);
}

.a4-sheet::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 3px;
  background: linear-gradient(90deg, #39ff14, #00ffff, #39ff14, #00ff88, #39ff14);
  background-size: 300% 100%;
  border-radius: 8px 8px 0 0;
  animation: gradient-shift 4s ease infinite;
}

.resume-content {
  font-family: 'Inter', sans-serif;
  color: #1a1a2e;
  font-size: 13px;
  line-height: 1.6;
}

.r-name {
  font-size: 26px;
  font-weight: 800;
  letter-spacing: -0.02em;
  color: #0f1c0f;
  margin-bottom: 4px;
  text-shadow: 0 2px 8px rgba(57, 255, 20, 0.15);
}

.r-title {
  font-size: 14px;
  color: #1a7a0a;
  font-weight: 600;
  margin-bottom: 6px;
  text-shadow: 0 0 12px rgba(57, 255, 20, 0.4);
  animation: title-glow 3s ease-in-out infinite;
}

@keyframes title-glow {
  0%, 100% { text-shadow: 0 0 12px rgba(57, 255, 20, 0.4); }
  50% { text-shadow: 0 0 20px rgba(57, 255, 20, 0.7), 0 0 30px rgba(57, 255, 20, 0.4); }
}

.r-contact {
  font-size: 12px;
  color: #6b7280;
  margin-bottom: 24px;
}

.r-section {
  margin-bottom: 20px;
}

.r-section-title {
  font-size: 11px;
  font-weight: 800;
  text-transform: uppercase;
  letter-spacing: 0.1em;
  color: #39ff14;
  border-bottom: 1.5px solid #d1ffd8;
  padding-bottom: 4px;
  margin-bottom: 10px;
  position: relative;
  text-shadow: 0 0 10px rgba(57, 255, 20, 0.6), 0 0 20px rgba(57, 255, 20, 0.3);
}

.r-section-title::after {
  content: '';
  position: absolute;
  bottom: -1.5px;
  left: 0;
  width: 40px;
  height: 1.5px;
  background: linear-gradient(90deg, #39ff14, #00ffff);
  border-radius: 1px;
  animation: line-shimmer 3s ease-in-out infinite;
  box-shadow: 0 0 6px rgba(57, 255, 20, 0.8);
}

@keyframes line-shimmer {
  0%, 100% { width: 40px; opacity: 1; }
  50% { width: 80px; opacity: 0.7; }
}

.r-item {
  margin-bottom: 10px;
  transition: transform 0.2s ease, padding-left 0.2s ease;
  padding-left: 0;
  border-radius: 4px;
}

.r-item:hover {
  transform: translateX(4px);
  padding-left: 8px;
  background: linear-gradient(90deg, rgba(57, 255, 20, 0.06), transparent);
}

.r-item-header {
  font-size: 13px;
  font-weight: 700;
  color: #1a1a2e;
  transition: color 0.2s ease, text-shadow 0.2s ease;
}

.r-item:hover .r-item-header {
  color: #1a7a0a;
  text-shadow: 0 0 8px rgba(57, 255, 20, 0.4);
}

.r-item-sub {
  font-size: 12px;
  color: #6b7280;
  font-style: italic;
}

.r-item-body {
  font-size: 12px;
  color: #374151;
  margin-top: 2px;
}

.r-empty {
  text-align: center;
  padding: 40px;
  color: #9ca3af;
  font-size: 14px;
}
</style>
