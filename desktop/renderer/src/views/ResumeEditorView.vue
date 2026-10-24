<template>
  <div ref="workbenchRef" class="editor-workbench">
    <!-- ── 顶栏：页面标题 / 保存状态 / 主操作 ───────────────── -->
    <header class="editor-topbar">
      <div class="editor-topbar__lead">
        <h1 class="ui-page-title editor-topbar__title">简历编辑器</h1>
        <span class="editor-topbar__subtitle">{{ resumeName || '未命名简历' }}</span>
      </div>

      <div class="editor-topbar__status">
        <button
          v-if="saveStatus === 'error'"
          type="button"
          class="save-indicator is-error"
          @click="onMainSave"
        >
          保存失败，点击重试
        </button>
        <span v-else class="save-indicator" :class="saveStatusClass">{{ saveStatusText }}</span>
      </div>

      <div class="editor-topbar__actions">
        <button
          type="button"
          class="ui-btn ui-btn--sm ui-btn--ghost"
          title="交换编辑区与预览的位置"
          @click="toggleLayout"
        >
          <AppIcon name="swap" :size="16" />
          交换位置
        </button>
        <button type="button" class="ui-btn ui-btn--sm ui-btn--outline" @click="togglePreview">
          <AppIcon :name="previewVisible ? 'eye' : 'eye-slash'" :size="16" />
          {{ previewVisible ? '隐藏预览' : '显示预览' }}
        </button>
        <button type="button" class="ui-btn ui-btn--sm ui-btn--outline" :disabled="exportingPdf" @click="onExportPdf">
          <AppIcon name="download-simple" :size="16" />
          {{ exportingPdf ? '导出中...' : '导出 PDF' }}
        </button>
        <button type="button" class="ui-btn ui-btn--sm ui-btn--primary" :disabled="saveStatus === 'saving'" @click="onMainSave">
          <AppIcon name="check" :size="16" />
          {{ saveStatus === 'saving' ? '保存中...' : '保存' }}
        </button>
      </div>
    </header>

    <!-- ── 三栏主体 ─────────────────────────────────────────── -->
    <div class="editor-body" :class="{ 'is-preview-first': layout === 'edit-right' }">
      <!-- 左栏：模块导航 + 操作区 -->
      <aside class="editor-nav" :style="navStyle">
        <section class="editor-nav__group editor-nav__group--grow">
          <div class="editor-nav__group-head">
            <button
              type="button"
              class="ui-icon-btn editor-nav__collapse"
              :aria-expanded="navListExpanded"
              title="折叠 / 展开模块列表"
              @click="navListExpanded = !navListExpanded"
            >
              <AppIcon :name="navListExpanded ? 'caret-down' : 'caret-right'" :size="16" />
            </button>
            <span class="section-nav__caption">简历模块</span>
            <button
              type="button"
              class="ui-icon-btn"
              title="添加模块"
              aria-label="添加模块"
              @click="showAddBlock = true"
            >
              <AppIcon name="plus" :size="16" />
            </button>
          </div>

          <ul v-show="navListExpanded" class="section-nav__list ui-scroll">
            <li
              v-for="section in store.sections"
              :key="section.id"
              class="section-nav__item"
              :class="{
                'is-active': activeId === section.id,
                'is-hidden': !section.visible,
                'is-drag-over': dragOverId === section.id,
              }"
              draggable="true"
              @dragstart="onDragStart(section.id, $event)"
              @dragover.prevent="onDragOver(section.id)"
              @dragleave="onDragLeave"
              @drop.prevent="onDrop(section.id)"
              @dragend="onDragEnd"
              @click="toggleActive(section.id)"
            >
              <span class="section-nav__handle" title="拖拽调整顺序" aria-hidden="true">
                <AppIcon name="dots-three-vertical" :size="14" />
              </span>
              <span class="section-nav__title">{{ section.sectionTitle }}</span>
              <button
                type="button"
                class="ui-icon-btn section-nav__action"
                :title="section.visible ? '在简历中隐藏' : '在简历中显示'"
                :aria-label="section.visible ? '在简历中隐藏' : '在简历中显示'"
                @click.stop="store.toggleVisible(section.id)"
              >
                <AppIcon :name="section.visible ? 'eye' : 'eye-slash'" :size="16" />
              </button>
              <button
                type="button"
                class="ui-icon-btn section-nav__action section-nav__action--danger"
                title="删除模块"
                aria-label="删除模块"
                @click.stop="onRemoveSection(section.id, section.sectionTitle)"
              >
                <AppIcon name="trash" :size="16" />
              </button>
            </li>
          </ul>

          <p v-if="!store.sections.length" class="editor-nav__empty">
            还没有模块，点击右上角「添加模块」创建。
          </p>
        </section>

        <section class="editor-nav__group">
          <span class="section-nav__caption">外观</span>

          <div class="editor-nav__field">
            <label class="ui-label" for="editor-template-select">模板</label>
            <select
              id="editor-template-select"
              class="ui-input ui-select"
              :value="store.selectedTemplate.value"
              @change="store.selectedTemplate.value = $event.target.value"
            >
              <option v-for="t in templates" :key="t.value" :value="t.value">{{ t.label }}</option>
            </select>
          </div>

          <div class="editor-nav__field">
            <span class="ui-label">正文字号</span>
            <div class="ui-segmented editor-nav__segmented" role="group" aria-label="正文字号">
              <button
                v-for="size in bodyFontOptions"
                :key="size"
                type="button"
                class="ui-segmented__item"
                :class="{ 'is-active': store.resumeBodyFontSize.value === size }"
                @click="setResumeBodyFontSize(size)"
              >
                {{ size }}px
              </button>
            </div>
          </div>

          <div v-if="supportsSidebarColor" class="editor-nav__field">
            <span class="ui-label">左栏配色</span>
            <div class="editor-nav__chips">
              <button
                v-for="color in sidebarColorOptions"
                :key="color.value"
                type="button"
                class="editor-nav__chip"
                :class="{ 'is-active': store.sidebarColor.value === color.value }"
                @click="setSidebarColor(color.value)"
              >
                <span class="editor-nav__swatch" :style="{ background: color.swatch }"></span>
                {{ color.label }}
              </button>
            </div>
          </div>
        </section>

        <section class="editor-nav__group">
          <span class="section-nav__caption">操作</span>
          <div class="editor-nav__actions">
            <button type="button" class="ui-btn ui-btn--sm ui-btn--outline ui-btn--block" :disabled="saveStatus === 'saving'" @click="onMainSave">
              保存全部
            </button>
            <button type="button" class="ui-btn ui-btn--sm ui-btn--outline ui-btn--block" :disabled="exportingPdf" @click="onExportPdf">
              {{ exportingPdf ? '导出中...' : '导出 PDF' }}
            </button>
            <button type="button" class="ui-btn ui-btn--sm ui-btn--outline ui-btn--block" @click="openAgentCopilot">
              AI 助手
            </button>
          </div>
        </section>
      </aside>

      <div
        class="editor-splitter editor-splitter--nav"
        :class="{ 'is-active': isDragging && resizeTarget === 'nav' }"
        role="separator"
        aria-orientation="vertical"
        title="拖拽调整左栏宽度"
        @mousedown="startResize('nav', $event)"
      ></div>

      <!-- 中栏：当前分区编辑区 -->
      <main class="editor-canvas ui-scroll">
        <div v-if="pageLoading" class="editor-canvas__inner editor-canvas__loading">
          <div class="ui-skeleton editor-canvas__skeleton editor-canvas__skeleton--title"></div>
          <div class="ui-skeleton editor-canvas__skeleton"></div>
          <div class="ui-skeleton editor-canvas__skeleton"></div>
          <div class="ui-skeleton editor-canvas__skeleton editor-canvas__skeleton--short"></div>
        </div>

        <div v-else-if="loadError" class="editor-canvas__inner">
          <div class="ui-callout ui-callout--danger editor-canvas__callout">
            <AppIcon name="warning-circle" :size="18" />
            <div>
              <strong>简历加载失败</strong>
              <p>{{ loadError }}。请确认后端服务已启动，然后返回简历列表重新打开，或稍后在顶部点击「保存」重试。</p>
            </div>
          </div>
        </div>

        <div v-else-if="activeId && activeSection" class="editor-canvas__inner">
          <header class="ui-section-head editor-canvas__head">
            <div class="editor-canvas__heading">
              <h2 class="ui-section-head__title">{{ activeSection.sectionTitle }}</h2>
              <p class="ui-section-head__hint">修改会实时反映到{{ layout === 'edit-left' ? '右侧' : '左侧' }}预览，停止输入 1.5 秒后自动保存。</p>
            </div>
            <div class="editor-canvas__tools">
              <button
                type="button"
                class="ui-icon-btn"
                :title="sectionExpanded ? '收起该模块' : '展开该模块'"
                :aria-label="sectionExpanded ? '收起该模块' : '展开该模块'"
                :aria-expanded="sectionExpanded"
                @click="sectionExpanded = !sectionExpanded"
              >
                <AppIcon :name="sectionExpanded ? 'caret-down' : 'caret-right'" :size="16" />
              </button>
              <button type="button" class="ui-btn ui-btn--sm ui-btn--ghost" @click="activeId = null">收起</button>
            </div>
          </header>

          <section class="ui-panel editor-canvas__section">
            <component
              :is="activeEditor"
              :key="activeId"
              :model-value="activeSectionContent"
              :is-active="sectionExpanded"
              :resume-id="editorResumeId"
              :schema-type="activeSection?.schemaType ?? 'TEXT'"
              :is-custom="activeSection?.sectionType === 'CUSTOM'"
              @update:model-value="onEditorUpdate"
            />
          </section>
        </div>

        <div v-else class="ui-empty editor-canvas__empty">
          <div class="ui-empty__icon">
            <AppIcon name="squares-four" :size="24" />
          </div>
          <div class="ui-empty__title">选择一个模块开始编辑</div>
          <div class="ui-empty__hint">在左侧「简历模块」中点选要编辑的分区，右侧会实时预览排版效果。</div>
        </div>
      </main>

      <template v-if="previewVisible && !isNarrow">
        <div
          class="editor-splitter editor-splitter--preview"
          :class="{ 'is-active': isDragging && resizeTarget === 'preview' }"
          role="separator"
          aria-orientation="vertical"
          title="拖拽调整预览宽度"
          @mousedown="startResize('preview', $event)"
        ></div>
      </template>

      <!-- 右栏：实时预览 -->
      <aside
        v-if="previewVisible"
        class="editor-preview"
        :class="{ 'editor-preview--drawer': isNarrow }"
        :style="isNarrow ? null : { width: `${previewWidth}px` }"
      >
        <header class="editor-preview__head">
          <span class="editor-preview__title">
            <AppIcon name="file-text" :size="16" />
            实时预览
          </span>
          <span class="ui-badge">A4</span>
        </header>
        <div ref="previewViewportRef" class="editor-preview__viewport ui-scroll">
          <div
            class="editor-preview__fit"
            :style="{
              width: `${PREVIEW_PAGE_WIDTH * previewScale}px`,
              height: `${previewContentHeight * previewScale}px`,
            }"
          >
            <div
              ref="previewStageRef"
              class="editor-preview__stage"
              :style="{ transform: `scale(${previewScale})` }"
            >
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
        </div>
      </aside>
    </div>

    <div v-if="isNarrow && previewVisible" class="editor-preview__backdrop" @click="narrowPreviewOpen = false"></div>

    <!-- ── 添加模块抽屉 ─────────────────────────────────────── -->
    <Transition name="editor-drawer">
      <aside v-if="showAddBlock" class="editor-drawer">
        <header class="editor-drawer__head">
          <span class="editor-drawer__title">添加模块</span>
          <button
            type="button"
            class="ui-icon-btn"
            title="关闭"
            aria-label="关闭"
            @click="showAddBlock = false"
          >
            <AppIcon name="x" :size="16" />
          </button>
        </header>
        <div class="editor-drawer__body">
          <AddBlock :added-section-codes="store.sections.map(s => s.sectionCode)" @add-block="onBlockAdded" />
        </div>
      </aside>
    </Transition>
    <div v-if="showAddBlock" class="editor-drawer__backdrop" @click="showAddBlock = false"></div>

    <FloatingAgentCopilot
      :resume-id="editorResumeId"
      :section="activeSection"
      @approved="onAgentApproved"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, nextTick, watch } from 'vue';
