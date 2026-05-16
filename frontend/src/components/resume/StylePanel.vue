<template>
  <section class="style-panel">
    <h2 class="section-title">Style</h2>

    <div class="control-group">
      <label class="control-label">Font Family</label>
      <div class="font-options">
        <button
          v-for="font in fonts"
          :key="font"
          class="font-btn"
          :class="{ selected: localStyle.font === font }"
          :style="{ fontFamily: font }"
          @click="update('font', font)"
        >
          {{ font }}
        </button>
      </div>
    </div>

    <div class="control-group">
      <label class="control-label">Accent Color</label>
      <div class="color-swatches">
        <button
          v-for="color in accentColors"
          :key="color.value"
          class="swatch-btn"
          :class="{ selected: localStyle.color === color.value }"
          :style="{ background: color.value }"
          :title="color.name"
          @click="update('color', color.value)"
        >
          <span v-if="localStyle.color === color.value" class="swatch-check">&#10003;</span>
        </button>
      </div>
    </div>

    <div class="control-group" v-if="showSidebarColor">
      <label class="control-label">Sidebar Color</label>
      <div class="color-swatches">
        <button
          v-for="color in sidebarColors"
          :key="color.value"
          class="swatch-btn"
          :class="{ selected: localStyle.sidebarColor === color.value }"
          :style="{ background: color.value }"
          :title="color.name"
          @click="update('sidebarColor', color.value)"
        >
          <span v-if="localStyle.sidebarColor === color.value" class="swatch-check">&#10003;</span>
        </button>
      </div>
    </div>

    <div class="control-group">
      <label class="control-label">Line Spacing</label>
      <div class="spacing-options">
        <button
          v-for="sp in spacingOptions"
          :key="sp.value"
          class="spacing-btn"
          :class="{ selected: localStyle.spacing === sp.value }"
          @click="update('spacing', sp.value)"
        >
          {{ sp.label }}
        </button>
      </div>
    </div>
  </section>
</template>

<script setup>
import { reactive, watch, computed } from 'vue'

const props = defineProps({
  modelValue: {
    type: Object,
  },
})

const emit = defineEmits(['update:modelValue'])

const localStyle = reactive({ ...props.modelValue })

watch(
  () => props.modelValue,
  (val) => Object.assign(localStyle, val),
  { deep: true },
)

function update(key, value) {
  localStyle[key] = value
  emit('update:modelValue', { ...localStyle })
}

const fonts = ['Inter', 'Merriweather', 'Roboto', 'Georgia']

const accentColors = [
  { value: '#22c55e', name: 'Green' },
  { value: '#3b82f6', name: 'Blue' },
  { value: '#10b981', name: 'Green' },
  { value: '#f59e0b', name: 'Amber' },
  { value: '#ef4444', name: 'Red' },
]

const sidebarColors = [
  { value: 'blue',  name: 'Blue' },
  { value: 'black', name: 'Black' },
  { value: 'white', name: 'White' },
  { value: 'gray',  name: 'Gray' },
]

const showSidebarColor = computed(() => localStyle.template === 'modern')

const spacingOptions = [
  { label: 'Compact',   value: 'compact'   },
  { label: 'Normal',     value: 'normal'    },
  { label: 'Relaxed',   value: 'relaxed'   },
]
</script>

<style scoped>
.style-panel {
  padding: 24px;
}

.section-title {
  font-size: 18px;
  font-weight: 700;
  color: #1a1a2e;
  margin: 0 0 20px;
}

.control-group {
  margin-bottom: 24px;
}

.control-label {
  display: block;
  font-size: 12px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  color: #6b7280;
  margin-bottom: 10px;
}

/* Font buttons */
.font-options {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.font-btn {
  padding: 10px 14px;
  border: 2px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
  cursor: pointer;
  text-align: left;
  font-size: 15px;
  color: #374151;
  transition: border-color 0.2s, background 0.2s;
}

.font-btn:hover {
  border-color: #22c55e;
}

.font-btn.selected {
  border-color: #22c55e;
  background: #dcfce7;
  font-weight: 600;
}

/* Color swatches */
.color-swatches {
  display: flex;
  gap: 10px;
}

.swatch-btn {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  border: 3px solid transparent;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: transform 0.15s, border-color 0.2s;
}

.swatch-btn:hover {
  transform: scale(1.1);
}

.swatch-btn.selected {
  border-color: #1a1a2e;
}

.swatch-check {
  color: #fff;
  font-size: 16px;
  font-weight: bold;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.4);
}

/* Spacing buttons */
.spacing-options {
  display: flex;
  gap: 8px;
}

.spacing-btn {
  flex: 1;
  padding: 10px;
  border: 2px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
  cursor: pointer;
  font-size: 13px;
  font-weight: 500;
  color: #374151;
  transition: border-color 0.2s, background 0.2s;
}

.spacing-btn:hover {
  border-color: #22c55e;
}

.spacing-btn.selected {
  border-color: #22c55e;
  background: #dcfce7;
  color: #22c55e;
  font-weight: 700;
}
</style>
