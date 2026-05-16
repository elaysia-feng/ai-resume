<template>
  <div class="editor-layout" :class="layoutClass">
    <!-- Edit Panel -->
    <div class="edit-panel" :style="{ width: panelWidth + '%', minWidth: '280px', transition: isDragging ? 'none' : 'width 0.1s' }">
      <div class="panel-header">
        <span class="panel-title">模块编辑面板</span>
        <div class="panel-actions">
          <!-- Save status indicator -->
          <span class="save-indicator" :class="saveStatusClass">{{ saveStatusText }}</span>
          <!-- Main explicit save button (saves all built-in modules immediately) -->
          <button class="main-save-btn" :disabled="saveStatus === 'saving'" @click="onMainSave">
            {{ saveStatus === 'saving' ? '保存中...' : '保存全部' }}
          </button>
          <!-- Layout Swap Button -->
          <button class="swap-btn" @click="toggleLayout" title="左右互换">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none">
              <path d="M7 16l-4-4 4-4M17 8l4 4-4 4M3 12h18" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
          </button>
          <!-- Template Selector -->
          <div class="template-selector">
            <button
              v-for="t in templates"
              :key="t.value"
              class="template-btn"
              :class="{ active: store.selectedTemplate.value === t.value }"
              @click="store.selectedTemplate.value = t.value"
            >
              {{ t.label }}
            </button>
          </div>
          <div class="font-size-selector">
            <span class="font-size-label">正文</span>
            <button
              v-for="size in bodyFontOptions"
              :key="size"
              class="font-size-btn"
              :class="{ active: store.resumeBodyFontSize.value === size }"
              @click="setResumeBodyFontSize(size)"
            >
              {{ size }}px
            </button>
          </div>
          <div v-if="supportsSidebarColor" class="sidebar-color-selector">
            <span class="sidebar-color-label">左栏</span>
            <button
              v-for="color in sidebarColorOptions"
              :key="color.value"
              class="sidebar-color-btn"
              :class="{ active: store.sidebarColor.value === color.value }"
              @click="setSidebarColor(color.value)"
            >
              <span class="sidebar-color-btn__swatch" :style="{ background: color.swatch }"></span>
              <span>{{ color.label }}</span>
            </button>
          </div>
          <button class="export-pdf-btn" :disabled="exportingPdf" @click="onExportPdf">
            {{ exportingPdf ? '导出中...' : '导出 PDF' }}
          </button>
          <button class="add-block-btn" @click="showAddBlock = true">+ 添加模块</button>
        </div>
      </div>

      <!-- Section List (scrollable) -->
      <div class="block-list-container">
        <div class="block-list">
          <div
            v-for="section in store.sections"
            :key="section.id"
            class="block-item"
            :class="{ active: activeId === section.id, hidden: !section.visible }"
            draggable="true"
            @dragstart="onDragStart(section.id, $event)"
            @dragover.prevent="onDragOver(section.id)"
            @dragleave="onDragLeave"
            @drop.prevent="onDrop(section.id)"
            @dragend="onDragEnd"
            @click="toggleActive(section.id)"
          >
            <span class="drag-handle">☰</span>
            <button
              class="eye-btn"
              @click.stop="store.toggleVisible(section.id)"
              :title="section.visible ? '隐藏' : '显示'"
            >
              {{ section.visible ? '👁' : '👁‍🗨' }}
            </button>
            <span class="block-name">{{ section.sectionTitle }}</span>
            <button
              class="delete-btn"
              @click.stop="store.removeSection(section.id)"
            >
              ×
            </button>
          </div>
        </div>
      </div>

      <!-- Inline Editor Area -->
      <div v-if="activeId" class="editor-area">
        <div class="editor-header">
          <span class="editor-title">{{ activeSection?.sectionTitle }}</span>
          <button class="close-editor-btn" @click="activeId = null">×</button>
        </div>
        <component
          :is="activeEditor"
          :model-value="activeSectionContent"
          :is-active="true"
          :resume-id="editorResumeId"
          :schema-type="activeSection?.schemaType ?? 'TEXT'"
          :is-custom="activeSection?.sectionType === 'CUSTOM'"
          @update:model-value="onEditorUpdate"
        />
      </div>

      <!-- Add Block Drawer -->
      <Transition name="drawer">
        <div v-if="showAddBlock" class="add-block-drawer">
          <div class="drawer-header">
            <span class="drawer-title">添加模块</span>
            <button class="close-drawer-btn" @click="showAddBlock = false">×</button>
          </div>
          <div class="drawer-body">
            <AddBlock :added-section-codes="store.sections.map(s => s.sectionCode)" @add-block="onBlockAdded" />
          </div>
        </div>
      </Transition>
      <div v-if="showAddBlock" class="drawer-backdrop" @click="showAddBlock = false" />
    </div>

    <!-- Resize Handle -->
    <div
      class="resize-handle"
      @mousedown="onResizeStart"
    >
      <span class="resize-grip"></span>
    </div>

    <!-- Preview Panel -->
    <div class="preview-panel" :style="{ width: previewWidth + '%', minWidth: '280px', transition: isDragging ? 'none' : 'width 0.1s' }">
      <div class="resume-preview-container">
        <ResumeTemplate
          ref="resumeTemplateRef"
          :template="store.selectedTemplate.value"
          :sections="store.visibleSections.value"
          :locale="locale"
          :body-font-size="store.resumeBodyFontSize.value"
          :sidebar-color="store.sidebarColor.value"
        />
      </div>
    </div>
    <FloatingAgentCopilot
      :resume-id="editorResumeId"
      :section="activeSection"
      @approved="onAgentApproved"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import {
  sections,
  visibleSections,
  toggleVisible,
  removeSection,
  reorderSections,
  addSection,
  getSectionContent,
  setSectionContent,
  selectedTemplate,
  sidebarColor,
  resumeBodyFontSize,
  setSidebarColor,
  setResumeBodyFontSize,
  useResumeStore,
} from '../store/resumeStore.js';
import ResumeTemplate from '../components/resume/ResumeTemplate.vue';
import AddBlock from '../components/resume/AddBlock.vue';
import FloatingAgentCopilot from '../components/resume/FloatingAgentCopilot.vue';
import PersonalInfoEditor from '../components/resume/editors/PersonalInfoEditor.vue';
import JobIntentEditor from '../components/resume/editors/JobIntentEditor.vue';
import EducationEditor from '../components/resume/editors/EducationEditor.vue';
import ExperienceEditor from '../components/resume/editors/ExperienceEditor.vue';
import CampusEditor from '../components/resume/editors/CampusEditor.vue';
import SkillsEditor from '../components/resume/editors/SkillsEditor.vue';
import CertificatesEditor from '../components/resume/editors/CertificatesEditor.vue';
import SelfEvaluationEditor from '../components/resume/editors/SelfEvaluationEditor.vue';
import ProjectsEditor from '../components/resume/editors/ProjectsEditor.vue';
import InternshipEditor from '../components/resume/editors/InternshipEditor.vue';
import GenericEditor from '../components/resume/editors/GenericEditor.vue';
import { exportToPDF } from '../utils/export.js';
import { extractApiMessage } from '../api/request.js';
import '../styles/resume-editor.css';

