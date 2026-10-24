<template>
  <div class="templates-page">
    <div class="templates-header">
      <h1 class="page-title">选择简历模板</h1>
      <p class="page-subtitle">挑选一个适合你的模板，开始制作简历</p>
    </div>

    <div class="templates-grid">
      <div
        v-for="tpl in templates"
        :key="tpl.id"
        class="template-card"
        @click="handleSelect(tpl.id)"
      >
        <!-- Mini preview -->
        <div class="template-preview" :style="{ background: tpl.bg }">
          <div class="preview-shell">
            <div class="preview-header" :style="{ background: tpl.headerBg }"></div>
            <div class="preview-body" :class="tpl.layout">
              <div v-if="tpl.sidebar" class="preview-sidebar" :style="{ background: tpl.sidebarBg }"></div>
              <div class="preview-main">
                <div class="preview-line title" :style="{ width: tpl.line1, background: tpl.accent }"></div>
                <div class="preview-line subtitle" :style="{ width: tpl.line2 }"></div>
                <div class="preview-line" :style="{ width: tpl.line3 }"></div>
                <div class="preview-line short" :style="{ width: tpl.line4 }"></div>
                <div class="preview-line" :style="{ width: '65%' }"></div>
              </div>
            </div>
          </div>
        </div>

        <!-- Info -->
        <div class="template-info">
          <h3 class="template-name">{{ tpl.name }}</h3>
          <p class="template-desc">{{ tpl.desc }}</p>
          <span class="template-tags">
            <span v-for="tag in tpl.tags" :key="tag" class="template-tag">{{ tag }}</span>
          </span>
        </div>

        <button class="template-select-btn" @click.stop="handleSelect(tpl.id)">
          选择此模板
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { useResumeStore } from '../store/resumeStore.js'

const router = useRouter()
const resumeStore = useResumeStore()

const templates = [
  {
    id: 'classic',
    name: '标准版',
    desc: '结构清晰，重点突出，适合程序员、产品经理、设计师等职位使用。',
    tags: ['程序员', '产品', '设计通用'],
    bg: 'linear-gradient(180deg, #ffffff 0%, #f8fbff 100%)',
    headerBg: '#1d4f91',
    sidebarBg: null,
    accent: '#1d4f91',
    layout: 'layout-single',
    sidebar: false,
    line1: '80%', line2: '55%', line3: '75%', line4: '45%',
  },
  {
    id: 'modern',
    name: '简约求职版',
    desc: '信息清晰紧凑，突出核心经历与能力，适合通用岗位、校招及社招使用。',
    tags: ['通用', '校招', '社招'],
    bg: 'linear-gradient(180deg, #f6fbfa 0%, #eef7f5 100%)',
    headerBg: '#0f766e',
    sidebarBg: 'rgba(15,118,110,0.08)',
    accent: '#0f766e',
    layout: 'layout-side',
    sidebar: true,
    line1: '65%', line2: '70%', line3: '60%', line4: '50%',
  },
  {
    id: 'compact',
    name: '双列紧凑版',
    desc: '信息密度高，一页展示核心经历，适合应届毕业生、实习生、校招类岗位。',
    tags: ['实习生', '校招', '紧凑'],
    bg: 'linear-gradient(180deg, #faf5ff 0%, #f3e8ff 100%)',
    headerBg: '#6d28d9',
    sidebarBg: 'rgba(109,40,217,0.08)',
    accent: '#6d28d9',
    layout: 'layout-side',
    sidebar: true,
    line1: '60%', line2: '65%', line3: '55%', line4: '48%',
  },
  {
    id: 'academic',
    name: '学术科研版',
    desc: '极简风格、信息容量大，适合科研、教育、技术研发等岗位。',
    tags: ['研究', '教育', '技术岗'],
    bg: 'linear-gradient(180deg, #f7f9fc 0%, #edf2f7 100%)',
    headerBg: '#1e3a5f',
    sidebarBg: null,
    accent: '#1e3a5f',
    layout: 'layout-single',
    sidebar: false,
    line1: '75%', line2: '50%', line3: '70%', line4: '42%',
  },
  {
    id: 'creative',
    name: '创意进阶版',
    desc: '不规则设计、视觉分区强烈，适合UI设计师、平面设计、品牌营销岗位。',
    tags: ['视觉', 'UI', '品牌'],
    bg: 'linear-gradient(180deg, #fffdf8 0%, #fff6e7 100%)',
    headerBg: 'linear-gradient(135deg, #1f2937, #24364a)',
    sidebarBg: 'rgba(36,54,74,0.06)',
    accent: '#b45309',
    layout: 'layout-side',
    sidebar: true,
    line1: '70%', line2: '40%', line3: '80%', line4: '60%',
  },
  {
    id: 'dual-creative',
    name: '双列创意版',
    desc: '兼顾信息密度与视觉美感，适合前端开发、交互设计、产品经理等创意岗位。',
    tags: ['前端', '设计', '产品'],
    bg: 'linear-gradient(180deg, #fffbeb 0%, #fef3c7 100%)',
    headerBg: 'linear-gradient(135deg, #78350f, #b45309)',
    sidebarBg: 'rgba(146,64,14,0.06)',
    accent: '#92400e',
    layout: 'layout-side',
    sidebar: true,
    line1: '68%', line2: '45%', line3: '72%', line4: '55%',
  },
]

