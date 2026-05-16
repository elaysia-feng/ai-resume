import { defineStore } from 'pinia';
import { reactive, computed, ref } from 'vue';
import {
  getResumes as apiGetResumes,
  getResume as apiGetResume,
  createResume as apiCreateResume,
  updateResume as apiUpdateResume,
  deleteResume as apiDeleteResume,
  addSection as apiAddSection,
  updateSection as apiUpdateSection,
  deleteSection as apiDeleteSection,
  reorderSections as apiReorderSections,
} from '../api/index.js';
import { parseJsonContent } from '../utils/resume.js';

// Keep resumeId as a module-level constant (matches editor expectations)
// Updated by fetchResume / createResume
export let resumeId = null;
const BODY_FONT_SIZE_STORAGE_KEY = 'resume_body_font_size';
const SIDEBAR_COLOR_STORAGE_KEY = 'resume_sidebar_color';
const ALLOWED_BODY_FONT_SIZES = [9, 10, 11, 12];
const ALLOWED_SIDEBAR_COLORS = ['black', 'blue', 'white', 'gray'];

function readResumeBodyFontSize() {
  if (typeof window === 'undefined') {
    return 9;
  }

  const rawValue = Number(window.localStorage.getItem(BODY_FONT_SIZE_STORAGE_KEY));
  return ALLOWED_BODY_FONT_SIZES.includes(rawValue) ? rawValue : 9;
}

function persistResumeBodyFontSize(value) {
  if (typeof window === 'undefined') {
    return;
  }

  window.localStorage.setItem(BODY_FONT_SIZE_STORAGE_KEY, String(value));
}

function readSidebarColor() {
  if (typeof window === 'undefined') {
    return 'gray';
  }

  const rawValue = window.localStorage.getItem(SIDEBAR_COLOR_STORAGE_KEY);
  return ALLOWED_SIDEBAR_COLORS.includes(rawValue) ? rawValue : 'gray';
}

function persistSidebarColor(value) {
  if (typeof window === 'undefined') {
    return;
  }

  window.localStorage.setItem(SIDEBAR_COLOR_STORAGE_KEY, value);
}

// ── Pinia Store ────────────────────────────────────────────────────────────
export const useResumeStore = defineStore('resume', () => {
  const resumes = ref([]);
  const loading = ref(false);
  const error = ref(null);

  function applyResumeDetail(data) {
    resumeId = data?.id ?? null;
    sections.splice(0, sections.length);

    if (data?.template) {
      selectedTemplate.value = data.template;
    }

    if (data?.sections && Array.isArray(data.sections)) {
      data.sections.forEach((section) => {
        sections.push({
          ...section,
          contentJson: parseJsonContent(section.contentJson, {}),
        });
      });
    }
  }

  async function fetchResumes() {
    loading.value = true;
    error.value = null;
    try {
      resumes.value = await apiGetResumes();
    } catch (err) {
      error.value = err?.response?.data?.message || err.message || 'Failed to load resumes';
    } finally {
      loading.value = false;
    }
  }

  async function fetchResume(id) {
    loading.value = true;
    error.value = null;
    try {
      const data = await apiGetResume(id);
      applyResumeDetail(data);
      return data;
    } catch (err) {
      error.value = err?.response?.data?.message || err.message || 'Failed to load resume';
      throw err;
    } finally {
      loading.value = false;
    }
  }

  async function createResume(data = {}) {
    loading.value = true;
    error.value = null;
    try {
      const previousIds = new Set(resumes.value.map((item) => item.id));
      await apiCreateResume(data);
      const latestResumes = await apiGetResumes();
      resumes.value = latestResumes;

      const created =
        latestResumes.find((item) => !previousIds.has(item.id))
        || [...latestResumes].sort((a, b) => new Date(b.updatedAt || 0) - new Date(a.updatedAt || 0))[0]
        || null;
      return created;
    } catch (err) {
      error.value = err?.response?.data?.message || err.message || 'Failed to create resume';
      throw err;
    } finally {
      loading.value = false;
    }
  }

  async function saveResume(id, data) {
    loading.value = true;
    error.value = null;
    try {
      await apiUpdateResume(id, data);
      const updated = await apiGetResume(id);
      applyResumeDetail(updated);
      const latestResumes = await apiGetResumes();
      resumes.value = latestResumes;
      return updated;
    } catch (err) {
      error.value = err?.response?.data?.message || err.message || 'Failed to save resume';
      throw err;
    } finally {
      loading.value = false;
    }
  }

  async function removeResume(id) {
    loading.value = true;
    error.value = null;
    try {
      await apiDeleteResume(id);
      resumes.value = resumes.value.filter((r) => r.id !== id);
    } catch (err) {
      error.value = err?.response?.data?.message || err.message || 'Failed to delete resume';
      throw err;
    } finally {
      loading.value = false;
    }
  }

  async function addSectionToApi(resumeId, data) {
    await apiAddSection(resumeId, data);
    const detail = await apiGetResume(resumeId);
    applyResumeDetail(detail);
    return detail;
  }

  async function updateSectionOnApi(resumeId, sectionId, data) {
    await apiUpdateSection(resumeId, sectionId, data);
    const detail = await apiGetResume(resumeId);
    applyResumeDetail(detail);
    return detail;
  }

  async function deleteSectionFromApi(resumeId, sectionId) {
    await apiDeleteSection(resumeId, sectionId);
    const detail = await apiGetResume(resumeId);
    applyResumeDetail(detail);
  }

  async function reorderSectionsOnApi(currentResumeId, sectionIds) {
    await apiReorderSections(currentResumeId, sectionIds);
    const detail = await apiGetResume(currentResumeId);
    applyResumeDetail(detail);
  }

  return {
    resumes,
    loading,
    error,
    applyResumeDetail,
    fetchResumes,
    fetchResume,
    createResume,
    saveResume,
    removeResume,
    addSectionToApi,
    updateSectionOnApi,
    deleteSectionFromApi,
    reorderSectionsOnApi,
  };
});