const route = useRoute();
const resumeStore = useResumeStore();

const resumeId = route.params.id || null;
const editorResumeId = computed(() => {
  const parsedValue = Number(resumeId);
  return Number.isFinite(parsedValue) && parsedValue > 0 ? parsedValue : null;
});
const locale = ref('zh');

// ── Auto-save state ──────────────────────────────────────────────────────
let saveTimeout = null;
const saveStatus = ref('idle'); // 'idle' | 'pending' | 'saving' | 'saved' | 'error'
const pendingData = ref(null);
const exportingPdf = ref(false);
const resumeTemplateRef = ref(null);

const saveStatusText = computed(() => {
  switch (saveStatus.value) {
    case 'pending': return 'Unsaved changes';
    case 'saving':  return 'Saving...';
    case 'saved':   return 'Saved';
    case 'error':   return 'Save failed';
    default:        return '';
  }
});

const saveStatusClass = computed(() => ({
  'indicator-pending': saveStatus.value === 'pending',
  'indicator-saving': saveStatus.value === 'saving',
  'indicator-saved':  saveStatus.value === 'saved',
  'indicator-error':  saveStatus.value === 'error',
}));

// Collect the full resume data from the current sections state
function buildResumePayload() {
  const resumeData = {};
  sections.forEach((s) => {
    resumeData[s.id] = s.contentJson;
  });
  return { sections: resumeData };
}

