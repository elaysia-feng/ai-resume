<template>
  <section class="version-panel">
    <div class="ui-section-head">
      <div>
        <h3 class="ui-section-head__title">版本历史</h3>
        <p class="ui-section-head__hint">保存、对比和恢复简历的历史版本</p>
      </div>
      <button type="button" class="ui-btn ui-btn--sm ui-btn--outline" :disabled="loading" @click="loadVersions">
        <AppIcon name="arrows-clockwise" :size="14" />
        刷新
      </button>
    </div>

    <form class="save-form" @submit.prevent="handleSaveVersion">
      <input
        v-model="saveForm.versionName"
        class="ui-input"
        type="text"
        placeholder="版本名称，可为空"
        aria-label="版本名称"
      />
      <select v-model="saveForm.source" class="ui-input ui-select" aria-label="版本来源">
        <option value="MANUAL">手动保存</option>
        <option value="AUTO">自动保存</option>
        <option value="AI">AI 生成</option>
      </select>
      <button type="button" class="ui-btn ui-btn--sm ui-btn--primary" :disabled="saving" @click="handleSaveVersion">
        <AppIcon name="note-pencil" :size="14" />
        {{ saving ? '保存中…' : '保存版本' }}
      </button>
    </form>

    <div v-if="message" class="ui-callout ui-callout--accent" role="status">
      <AppIcon name="check-circle" :size="16" />
      <span>{{ message }}</span>
    </div>
    <div v-if="error" class="ui-callout ui-callout--danger" role="alert">
      <AppIcon name="warning-circle" :size="16" />
      <span>{{ error }}</span>
    </div>

    <div class="version-body">
      <div class="ui-panel version-list">
        <div v-if="loading" aria-hidden="true">
          <div class="version-skeleton">
            <div v-for="index in 3" :key="index" class="ui-skeleton version-skeleton__row"></div>
          </div>
        </div>

        <div v-else-if="versions.length === 0" class="ui-empty version-empty">
          <span class="ui-empty__icon" aria-hidden="true">
            <AppIcon name="clock-counter-clockwise" :size="24" />
          </span>
          <p class="ui-empty__title">暂无版本记录</p>
          <p class="ui-empty__hint">保存一个版本后，这里会列出时间、来源与变更摘要。</p>
        </div>

        <ul v-else class="version-list__items">
          <li v-for="version in versions" :key="version.id">
            <button
              type="button"
              class="version-item"
              :class="{ 'is-active': version.id === selectedVersionId }"
              @click="loadVersionDetail(version.id)"
            >
              <span class="version-item__top">
                <span class="version-name">{{ version.versionName || `v${version.versionNo}` }}</span>
                <span class="ui-badge" :class="sourceBadgeClass(version.source)">{{ sourceLabel(version.source) }}</span>
              </span>
              <span class="version-item__bottom">
                <span class="version-no">v{{ version.versionNo }}</span>
                <span class="version-time">{{ formatDateTime(version.createdAt) }}</span>
              </span>
              <span v-if="compareIds.includes(version.id)" class="version-item__compare">
                对比基准 {{ compareIds.indexOf(version.id) === 0 ? 'A' : 'B' }}
              </span>
            </button>
          </li>
        </ul>
      </div>

      <div class="ui-panel version-detail">
        <div v-if="detailLoading" class="ui-empty version-empty">
          <span class="ui-empty__icon" aria-hidden="true">
            <AppIcon name="spinner-gap" :size="24" spin />
          </span>
          <p class="ui-empty__title">正在加载版本详情…</p>
        </div>

        <div v-else-if="!versionDetail" class="ui-empty version-empty">
          <span class="ui-empty__icon" aria-hidden="true">
            <AppIcon name="file-doc" :size="24" />
          </span>
          <p class="ui-empty__title">选择一个版本查看详情</p>
          <p class="ui-empty__hint">左侧列表中选择任意版本，可查看模块结构并对比或恢复。</p>
        </div>

        <template v-else>
          <div class="detail-card">
            <div class="detail-row"><span>版本名</span><strong>{{ versionDetail.versionName || '未命名' }}</strong></div>
            <div class="detail-row"><span>版本号</span><strong>v{{ versionDetail.versionNo }}</strong></div>
            <div class="detail-row"><span>来源</span><strong>{{ sourceLabel(versionDetail.source) }}</strong></div>
            <div class="detail-row"><span>模板</span><strong>{{ versionDetail.resumeTemplate || '未指定' }}</strong></div>
            <div class="detail-row"><span>模块数</span><strong>{{ versionDetail.sections?.length || 0 }}</strong></div>
            <div class="detail-row"><span>创建时间</span><strong>{{ formatDateTime(versionDetail.createdAt) }}</strong></div>
          </div>

          <div class="section-list">
            <div v-for="section in versionDetail.sections || []" :key="section.id" class="section-item">
              <div class="section-title">{{ section.sectionTitle || section.sectionCode }}</div>
              <div class="section-meta">
                <span>{{ section.sectionCode }}</span>
                <span>{{ section.schemaType }}</span>
              </div>
            </div>
          </div>

          <div class="detail-actions">
            <button
              type="button"
              class="ui-btn ui-btn--sm ui-btn--outline"
              :disabled="compareIds.includes(selectedVersionId) || compareIds.length >= 2"
              @click="toggleCompare(selectedVersionId)"
            >
              <AppIcon name="swap" :size="14" />
              加入对比
            </button>
            <button
              type="button"
              class="ui-btn ui-btn--sm ui-btn--danger"
              :disabled="restoring"
              @click="handleRestore"
            >
              <AppIcon name="clock-counter-clockwise" :size="14" />
              {{ restoring ? '恢复中…' : '恢复到此版本' }}
            </button>
          </div>
        </template>
      </div>
    </div>

    <section v-if="compareDetailList.length" class="ui-panel compare-panel">
      <div class="ui-section-head">
        <h4 class="ui-section-head__title">版本对比</h4>
        <button type="button" class="ui-btn ui-btn--sm ui-btn--ghost" @click="clearCompare">清空对比</button>
      </div>
      <ul class="compare-list">
        <li v-for="row in compareRows" :key="row.code" class="compare-row" :class="`is-${row.status}`">
          <span class="compare-row__name">{{ row.title }}</span>
          <span class="compare-row__status">{{ row.statusLabel }}</span>
          <span class="compare-row__delta">{{ row.delta }}</span>
        </li>
      </ul>
    </section>
  </section>