import { useRoute, onBeforeRouteLeave } from 'vue-router';
import AppIcon from '../components/common/AppIcon.vue';
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
import { sanitizeSectionContentForSave } from '../utils/resumeSectionNormalizer.js';
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

// A4 预览页宽度（与 resume-template-base.css 的 .resume-page 一致）
const PREVIEW_PAGE_WIDTH = 794;
const PREVIEW_PAGE_HEIGHT = 1123;

// ── 面板宽度（localStorage 记忆）────────────────────────────────────────
const NAV_WIDTH_KEY = 'resume_editor_nav_width';
const PREVIEW_WIDTH_KEY = 'resume_editor_preview_width';
const PREVIEW_COLLAPSED_KEY = 'resume_editor_preview_collapsed';

const NAV_MIN = 200;
const NAV_MAX = 360;
const NAV_NARROW = 200;
const PREVIEW_MIN = 320;
const PREVIEW_MAX = 760;
const CANVAS_MIN = 380;
// 工作台可用宽度小于该值时，右栏预览改为抽屉
const NARROW_BREAKPOINT = 1000;

function readStoredNumber(key, fallback, min, max) {
  if (typeof window === 'undefined') return fallback;
  const rawValue = Number(window.localStorage.getItem(key));
  if (!Number.isFinite(rawValue) || rawValue <= 0) return fallback;
  return Math.min(max, Math.max(min, rawValue));
}