function buildSectionCreatePayload(block) {
  return {
    sectionCode: block.sectionCode,
    sectionTitle: block.sectionTitle,
    schemaType: block.schemaType,
    contentJson: block.contentJson ?? {},
    visible: block.visible ?? true,
  };
}

function scheduleAutoSave() {
  if (!resumeId) return;
  pendingData.value = buildResumePayload();
  saveStatus.value = 'pending';

  if (saveTimeout) clearTimeout(saveTimeout);
  saveTimeout = setTimeout(async () => {
    if (pendingData.value === null) return;
    saveStatus.value = 'saving';
    try {
      await resumeStore.saveResume(resumeId, pendingData.value);
      saveStatus.value = 'saved';
      pendingData.value = null;
      // Reset to idle after a brief moment
      setTimeout(() => { saveStatus.value = 'idle'; }, 2000);
    } catch {
      saveStatus.value = 'error';
    }
  }, 1500);
}

async function onMainSave() {
  if (!resumeId) return;
  saveStatus.value = 'saving';
  try {
    const data = buildResumePayload();
    await resumeStore.saveResume(resumeId, data);
    saveStatus.value = 'saved';
    setTimeout(() => { saveStatus.value = 'idle'; }, 2000);
  } catch {
    saveStatus.value = 'error';
  }
}

// ── Mount: load resume from API ───────────────────────────────────────────
onMounted(async () => {
  if (resumeId) {
    try {
      await resumeStore.fetchResume(resumeId);
      // New resume (no sections yet): auto-fill all system modules
      if (sections.length === 0) {
        for (const block of DEFAULT_SECTIONS) {
          await resumeStore.addSectionToApi(resumeId, buildSectionCreatePayload(block));
        }
      }
    } catch {
      // error is set on the store
    }
  }
});