</template>

<script setup>
import { computed, ref, watch } from 'vue';
import AppIcon from './common/AppIcon.vue';
import {
  getResumeVersionDetail,
  getResumeVersions,
  restoreResumeVersion,
  saveResumeVersion,
  extractApiMessage,
} from '../api/index.js';
import { formatDateTime } from '../utils/resume.js';

const props = defineProps({
  resumeId: {
    type: [Number, String],
    default: null,
  },
});

const emit = defineEmits(['restored']);

const loading = ref(false);
const detailLoading = ref(false);
const saving = ref(false);
const restoring = ref(false);
const error = ref('');
const message = ref('');
const versions = ref([]);
const selectedVersionId = ref(null);
const versionDetail = ref(null);
const compareIds = ref([]);
const compareDetails = ref([]);
const saveForm = ref({
  versionName: '',
  source: 'MANUAL',
});

watch(
  () => props.resumeId,
  async (value) => {
    resetState();
    if (value) {
      await loadVersions();
    }
  },
  { immediate: true }
);

function resetState() {
  versions.value = [];
  versionDetail.value = null;
  selectedVersionId.value = null;
  compareIds.value = [];
  compareDetails.value = [];
  error.value = '';
  message.value = '';
}

async function loadVersions() {
  if (!props.resumeId) {
    return;
  }
  loading.value = true;
  error.value = '';
  try {
    versions.value = await getResumeVersions(props.resumeId);
  } catch (err) {
    error.value = extractApiMessage(err, '加载版本列表失败');
  } finally {
    loading.value = false;
  }
}

async function loadVersionDetail(versionId) {
  if (!props.resumeId || !versionId) {
    return;
  }
  selectedVersionId.value = versionId;
  detailLoading.value = true;
  error.value = '';
  try {
    versionDetail.value = await getResumeVersionDetail(props.resumeId, versionId);
  } catch (err) {
    error.value = extractApiMessage(err, '加载版本详情失败');
  } finally {
    detailLoading.value = false;
  }
}

async function handleSaveVersion() {
  if (!props.resumeId) {
    return;
  }
  saving.value = true;
  error.value = '';
  message.value = '';
  try {
    await saveResumeVersion(props.resumeId, {
      versionName: saveForm.value.versionName || null,
      source: saveForm.value.source,
    });
    message.value = '版本保存成功';
    saveForm.value.versionName = '';
    await loadVersions();
  } catch (err) {
    error.value = extractApiMessage(err, '保存版本失败');
  } finally {
    saving.value = false;
  }
}