function readStoredFlag(key) {
  if (typeof window === 'undefined') return false;
  return window.localStorage.getItem(key) === '1';
}

function persistStoredNumber(key, value) {
  if (typeof window === 'undefined') return;
  window.localStorage.setItem(key, String(Math.round(value)));
}

function persistStoredFlag(key, value) {
  if (typeof window === 'undefined') return;
  window.localStorage.setItem(key, value ? '1' : '0');
}

const navWidth = ref(readStoredNumber(NAV_WIDTH_KEY, 248, NAV_MIN, NAV_MAX));
const previewWidth = ref(readStoredNumber(PREVIEW_WIDTH_KEY, 460, PREVIEW_MIN, PREVIEW_MAX));
const previewCollapsed = ref(readStoredFlag(PREVIEW_COLLAPSED_KEY));

const isNarrow = ref(false);
const narrowPreviewOpen = ref(false);

const previewVisible = computed(() => (isNarrow.value ? narrowPreviewOpen.value : !previewCollapsed.value));

const navStyle = computed(() => ({
  width: `${isNarrow.value ? NAV_NARROW : navWidth.value}px`,
}));

function togglePreview() {
  if (isNarrow.value) {
    narrowPreviewOpen.value = !narrowPreviewOpen.value;
    return;
  }
  previewCollapsed.value = !previewCollapsed.value;
  persistStoredFlag(PREVIEW_COLLAPSED_KEY, previewCollapsed.value);
}