// Default sections pre-filled for brand-new resumes
const DEFAULT_SECTIONS = [
  {
    sectionType: 'SYSTEM',
    sectionCode: 'BASIC',
    sectionTitle: '基本信息',
    schemaType: 'TEXT',
    contentJson: { name: '张三', title: 'Java 后端开发工程师', email: 'zhangsan@example.com', phone: '138-0013-8000', location: '北京市朝阳区', wechat: 'zhangsan_dev', github: 'github.com/zhangsan', website: 'zhangsan.com' },
  },
  {
    sectionType: 'SYSTEM',
    sectionCode: 'JOB_INTENT',
    sectionTitle: '求职意向',
    schemaType: 'TEXT',
    contentJson: { desiredPosition: 'Java 后端开发工程师', desiredCity: '北京', salaryRange: '25K-35K', employmentType: '全职', jobStatus: '随时可入职' },
  },
  {
    sectionType: 'SYSTEM',
    sectionCode: 'EDUCATION',
    sectionTitle: '教育背景',
    schemaType: 'LIST',
    contentJson: { items: [{ text: '北京理工大学 · 计算机科学与技术 · 硕士 · 2019.09 - 2022.06' }, { text: '天津大学 · 软件工程 · 本科 · 2015.09 - 2019.06' }] },
  },
  {
    sectionType: 'SYSTEM',
    sectionCode: 'EXPERIENCE',
    sectionTitle: '工作经验',
    schemaType: 'LIST',
    contentJson: { items: [{ text: '字节跳动 · 后端开发工程师 · 2022.07 - 至今\n• 负责抖音评论服务设计与开发，日均处理请求量超过 5000 万\n• 优化缓存架构，采用 Redis Cluster 集群方案，接口响应时间降低 40%\n• 主导技术选型，推动 Spring Boot + gRPC 框架落地，提升团队开发效率 30%' }, { text: '阿里巴巴 · Java 开发实习生 · 2021.03 - 2021.09\n• 参与电商订单系统开发，完成订单履约流程模块设计与实现\n• 基于 Kafka 实现订单状态异步通知，日均消息吞吐量达百万级' }] },
  },
  {
    sectionType: 'SYSTEM',
    sectionCode: 'CAMPUS',
    sectionTitle: '校园经历',
    schemaType: 'LIST',
    contentJson: { items: [{ text: '校ACM竞赛团队 · 队长 · 2017.09 - 2019.06\n• 带领团队获省级ACM程序设计竞赛银奖\n• 组织校内算法竞赛，累计参与人数超过 500 人' }, { text: '计算机学院学生会 · 副主席 · 2016.09 - 2018.06\n• 策划并执行学院科技文化节，参与人数达 2000+ 人' }] },
  },
  {
    sectionType: 'SYSTEM',
    sectionCode: 'SKILLS',
    sectionTitle: '技能特长',
    schemaType: 'TAGS',
    contentJson: { items: ['Java', 'Spring Boot', 'MySQL', 'Redis', 'Kafka', 'Docker', 'Kubernetes', 'Git', 'Linux'] },
  },
  {
    sectionType: 'SYSTEM',
    sectionCode: 'CERTIFICATES',
    sectionTitle: '荣誉证书',
    schemaType: 'LIST',
    contentJson: { items: [{ text: 'Oracle Certified Professional, Java SE 8 Programmer (OCP)' }, { text: 'ACM-ICPC 省级银奖' }, { text: '校级优秀毕业生' }, { text: '研究生国家奖学金' }] },
  },
  {
    sectionType: 'SYSTEM',
    sectionCode: 'SELF_EVALUATION',
    sectionTitle: '自我评价',
    schemaType: 'TEXT',
    contentJson: { text: '拥有扎实的 Java 基础和良好的面向对象设计能力，熟悉微服务架构设计。具备独立负责业务模块的能力，善于与产品、测试团队高效沟通。热爱技术，持续关注新技术发展，具有较强的学习能力和问题解决能力。' },
  },
  {
    sectionType: 'SYSTEM',
    sectionCode: 'PROJECTS',
    sectionTitle: '项目经历',
    schemaType: 'LIST',
    contentJson: { items: [{ text: '分布式任务调度平台 · 2023.03 - 2023.08\n• 采用 XXL-JOB 二次开发，支持集群部署与任务分片\n• 实现了可视化任务配置、任务依赖管理及失败告警\n• 日均调度任务量达 10 万+，系统可用性 99.99%' }, { text: '实时数据同步中间件 · 2022.10 - 2023.02\n• 基于 Canal + Flink 实现 MySQL 到 Elasticsearch 实时数据同步\n• 支持全量与增量同步，平均延迟控制在 1 秒以内\n• 支撑搜索业务场景，日均处理数据量超 5000 万条' }] },
  },
  {
    sectionType: 'SYSTEM',
    sectionCode: 'INTERNSHIP',
    sectionTitle: '实习经历',
    schemaType: 'LIST',
    contentJson: { items: [{ text: '阿里巴巴 · Java开发实习生 · 2021.03 - 2021.09\n• 参与电商订单系统开发，完成订单履约流程模块设计与实现\n• 基于 Kafka 实现订单状态异步通知，日均消息吞吐量达百万级' }] },
  },
];

// ── Store shortcuts (backward-compatible with existing editors) ───────────
const store = {
  sections,
  visibleSections,
  selectedTemplate,
  sidebarColor,
  resumeBodyFontSize,
  toggleVisible,
  removeSection,
  getSectionContent,
};

const activeId = ref(null);
const showAddBlock = ref(false);
const layout = ref('edit-left');
const panelWidth = ref(40);
const previewWidth = ref(60);
const isDragging = ref(false);
let dragStartX = 0;

const templates = [
  { value: 'classic', label: '经典' },
  { value: 'modern', label: '现代' },
  { value: 'creative', label: '创意' },
];

const bodyFontOptions = [9, 10, 11, 12];
const sidebarColorOptions = [
  { value: 'black', label: '黑', swatch: 'linear-gradient(180deg, #111827 0%, #1f2937 100%)' },
  { value: 'blue', label: '蓝', swatch: 'linear-gradient(180deg, #1e3a8a 0%, #2563eb 100%)' },
  { value: 'white', label: '白', swatch: 'linear-gradient(180deg, #ffffff 0%, #f3f4f6 100%)' },
  { value: 'gray', label: '灰', swatch: 'linear-gradient(180deg, #e5e7eb 0%, #f8fafc 100%)' },
];

const editorMap = {
  'BASIC':        PersonalInfoEditor,
  'JOB_INTENT':   JobIntentEditor,
  'EDUCATION':    EducationEditor,
  'EXPERIENCE':   ExperienceEditor,
  'CAMPUS':       CampusEditor,
  'SKILLS':       SkillsEditor,
  'CERTIFICATES': CertificatesEditor,
  'SELF_EVALUATION': SelfEvaluationEditor,
  'PROJECTS':     ProjectsEditor,
  'INTERNSHIP':   InternshipEditor,
};

