<template>
  <section class="version-panel">
    <header class="panel-header">
      <div>
        <h3 class="panel-title">版本管理</h3>
        <p class="panel-subtitle">保存、查看和恢复简历历史版本</p>
      </div>
      <button class="refresh-btn" :disabled="loading" @click="loadVersions">
        刷新
      </button>
    </header>

    <form class="save-form" @submit.prevent="handleSaveVersion">
      <input
        v-model="saveForm.versionName"
        class="text-input"
        type="text"
        placeholder="版本名称，可为空"
      />
      <select v-model="saveForm.source" class="select-input">
        <option value="MANUAL">MANUAL</option>
        <option value="AUTO">AUTO</option>
        <option value="AI">AI</option>
      </select>
      <button class="primary-btn" type="submit" :disabled="saving">
        {{ saving ? '保存中...' : '保存版本' }}
      </button>
    </form>

    <p v-if="message" class="message success">{{ message }}</p>
    <p v-if="error" class="message error">{{ error }}</p>

    <div class="version-body">
      <div class="version-list">
        <div v-if="loading" class="empty-block">加载中...</div>
        <div v-else-if="versions.length === 0" class="empty-block">暂无版本记录</div>
        <button
          v-for="version in versions"
          :key="version.id"
          class="version-item"
          :class="{ active: version.id === selectedVersionId }"
          @click="loadVersionDetail(version.id)"
        >
          <div class="version-item-top">
            <span class="version-name">{{ version.versionName || `v${version.versionNo}` }}</span>
            <span class="version-source">{{ version.source }}</span>
          </div>
          <div class="version-item-bottom">
            <span>v{{ version.versionNo }}</span>
            <span>{{ formatDateTime(version.createdAt) }}</span>
          </div>
        </button>
      </div>

      <div class="version-detail">
        <div v-if="detailLoading" class="empty-block">加载详情中...</div>
        <div v-else-if="!versionDetail" class="empty-block">选择一个版本查看详情</div>
        <template v-else>
          <div class="detail-card">
            <div class="detail-row"><span>版本名</span><strong>{{ versionDetail.versionName }}</strong></div>
            <div class="detail-row"><span>版本号</span><strong>v{{ versionDetail.versionNo }}</strong></div>
            <div class="detail-row"><span>来源</span><strong>{{ versionDetail.source }}</strong></div>
            <div class="detail-row"><span>模板</span><strong>{{ versionDetail.resumeTemplate }}</strong></div>
            <div class="detail-row"><span>模块数</span><strong>{{ versionDetail.sections?.length || 0 }}</strong></div>
            <div class="detail-row"><span>创建时间</span><strong>{{ formatDateTime(versionDetail.createdAt) }}</strong></div>
          </div>

          <div class="section-list">
            <div
              v-for="section in versionDetail.sections"
              :key="section.id"
              class="section-item"
            >
              <div class="section-title">{{ section.sectionTitle }}</div>
              <div class="section-meta">
                <span>{{ section.sectionCode }}</span>
                <span>{{ section.schemaType }}</span>
              </div>
            </div>
          </div>

          <button class="danger-btn" :disabled="restoring" @click="handleRestore">
            {{ restoring ? '恢复中...' : '恢复到此版本' }}
          </button>
        </template>
      </div>
    </div>
  </section>
</template>

<script setup>
import { ref, watch } from 'vue';
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
  if (!window.confirm('恢复版本会覆盖当前简历内容，确定继续吗？')) {
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
</script>

<style scoped>
.version-panel {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 20px;
}

.panel-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.panel-title {
  margin: 0;
  font-size: 18px;
  font-weight: 700;
  color: #166534;
}

.panel-subtitle {
  margin: 6px 0 0;
  font-size: 13px;
  color: #6b7280;
}

.refresh-btn,
.primary-btn,
.danger-btn {
  border: none;
  border-radius: 8px;
  padding: 10px 14px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
}

.refresh-btn {
  background: #f3f4f6;
  color: #374151;
}

.save-form {
  display: grid;
  grid-template-columns: 1fr 140px 120px;
  gap: 10px;
}

.text-input,
.select-input {
  width: 100%;
  border: 1px solid #d1d5db;
  border-radius: 8px;
  padding: 10px 12px;
  font-size: 14px;
  box-sizing: border-box;
}

.primary-btn {
  background: #22c55e;
  color: #fff;
}

.danger-btn {
  background: #ef4444;
  color: #fff;
}

.message {
  margin: 0;
  padding: 10px 12px;
  border-radius: 8px;
  font-size: 13px;
}

.message.success {
  background: #dcfce7;
  color: #166534;
}

.message.error {
  background: #fee2e2;
  color: #991b1b;
}

.version-body {
  display: grid;
  grid-template-columns: 240px 1fr;
  gap: 14px;
  min-height: 320px;
}

.version-list,
.version-detail {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  padding: 12px;
}

.version-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.version-item {
  width: 100%;
  border: 1px solid #e5e7eb;
  background: #fff;
  border-radius: 10px;
  padding: 12px;
  text-align: left;
  cursor: pointer;
}

.version-item.active {
  border-color: #22c55e;
  background: #f0fdf4;
}

.version-item-top,
.version-item-bottom,
.detail-row,
.section-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.version-item-top {
  margin-bottom: 8px;
}

.version-name {
  font-size: 14px;
  font-weight: 600;
  color: #111827;
}

.version-source,
.version-item-bottom,
.section-meta {
  font-size: 12px;
  color: #6b7280;
}

.version-detail {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.detail-card,
.section-item {
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  padding: 12px;
}

.detail-card {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.detail-row span {
  font-size: 13px;
  color: #6b7280;
}

.detail-row strong {
  font-size: 13px;
  color: #111827;
}

.section-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  max-height: 260px;
  overflow-y: auto;
}

.section-title {
  font-size: 14px;
  font-weight: 600;
  color: #111827;
  margin-bottom: 6px;
}

.empty-block {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 160px;
  color: #9ca3af;
  font-size: 13px;
}

@media (max-width: 900px) {
  .save-form,
  .version-body {
    grid-template-columns: 1fr;
  }
}
</style>