// 预览展开后重新计算缩放比例
watch(previewVisible, async (visible) => {
  if (!visible) return;
  await nextTick();
  observePreview();
});

// ── Auto-save state ──────────────────────────────────────────────────────
let saveTimeout = null;
const saveStatus = ref('idle'); // 'idle' | 'pending' | 'saving' | 'saved' | 'error'
const pendingData = ref(null);
const exportingPdf = ref(false);
const resumeTemplateRef = ref(null);

const saveStatusText = computed(() => {
  switch (saveStatus.value) {
    case 'pending': return '有未保存的修改';
    case 'saving':  return '保存中...';
    case 'saved':   return '已保存';
    case 'error':   return '保存失败';
    default:        return '内容已同步';
  }
});

const saveStatusClass = computed(() => ({
  'is-idle': saveStatus.value === 'idle',
  'is-pending': saveStatus.value === 'pending',
  'is-saving': saveStatus.value === 'saving',
  'is-saved':  saveStatus.value === 'saved',
  'is-error':  saveStatus.value === 'error',
}));

const hasUnsavedChanges = computed(() =>
  saveStatus.value === 'pending' || saveStatus.value === 'saving' || pendingData.value !== null
);

// Collect the full resume data from the current sections state
function buildResumePayload() {
  const resumeData = {};
  sections.forEach((s) => {
    resumeData[s.id] = sanitizeSectionContentForSave(s.sectionCode, s.contentJson);
  });
  return {
    sections: resumeData,
    template: store.selectedTemplate.value,
    sidebarColor: store.sidebarColor.value,
    fontSize: store.resumeBodyFontSize.value,
  };
}