const activeSection = computed(() =>
  store.sections.find((s) => s.id === activeId.value) || null
);

const layoutClass = computed(() => layout.value);
const supportsSidebarColor = computed(() => store.selectedTemplate.value === 'modern');

const LIST_TYPES = new Set([
  'EDUCATION', 'EXPERIENCE', 'CAMPUS', 'SKILLS',
  'CERTIFICATES', 'PROJECTS', 'INTERNSHIP', 'CUSTOM',
]);

const activeEditor = computed(() => {
  const code = activeSection.value?.sectionCode;
  if (code && editorMap[code]) return editorMap[code];
  return GenericEditor;
});

const activeSectionContent = computed(() => {
  if (!activeId.value) return null;
  const content = store.getSectionContent(activeId.value);
  if (!content) return null;
  const code = activeSection.value?.sectionCode;
  const schema = activeSection.value?.schemaType;
  if (schema === 'LIST' || schema === 'TAGS') return content.items ?? [];
  if (schema === 'TEXT' && code !== 'BASIC') return content.text ?? '';
  return content;
});

function toggleLayout() {
  layout.value = layout.value === 'edit-left' ? 'edit-right' : 'edit-left';
}

function toggleActive(id) {
  activeId.value = activeId.value === id ? null : id;
}

function onEditorUpdate(newContent) {
  if (!activeId.value) return;
  const code = activeSection.value?.sectionCode;
  const schema = activeSection.value?.schemaType;
  if (schema === 'LIST' || schema === 'TAGS') {
    setSectionContent(activeId.value, { items: newContent });
  } else if (schema === 'TEXT' && code !== 'BASIC') {
    setSectionContent(activeId.value, { text: newContent });
  } else {
    setSectionContent(activeId.value, newContent);
  }
  scheduleAutoSave();
}

async function onBlockAdded(blockData) {
  if (!blockData) {
    showAddBlock.value = false;
    return;
  }

  try {
    if (editorResumeId.value) {
      await resumeStore.addSectionToApi(editorResumeId.value, buildSectionCreatePayload(blockData));
    } else {
      addSection(blockData);
      scheduleAutoSave();
    }
  } catch (error) {
    console.error('添加模块失败', error);
    window.alert(extractApiMessage(error, '添加模块失败，请稍后重试'));
    return;
  }

  showAddBlock.value = false;
}

async function onAgentApproved() {
  if (!resumeId) return;
  pendingData.value = null;
  if (saveTimeout) {
    clearTimeout(saveTimeout);
    saveTimeout = null;
  }
  try {
    await resumeStore.fetchResume(resumeId);
    saveStatus.value = 'saved';
    setTimeout(() => { saveStatus.value = 'idle'; }, 2000);
  } catch {
    saveStatus.value = 'error';
  }
}