async function handleRestore() {
  if (!props.resumeId || !selectedVersionId.value) {
    return;
  }
  const versionLabel = versionDetail.value?.versionName || `v${versionDetail.value?.versionNo || selectedVersionId.value}`;
  if (!window.confirm(`恢复「${versionLabel}」会覆盖当前简历内容，且不可撤销。确定继续吗？`)) {
    return;
  }
  restoring.value = true;
  error.value = '';
  message.value = '';
  try {
    const restored = await restoreResumeVersion(props.resumeId, selectedVersionId.value);
    message.value = '版本恢复成功';
    emit('restored', restored);
    await loadVersions();
    await loadVersionDetail(selectedVersionId.value);
  } catch (err) {
    error.value = extractApiMessage(err, '恢复版本失败');
  } finally {
    restoring.value = false;
  }
}

/* ---------- 版本对比 ---------- */
async function toggleCompare(versionId) {
  if (!versionId) {
    return;
  }
  if (compareIds.value.includes(versionId)) {
    clearCompare();
    return;
  }
  if (compareIds.value.length >= 2) {
    compareIds.value = [compareIds.value[1], versionId];
  } else {
    compareIds.value = [...compareIds.value, versionId];
  }
  await loadCompareDetails();
}

function clearCompare() {
  compareIds.value = [];
  compareDetails.value = [];
}

async function loadCompareDetails() {
  if (!props.resumeId || compareIds.value.length === 0) {
    compareDetails.value = [];
    return;
  }
  try {
    const details = await Promise.all(
      compareIds.value.map((id) => getResumeVersionDetail(props.resumeId, id))
    );
    compareDetails.value = details;
  } catch (err) {
    error.value = extractApiMessage(err, '加载对比版本失败');
  }
}

const compareDetailList = computed(() => compareDetails.value.filter(Boolean));

function sourceLabel(source) {
  const map = { MANUAL: '手动保存', AUTO: '自动保存', AI: 'AI 生成' };
  return map[source] || source || '未知来源';
}

function sourceBadgeClass(source) {
  if (source === 'AI') return 'ui-badge--accent';
  if (source === 'AUTO') return 'ui-badge--warning';
  return '';
}

function sectionSize(section) {
  const content = section?.contentJson ?? section?.content ?? '';
  if (typeof content === 'string') {
    return content.replace(/<[^>]*>/g, '').trim().length;
  }
  if (content && typeof content === 'object') {
    return JSON.stringify(content).length;
  }
  return 0;
}

const compareRows = computed(() => {
  const [left, right] = compareDetailList.value;
  if (!left || !right) {
    return [];
  }

  const leftMap = new Map((left.sections || []).map((item) => [item.sectionCode, item]));
  const rightMap = new Map((right.sections || []).map((item) => [item.sectionCode, item]));
  const codes = Array.from(new Set([...leftMap.keys(), ...rightMap.keys()]));

  return codes.map((code) => {
    const leftSection = leftMap.get(code);
    const rightSection = rightMap.get(code);
    const leftSize = leftSection ? sectionSize(leftSection) : 0;
    const rightSize = rightSection ? sectionSize(rightSection) : 0;

    let status = 'same';
    let statusLabel = '内容一致';
    if (leftSection && !rightSection) {
      status = 'only-left';
      statusLabel = '仅 A 有';
    } else if (!leftSection && rightSection) {
      status = 'only-right';
      statusLabel = '仅 B 有';
    } else if (leftSize !== rightSize) {
      status = 'changed';
      statusLabel = '内容有变化';
    }

    const delta = leftSize === rightSize ? '字数相同' : `A ${leftSize} 字 / B ${rightSize} 字`;

    return {
      code,
      title: (rightSection || leftSection)?.sectionTitle || code,
      status,
      statusLabel,
      delta,
    };
  });
});
</script>

<style scoped>
.version-panel {
  display: flex;
  flex-direction: column;
  gap: var(--ui-space-4);
  padding: var(--ui-space-5);
  color: var(--ui-fg-primary);
}

/* ---------- 保存表单 ---------- */
.save-form {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 150px 120px;
  gap: var(--ui-space-2);
}

.save-form :deep(select) {
  font-family: inherit;
}

/* ---------- 主体 ---------- */
.version-body {
  display: grid;
  grid-template-columns: 260px minmax(0, 1fr);
  gap: var(--ui-space-4);
  min-height: 300px;
}

.version-list,
.version-detail {
  padding: var(--ui-space-3);
}