async function handleSelect(templateId) {
  try {
    const created = await resumeStore.createResume({ title: 'Untitled Resume' })
    const id = created?.id || created?._id
    if (id) {
      router.push({ path: `/editor/${id}`, query: { template: templateId } })
    }
  } catch {
    // error handled by store
  }
}
</script>

<style scoped>
.templates-page {
  max-width: 1200px;
  margin: 0 auto;
}

.templates-header {
  margin-bottom: 32px;
}

.page-title {
  font-size: 28px;
  font-weight: 800;
  color: #1a1a2e;
  margin: 0 0 8px;
}

.page-subtitle {
  font-size: 15px;
  color: #6b7280;
  margin: 0;
}

.templates-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 24px;
}

/* ── Card ─────────────────────────────────────────────────────────────── */
.template-card {
  background: #fff;
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  cursor: pointer;
  transition: all 0.25s ease;
  border: 2px solid transparent;
  display: flex;
  flex-direction: column;
}

.template-card:hover {
  box-shadow: 0 8px 30px rgba(0, 0, 0, 0.12);
  transform: translateY(-4px);
  border-color: #22c55e;
}

/* ── Preview thumbnail ────────────────────────────────────────────────── */
.template-preview {
  aspect-ratio: 3 / 4;
  padding: 12px;
  position: relative;
}

.preview-shell {
  width: 100%;
  height: 100%;
  border-radius: 8px;
  overflow: hidden;
  background: rgba(255, 255, 255, 0.88);
  border: 1px solid rgba(148, 163, 184, 0.15);
  display: flex;
  flex-direction: column;
}

.preview-header {
  height: 18%;
  width: 100%;
  flex-shrink: 0;
}

.preview-body {
  flex: 1;
  padding: 8px;
  display: grid;
  gap: 6px;
}

.layout-single {
  grid-template-columns: 1fr;
}

.layout-side {
  grid-template-columns: 26% 1fr;
}

.preview-sidebar {
  border-radius: 4px;
  min-height: 100%;
}

.preview-main {
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.preview-line {
  height: 5px;
  background: rgba(0, 0, 0, 0.1);
  border-radius: 3px;
}

.preview-line.title {
  height: 7px;
}

.preview-line.subtitle {
  height: 4px;
}

.preview-line.short {
  width: 55% !important;
}

/* ── Info ─────────────────────────────────────────────────────────────── */
.template-info {
  padding: 16px 18px 12px;
  flex: 1;
}

.template-name {
  font-size: 16px;
  font-weight: 700;
  color: #1a1a2e;
  margin: 0 0 6px;
}

.template-desc {
  font-size: 13px;
  color: #6b7280;
  line-height: 1.55;
  margin: 0 0 10px;
}

.template-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.template-tag {
  font-size: 11px;
  font-weight: 600;
  color: #059669;
  background: #ecfdf5;
  padding: 3px 10px;
  border-radius: 20px;
}

/* ── Select button ────────────────────────────────────────────────────── */
.template-select-btn {
  margin: 0 18px 18px;
  padding: 10px 0;
  border: none;
  border-radius: 10px;
  background: linear-gradient(135deg, #22c55e, #16a34a);
  color: #fff;
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.2s;
}

.template-select-btn:hover {
  background: linear-gradient(135deg, #16a34a, #15803d);
  box-shadow: 0 4px 14px rgba(34, 197, 94, 0.35);
}

/* ── Responsive ───────────────────────────────────────────────────────── */
@media (max-width: 960px) {
  .templates-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 600px) {
  .templates-grid {
    grid-template-columns: 1fr;
  }
}
</style>