function buildExportFilename() {
  const basicSection = sections.find((section) => section.sectionCode === 'BASIC');
  const rawName = String(basicSection?.contentJson?.name || 'resume').trim();
  const safeName = rawName
    .replace(/[<>:"/\\|?*\x00-\x1F]/g, '')
    .replace(/\s+/g, '_');

  return safeName || 'resume';
}

async function onExportPdf() {
  if (exportingPdf.value) return;

  const exportElement = resumeTemplateRef.value?.getExportElement?.();
  if (!exportElement) {
    window.alert('未找到可导出的简历区域');
    return;
  }

  exportingPdf.value = true;
  try {
    await exportToPDF(exportElement, buildExportFilename());
  } catch (error) {
    console.error('导出 PDF 失败', error);
    window.alert('导出 PDF 失败，请稍后重试');
  } finally {
    exportingPdf.value = false;
  }
}

let draggedId = ref(null);
let dragOverId = ref(null);

function onDragStart(id, event) {
  draggedId.value = id;
  event.dataTransfer.effectAllowed = 'move';
  event.dataTransfer.setData('text/plain', id);
}

function onDragOver(id) {
  if (draggedId.value && draggedId.value !== id) {
    dragOverId.value = id;
  }
}

function onDragLeave() {
  dragOverId.value = null;
}

function onDrop(targetId) {
  if (!draggedId.value || draggedId.value === targetId) return;
  reorderSections(draggedId.value, targetId);
  dragOverId.value = null;
  scheduleAutoSave();
}

function onDragEnd() {
  draggedId.value = null;
  dragOverId.value = null;
}

function onResizeStart(event) {
  isDragging.value = true;
  dragStartX = event.clientX;
  document.addEventListener('mousemove', onResizeMove);
  document.addEventListener('mouseup', onResizeEnd);
  event.preventDefault();
}

function onResizeMove(event) {
  if (!isDragging.value) return;
  const layoutEl = document.querySelector('.editor-layout');
  if (!layoutEl) return;
  const totalWidth = layoutEl.offsetWidth;
  const delta = (event.clientX - dragStartX) / totalWidth * 100;
  dragStartX = event.clientX;

  if (layout.value === 'edit-left') {
    panelWidth.value   = Math.min(70, Math.max(20, panelWidth.value   + delta));
    previewWidth.value = Math.min(70, Math.max(20, previewWidth.value - delta));
  } else {
    panelWidth.value   = Math.min(70, Math.max(20, panelWidth.value   - delta));
    previewWidth.value = Math.min(70, Math.max(20, previewWidth.value + delta));
  }
}

function onResizeEnd() {
  isDragging.value = false;
  document.removeEventListener('mousemove', onResizeMove);
  document.removeEventListener('mouseup', onResizeEnd);
}
</script>

<style scoped>
.editor-layout {
  --green-primary: #22c55e;
  --green-light: #4ade80;
  --green-dark: #16a34a;
  --white-bg: #ffffff;
  --gray-bg: #f9fafb;
  --gray-card: #ffffff;
  --text-dark: #1a1a2e;
  --text-body: #374151;
  --text-muted: #6b7280;
}

.editor-layout {
  display: flex;
  height: 100vh;
  overflow: hidden;
  font-family: 'PingFang SC', 'Microsoft YaHei', sans-serif;
  background: var(--gray-bg);
}

/* ── Save indicator ─────────────────────────────────────────────────── */
.save-indicator {
  font-size: 12px;
  font-weight: 600;
  padding: 3px 10px;
  border-radius: 20px;
  transition: background 0.2s, color 0.2s;
  white-space: nowrap;
}

.indicator-pending {
  background: #fef3c7;
  color: #92400e;
}

.indicator-saving {
  background: #dbeafe;
  color: #1e40af;
}

.indicator-saved {
  background: #d1fae5;
  color: #065f46;
}

.indicator-error {
  background: #fee2e2;
  color: #991b1b;
}

/* ── Edit Panel ──────────────────────────────────────────────────────── */
.edit-panel {
  display: flex;
  flex-direction: column;
  background: var(--white-bg);
  border-right: 1px solid #bbf7d0;
  position: relative;
  overflow: hidden;
  flex-shrink: 0;
}

/* ── Panel Header ────────────────────────────────────────────────────── */
.panel-header {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 20px 20px 16px;
  border-bottom: 1px solid #e5e7eb;
  background: var(--white-bg);
  flex-shrink: 0;
}

.panel-title {
  font-size: 16px;
  font-weight: 700;
  color: var(--green-dark);
}

.panel-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}

/* ── Template Selector ───────────────────────────────────────────────── */
.template-selector {
  display: flex;
  gap: 4px;
  background: #f3f4f6;
  border-radius: 8px;
  padding: 3px;
}

.template-btn {
  padding: 5px 12px;
  border: none;
  background: transparent;
  border-radius: 6px;
  font-size: 13px;
  font-weight: 500;
  color: var(--text-muted);
  cursor: pointer;
  transition: background 0.15s, color 0.15s;
}

.template-btn.active {
  background: var(--green-primary);
  color: #fff;
  font-weight: 600;
}

.template-btn:hover:not(.active) {
  color: var(--green-dark);
  background: #dcfce7;
}

.font-size-selector {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 3px 4px 3px 10px;
  border: 1px solid #d1fae5;
  border-radius: 10px;
  background: #f0fdf4;
}

.font-size-label {
  color: var(--green-dark);
  font-size: 12px;
  font-weight: 700;
  white-space: nowrap;
}

.font-size-btn {
  padding: 5px 8px;
  border: none;
  border-radius: 7px;
  background: transparent;
  color: var(--text-muted);
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.15s, color 0.15s;
  white-space: nowrap;
}

.font-size-btn.active {
  background: #22c55e;
  color: #ffffff;
}

.font-size-btn:hover:not(.active) {
  background: #dcfce7;
  color: var(--green-dark);
}

.sidebar-color-selector {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 4px 6px 4px 10px;
  border: 1px solid #d1fae5;
  border-radius: 10px;
  background: #f0fdf4;
}

.sidebar-color-label {
  color: var(--green-dark);
  font-size: 12px;
  font-weight: 700;
  white-space: nowrap;
}

.sidebar-color-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 5px 8px;
  border: none;
  border-radius: 8px;
  background: transparent;
  color: var(--text-muted);
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.15s, color 0.15s, box-shadow 0.15s;
}