function buildSectionCreatePayload(block) {
  const raw = block.contentJson ?? {};
  const sanitized = sanitizeSectionContentForSave(block.sectionCode, raw);
  return {
    sectionCode: block.sectionCode,
    sectionTitle: block.sectionTitle,
    schemaType: block.schemaType,
    contentJson: sanitized,
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
  if (saveTimeout) {
    clearTimeout(saveTimeout);
    saveTimeout = null;
  }
  saveStatus.value = 'saving';
  try {
    const data = buildResumePayload();
    await resumeStore.saveResume(resumeId, data);
    pendingData.value = null;
    saveStatus.value = 'saved';
    setTimeout(() => { saveStatus.value = 'idle'; }, 2000);
  } catch {
    saveStatus.value = 'error';
  }
}

// ── 预览缩放 ──────────────────────────────────────────────────────────────
const workbenchRef = ref(null);
const previewViewportRef = ref(null);
const previewStageRef = ref(null);
const previewScale = ref(1);
const previewContentHeight = ref(PREVIEW_PAGE_HEIGHT);
let previewObserver = null;
let previewStageObserver = null;
let workbenchObserver = null;

// 工作台宽度受壳层侧边栏影响，用实测宽度判断是否收起预览
function updateNarrowState() {
  const workbench = workbenchRef.value;
  if (!workbench) return;
  const narrow = workbench.clientWidth < NARROW_BREAKPOINT;
  if (narrow === isNarrow.value) return;
  isNarrow.value = narrow;
  narrowPreviewOpen.value = false;
}

function updatePreviewScale() {
  const viewport = previewViewportRef.value;
  if (!viewport) return;
  const available = viewport.clientWidth - 32;
  if (available <= 0) return;
  const scale = Math.min(1, Math.max(0.3, available / PREVIEW_PAGE_WIDTH));
  previewScale.value = Number(scale.toFixed(4));
}

// 预览面板可能一开始就是折叠的，等它显示出来再挂观察器
function observePreview() {
  if (typeof ResizeObserver === 'undefined') return;
  if (previewViewportRef.value && previewObserver) {
    previewObserver.observe(previewViewportRef.value);
  }
  if (previewStageRef.value && previewStageObserver) {
    previewStageObserver.observe(previewStageRef.value);
  }
  updatePreviewScale();
}

// ── Mount: load resume from API ───────────────────────────────────────────
onMounted(async () => {
  window.addEventListener('resize', onWindowResize);
  window.addEventListener('beforeunload', onBeforeUnload);

  if (typeof ResizeObserver !== 'undefined') {
    previewObserver = new ResizeObserver(() => updatePreviewScale());
    previewStageObserver = new ResizeObserver(() => {
      const stage = previewStageRef.value;
      if (stage) {
        previewContentHeight.value = Math.max(stage.offsetHeight, PREVIEW_PAGE_HEIGHT);
      }
    });
    workbenchObserver = new ResizeObserver(() => updateNarrowState());
    if (workbenchRef.value) {
      workbenchObserver.observe(workbenchRef.value);
    }
  }
  observePreview();
  updateNarrowState();

  if (resumeId) {
    try {
      await resumeStore.fetchResume(resumeId);
      // Apply template from query param (e.g. from template market)
      const queryTemplate = route.query.template;
      if (queryTemplate && ['classic', 'modern', 'creative', 'compact', 'academic', 'dual-creative'].includes(queryTemplate)) {
        store.selectedTemplate.value = queryTemplate;
      }
      // New resume (no sections yet): auto-fill all system modules
      if (sections.length === 0) {
        for (const block of DEFAULT_SECTIONS) {
          await resumeStore.addSectionToApi(resumeId, buildSectionCreatePayload(block));
        }
      }
    } catch (error) {
      // 顶部状态条会提示保存失败，这里给出可操作的加载失败说明
      loadError.value = extractApiMessage(error, '无法读取该简历');
    }
  }

  pageLoading.value = false;

  // 打开即进入第一个模块，避免首屏空白
  if (!activeId.value && sections.length) {
    activeId.value = sections[0].id;
  }

  updatePreviewScale();
});

onBeforeUnmount(() => {
  window.removeEventListener('resize', onWindowResize);
  window.removeEventListener('beforeunload', onBeforeUnload);
  if (saveTimeout) clearTimeout(saveTimeout);
  previewObserver?.disconnect();
  previewStageObserver?.disconnect();
  workbenchObserver?.disconnect();
});

function onBeforeUnload(event) {
  if (!hasUnsavedChanges.value) return;
  event.preventDefault();
  event.returnValue = '';
}

// 离开页面前提示未保存内容
onBeforeRouteLeave(() => {
  if (!hasUnsavedChanges.value) return true;
  return window.confirm('简历还有未保存的修改，确定要离开吗？');
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
// 左栏模块列表折叠状态
const navListExpanded = ref(true);
// 中栏当前模块展开（true=编辑表单，false=摘要）
const sectionExpanded = ref(true);
// 首屏加载 / 加载失败状态
const pageLoading = ref(true);
const loadError = ref('');

// 切换模块时默认展开该模块的编辑表单
watch(activeId, () => {
  sectionExpanded.value = true;
});

const templates = [
  { value: 'classic', label: '标准版' },
  { value: 'modern', label: '简约求职版' },
  { value: 'compact', label: '双列紧凑版' },
  { value: 'academic', label: '学术科研版' },
  { value: 'creative', label: '创意进阶版' },
  { value: 'dual-creative', label: '双列创意版' },
];

const bodyFontOptions = [9, 10, 11, 12];
// 左栏配色色板：全部引用设计 token，避免写死十六进制颜色
const sidebarColorOptions = [
  { value: 'black', label: '黑', swatch: 'linear-gradient(180deg, var(--ui-neutral-900) 0%, var(--ui-neutral-800) 100%)' },
  { value: 'blue', label: '蓝', swatch: 'linear-gradient(180deg, var(--ui-info) 0%, var(--ui-neutral-950) 100%)' },
  { value: 'white', label: '白', swatch: 'linear-gradient(180deg, var(--ui-neutral-0) 0%, var(--ui-neutral-100) 100%)' },
  { value: 'gray', label: '灰', swatch: 'linear-gradient(180deg, var(--ui-neutral-200) 0%, var(--ui-neutral-50) 100%)' },
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

const resumeName = computed(() => {
  const basicSection = store.sections.find((s) => s.sectionCode === 'BASIC');
  return String(basicSection?.contentJson?.name || '').trim();
});

const supportsSidebarColor = computed(() =>
  ['modern', 'compact', 'creative', 'dual-creative'].includes(store.selectedTemplate.value)
);

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

function onRemoveSection(id, title) {
  if (!window.confirm(`确定删除模块「${title}」吗？删除后该模块内容不再出现在简历中。`)) {
    return;
  }
  store.removeSection(id);
  if (activeId.value === id) {
    activeId.value = null;
  }
  scheduleAutoSave();
}

// AI 助手面板由 FloatingAgentCopilot 自行管理开合，这里只负责唤起入口
function openAgentCopilot() {
  const launcher = document.querySelector('.copilot-launcher');
  if (launcher instanceof HTMLElement) {
    launcher.click();
    launcher.focus();
    return;
  }
  window.alert('AI 助手面板未就绪，请稍后再试');
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
  // 导出按 A4 原尺寸渲染，先把预览缩放恢复为 100%
  const restoredScale = previewScale.value;
  previewScale.value = 1;
  await nextTick();
  try {
    await exportToPDF(exportElement, buildExportFilename());
  } catch (error) {
    console.error('导出 PDF 失败', error);
    window.alert('导出 PDF 失败，请稍后重试');
  } finally {
    previewScale.value = restoredScale;
    exportingPdf.value = false;
  }
}

// ── 窗口尺寸变化 ──────────────────────────────────────────────────────────
function onWindowResize() {
  updateNarrowState();
  updatePreviewScale();
}

// ── 模块拖拽排序 ──────────────────────────────────────────────────────────
const draggedId = ref(null);
const dragOverId = ref(null);

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

// ── 面板拖拽调宽 ──────────────────────────────────────────────────────────
const isDragging = ref(false);
const resizeTarget = ref(null);
let resizeOriginX = 0;
let resizeStartWidth = 0;

function startResize(target, event) {
  isDragging.value = true;
  resizeTarget.value = target;
  resizeOriginX = event.clientX;
  resizeStartWidth = target === 'nav' ? navWidth.value : previewWidth.value;
  document.addEventListener('mousemove', onResizeMove);
  document.addEventListener('mouseup', onResizeEnd);
  event.preventDefault();
}

function onResizeMove(event) {
  if (!isDragging.value) return;
  const delta = event.clientX - resizeOriginX;

  if (resizeTarget.value === 'nav') {
    navWidth.value = Math.min(NAV_MAX, Math.max(NAV_MIN, resizeStartWidth + delta));
    return;
  }

  // 预览默认在右侧，向左拖动变宽；交换位置后方向相反
  const deltaWidth = layout.value === 'edit-left' ? -delta : delta;
  const workbenchWidth = workbenchRef.value?.clientWidth ?? window.innerWidth;
  const maxWidth = Math.max(PREVIEW_MIN, Math.min(PREVIEW_MAX, workbenchWidth - navWidth.value - CANVAS_MIN));
  previewWidth.value = Math.min(maxWidth, Math.max(PREVIEW_MIN, resizeStartWidth + deltaWidth));
}

function onResizeEnd() {
  isDragging.value = false;
  resizeTarget.value = null;
  document.removeEventListener('mousemove', onResizeMove);
  document.removeEventListener('mouseup', onResizeEnd);
  persistStoredNumber(NAV_WIDTH_KEY, navWidth.value);
  persistStoredNumber(PREVIEW_WIDTH_KEY, previewWidth.value);
}
</script>