.version-list {
  overflow-y: auto;
}

.version-list__items {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: var(--ui-space-2);
}

.version-item {
  display: flex;
  flex-direction: column;
  gap: var(--ui-space-1);
  width: 100%;
  padding: var(--ui-space-3);
  border: 1px solid var(--ui-border);
  border-radius: var(--ui-radius-md);
  background: var(--ui-bg-surface);
  text-align: left;
  cursor: pointer;
  transition: border-color var(--ui-duration-fast) var(--ui-ease-out),
    background var(--ui-duration-fast) var(--ui-ease-out);
}

.version-item:hover {
  border-color: var(--ui-border-strong);
  background: var(--ui-bg-hover);
}

.version-item:focus-visible {
  outline: none;
  box-shadow: var(--ui-ring);
}

.version-item.is-active {
  border-color: var(--ui-accent-border);
  background: var(--ui-accent-soft);
}

.version-item__top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--ui-space-2);
}

.version-name {
  font-size: var(--ui-text-sm);
  font-weight: 600;
  color: var(--ui-fg-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.version-item__bottom {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--ui-space-2);
  font-size: var(--ui-text-xs);
  color: var(--ui-fg-muted);
  font-variant-numeric: tabular-nums;
}

.version-item__compare {
  font-size: var(--ui-text-xs);
  color: var(--ui-fg-accent);
}

/* ---------- 骨架 ---------- */
.version-skeleton {
  display: flex;
  flex-direction: column;
  gap: var(--ui-space-2);
}

.version-skeleton__row {
  height: 56px;
  border-radius: var(--ui-radius-md);
}

/* ---------- 详情 ---------- */
.version-detail {
  display: flex;
  flex-direction: column;
  gap: var(--ui-space-3);
}

.detail-card {
  display: flex;
  flex-direction: column;
  gap: var(--ui-space-2);
  padding: var(--ui-space-3);
  border: 1px solid var(--ui-border);
  border-radius: var(--ui-radius-md);
}

.detail-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--ui-space-3);
  font-size: var(--ui-text-sm);
}

.detail-row span {
  color: var(--ui-fg-secondary);
}

.detail-row strong {
  color: var(--ui-fg-primary);
  font-weight: 600;
  text-align: right;
}

.section-list {
  display: flex;
  flex-direction: column;
  gap: var(--ui-space-2);
  max-height: 260px;
  overflow-y: auto;
}

.section-item {
  padding: var(--ui-space-2) var(--ui-space-3);
  border: 1px solid var(--ui-border);
  border-radius: var(--ui-radius-md);
  background: var(--ui-bg-sunken);
}

.section-title {
  font-size: var(--ui-text-sm);
  font-weight: 600;
  color: var(--ui-fg-primary);
}

.section-meta {
  display: flex;
  align-items: center;
  gap: var(--ui-space-2);
  margin-top: 2px;
  font-size: var(--ui-text-xs);
  color: var(--ui-fg-muted);
}

.detail-actions {
  display: flex;
  align-items: center;
  gap: var(--ui-space-2);
}

/* ---------- 对比 ---------- */
.compare-panel {
  display: flex;
  flex-direction: column;
  gap: var(--ui-space-2);
  padding: var(--ui-space-3);
}

.compare-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: var(--ui-space-1);
}

.compare-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 96px 160px;
  gap: var(--ui-space-2);
  align-items: center;
  padding: var(--ui-space-2) var(--ui-space-3);
  border: 1px solid var(--ui-border);
  border-radius: var(--ui-radius-sm);
  font-size: var(--ui-text-xs);
}

.compare-row__name {
  color: var(--ui-fg-primary);
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.compare-row__status {
  color: var(--ui-fg-secondary);
}

.compare-row__delta {
  color: var(--ui-fg-muted);
  text-align: right;
  font-variant-numeric: tabular-nums;
}

.compare-row.is-changed {
  border-color: var(--ui-warning-border);
  background: var(--ui-warning-soft);
}

.compare-row.is-only-left,
.compare-row.is-only-right {
  border-color: var(--ui-info-border);
  background: var(--ui-info-soft);
}

/* ---------- 空状态 ---------- */
.version-empty {
  padding: var(--ui-space-8) var(--ui-space-4);
}

.version-empty :deep(.ui-empty__title) {
  font-size: var(--ui-text-sm);
}

.version-empty :deep(.ui-empty__hint) {
  font-size: var(--ui-text-xs);
}

@media (max-width: 900px) {
  .save-form,
  .version-body {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