// ── Module-level reactive state (backward-compatible with editors) ────────────
export const sections = reactive([]);

export const selectedTemplate = reactive({ value: 'classic' });

export const sidebarColor = reactive({ value: readSidebarColor() });

export const resumeBodyFontSize = reactive({ value: readResumeBodyFontSize() });

export const visibleSections = computed(() =>
  [...sections].filter((s) => s.visible !== false).sort((a, b) => (a.sortOrder || 0) - (b.sortOrder || 0))
);

function genId() {
  return 's-' + Math.random().toString(36).substr(2, 9);
}

export function getSection(id) {
  return sections.find((s) => s.id === id) || null;
}

export function getSectionContent(id) {
  const sec = getSection(id);
  return sec ? sec.contentJson : null;
}

export function setSectionContent(id, content) {
  const sec = getSection(id);
  if (sec) {
    sec.contentJson = { ...sec.contentJson, ...content };
    sec.updatedAt = new Date().toISOString();
  }
}

export function toggleVisible(id) {
  const sec = getSection(id);
  if (sec) sec.visible = !sec.visible;
}

export function removeSection(id) {
  const idx = sections.findIndex((s) => s.id === id);
  if (idx !== -1) {
    sections.splice(idx, 1);
    sections.forEach((s, i) => { s.sortOrder = i; });
  }
}

export function addSection(sectionData) {
  const newSection = {
    id: genId(),
    resumeId: resumeId || 'local',
    sectionType: sectionData.sectionType || 'CUSTOM',
    sectionCode: sectionData.sectionCode || 'CUSTOM',
    sectionTitle: sectionData.sectionTitle || '自定义模块',
    contentJson: sectionData.contentJson || {},
    visible: true,
    sortOrder: sections.length,
    schemaType: sectionData.schemaType || 'TEXT',
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString(),
  };
  sections.push(newSection);
  return newSection;
}

export function reorderSections(fromId, toId) {
  const fromIdx = sections.findIndex((s) => s.id === fromId);
  const toIdx = sections.findIndex((s) => s.id === toId);
  if (fromIdx === -1 || toIdx === -1) return;
  const [moved] = sections.splice(fromIdx, 1);
  sections.splice(toIdx, 0, moved);
  sections.forEach((s, i) => { s.sortOrder = i; });
}

export function setResumeBodyFontSize(value) {
  const normalizedValue = Number(value);
  if (!ALLOWED_BODY_FONT_SIZES.includes(normalizedValue)) {
    return;
  }

  resumeBodyFontSize.value = normalizedValue;
  persistResumeBodyFontSize(normalizedValue);
}

export function setSidebarColor(value) {
  if (!ALLOWED_SIDEBAR_COLORS.includes(value)) {
    return;
  }

  sidebarColor.value = value;
  persistSidebarColor(value);
}

// Backward-compatible computed (used by ResumeEditorView.vue)
export const resumeData = computed(() => {
  const basic = sections.find((s) => s.sectionCode === 'BASIC')?.contentJson || {};
  const ji = sections.find((s) => s.sectionCode === 'JOB_INTENT')?.contentJson || {};
  const summarySec = sections.find((s) => s.sectionCode === 'SUMMARY')?.contentJson || {};
  const eduSec = sections.find((s) => s.sectionCode === 'EDUCATION')?.contentJson || {};
  const expSec = sections.find((s) => s.sectionCode === 'EXPERIENCE')?.contentJson || {};
  const skillsSec = sections.find((s) => s.sectionCode === 'SKILLS')?.contentJson || {};
  const selfEvalSec = sections.find((s) => s.sectionCode === 'SELF_EVALUATION')?.contentJson || {};
  const projectsSec = sections.find((s) => s.sectionCode === 'PROJECTS')?.contentJson || {};
  const campusSec = sections.find((s) => s.sectionCode === 'CAMPUS')?.contentJson || {};
  const certSec = sections.find((s) => s.sectionCode === 'CERTIFICATES')?.contentJson || {};
  const internSec = sections.find((s) => s.sectionCode === 'INTERNSHIP')?.contentJson || {};
  return {
    name: basic.name || '',
    title: basic.title || '',
    email: basic.email || '',
    phone: basic.phone || '',
    location: basic.location || '',
    wechat: basic.wechat || '',
    github: basic.github || '',
    website: basic.website || '',
    desiredPosition: ji.desiredPosition || '',
    desiredCity: ji.desiredCity || '',
    salaryRange: ji.salaryRange || '',
    employmentType: ji.employmentType || '',
    jobStatus: ji.jobStatus || '',
    summary: summarySec.text || '',
    education: eduSec.items || [],
    experience: expSec.items || [],
    skills: skillsSec.items || [],
    selfEvaluation: selfEvalSec.text || '',
    projects: projectsSec.items || [],
    campus: campusSec.items || [],
    certificates: certSec.items || [],
    internship: internSec.items || [],
    blockOrder: sections.map((s) => s.id),
  };
});