.sidebar-color-btn__swatch {
  width: 14px;
  height: 14px;
  border-radius: 999px;
  border: 1px solid rgba(148, 163, 184, 0.35);
  flex-shrink: 0;
}

.sidebar-color-btn.active {
  background: #22c55e;
  color: #ffffff;
}

.sidebar-color-btn:hover:not(.active) {
  background: #dcfce7;
  color: var(--green-dark);
}

.export-pdf-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 6px 14px;
  border: 1.5px solid #0f766e;
  border-radius: 8px;
  background: #ffffff;
  color: #0f766e;
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
  transition: background 0.15s, color 0.15s, border-color 0.15s, opacity 0.15s;
  white-space: nowrap;
}

.export-pdf-btn:hover:not(:disabled) {
  background: #0f766e;
  color: #ffffff;
}

.export-pdf-btn:disabled {
  opacity: 0.65;
  cursor: not-allowed;
}

/* ── Add Block Button ───────────────────────────────────────────────── */
.add-block-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 14px;
  border: 1.5px solid var(--green-primary);
  border-radius: 8px;
  background: transparent;
  color: var(--green-primary);
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.15s, color 0.15s;
  white-space: nowrap;
}

.add-block-btn:hover {
  background: var(--green-primary);
  color: #fff;
}

/* ── Block List ─────────────────────────────────────────────────────── */
.block-list-container {
  flex: 1;
  overflow-y: auto;
  padding: 12px;
}

.block-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.block-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 12px;
  background: var(--white-bg);
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  cursor: pointer;
  transition: border-color 0.15s, background 0.15s, box-shadow 0.15s;
  user-select: none;
}

.block-item:hover {
  border-color: var(--green-light);
  box-shadow: 0 2px 8px rgba(34, 197, 94, 0.15);
}

.block-item.active {
  border-color: var(--green-primary);
  background: #f0fdf4;
  box-shadow: 0 0 0 3px rgba(34, 197, 94, 0.1);
}

.block-item.drag-over {
  border-color: var(--green-primary);
  border-style: dashed;
  background: #f0fdf4;
}

.block-item.hidden {
  opacity: 0.45;
}

.block-item.hidden .block-name {
  text-decoration: line-through;
}

.drag-handle {
  color: #9ca3af;
  cursor: grab;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  padding: 2px;
  font-size: 14px;
  transition: color 0.15s;
}

.drag-handle:hover {
  color: var(--green-primary);
}

.drag-handle:active {
  cursor: grabbing;
}

.eye-btn {
  background: none;
  border: none;
  cursor: pointer;
  font-size: 14px;
  padding: 2px 4px;
  flex-shrink: 0;
  transition: opacity 0.15s;
}

.eye-btn:hover {
  opacity: 0.6;
}

.block-name {
  flex: 1;
  font-size: 14px;
  font-weight: 500;
  color: var(--text-body);
}

.block-item.active .block-name {
  color: var(--green-dark);
}

/* ── Swap Button ────────────────────────────────────────────────────── */
.swap-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  border: 1.5px solid #e5e7eb;
  background: var(--white-bg);
  border-radius: 8px;
  color: var(--text-muted);
  cursor: pointer;
  transition: all 0.15s;
  flex-shrink: 0;
}

.swap-btn:hover {
  border-color: var(--green-primary);
  color: var(--green-primary);
  background: #dcfce7;
}

/* ── Main Save Button ───────────────────────────────────────────────── */
.main-save-btn {
  padding: 6px 14px;
  border: none;
  border-radius: 8px;
  background: var(--green-primary);
  color: #fff;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.15s, opacity 0.15s;
  white-space: nowrap;
  font-family: inherit;
}

.main-save-btn:hover:not(:disabled) {
  background: var(--green-dark);
}

.main-save-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

/* ── Delete Button ───────────────────────────────────────────────────── */
.delete-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  border: none;
  background: transparent;
  border-radius: 6px;
  color: #9ca3af;
  cursor: pointer;
  transition: background 0.15s, color 0.15s;
  flex-shrink: 0;
  opacity: 0;
  font-size: 16px;
  line-height: 1;
}

.block-item:hover .delete-btn {
  opacity: 1;
}

.delete-btn:hover {
  background: #fef2f2;
  color: #ef4444;
}

/* ── Editor Area ────────────────────────────────────────────────────── */
.editor-area {
  border-top: 1px solid #e5e7eb;
  padding: 16px;
  background: var(--white-bg);
  flex-shrink: 0;
  max-height: 50vh;
  overflow-y: auto;
}

.editor-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.editor-title {
  font-size: 14px;
  font-weight: 700;
  color: var(--green-dark);
}

.close-editor-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 26px;
  height: 26px;
  border: none;
  background: #f3f4f6;
  border-radius: 6px;
  color: var(--text-muted);
  cursor: pointer;
  transition: background 0.15s, color 0.15s;
  font-size: 16px;
  line-height: 1;
}

.close-editor-btn:hover {
  background: #e5e7eb;
  color: var(--text-body);
}

/* ── Add Block Drawer ───────────────────────────────────────────────── */
.add-block-drawer {
  position: absolute;
  right: 0;
  top: 0;
  bottom: 0;
  width: 320px;
  background: var(--white-bg);
  box-shadow: -4px 0 16px rgba(0, 0, 0, 0.1);
  z-index: 50;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.drawer-backdrop {
  position: absolute;
  inset: 0;
  background: rgba(34, 197, 94, 0.08);
  z-index: 40;
}

.drawer-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px;
  border-bottom: 1px solid #e5e7eb;
  flex-shrink: 0;
}

.drawer-title {
  font-size: 16px;
  font-weight: 700;
  color: var(--green-dark);
}

.close-drawer-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border: none;
  background: #f3f4f6;
  border-radius: 6px;
  color: var(--text-muted);
  cursor: pointer;
  transition: background 0.15s, color 0.15s;
  font-size: 16px;
  line-height: 1;
}

.close-drawer-btn:hover {
  background: #e5e7eb;
  color: var(--text-body);
}

.drawer-body {
  flex: 1;
  overflow-y: auto;
}

.drawer-enter-active,
.drawer-leave-active {
  transition: transform 0.25s ease, opacity 0.25s ease;
}

.drawer-enter-from,
.drawer-leave-to {
  transform: translateX(100%);
  opacity: 0;
}

/* ── Layout Swap ─────────────────────────────────────────────────────── */
.editor-layout.edit-right .edit-panel { order: 2; }
.editor-layout.edit-right .preview-panel { order: 0; }
.editor-layout.edit-right .resize-handle { order: 1; }

/* ── Resize Handle ───────────────────────────────────────────────────── */
.resize-handle {
  width: 6px;
  cursor: col-resize;
  background: #e5e7eb;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background 0.15s;
  user-select: none;
}

.resize-handle:hover,
.resize-handle:active {
  background: var(--green-primary);
}

.resize-grip {
  width: 2px;
  height: 32px;
  border-radius: 2px;
  background: #d1d5db;
  transition: background 0.15s;
}

.resize-handle:hover .resize-grip,
.resize-handle:active .resize-grip {
  background: rgba(255, 255, 255, 0.8);
}

/* ── Preview Panel ───────────────────────────────────────────────────── */
.preview-panel {
  flex: 1;
  flex-shrink: 0;
  overflow: auto;
  background:
    radial-gradient(circle at top, rgba(34, 197, 94, 0.08), transparent 28%),
    linear-gradient(180deg, #f4f7f7 0%, #e9eef2 100%);
  padding: 28px;
}

.resume-preview-container {
  display: flex;
  justify-content: center;
  width: 100%;
  margin: 0 auto;
  padding-bottom: 28px;
  background: transparent;
  box-shadow: none;
}
</style>
