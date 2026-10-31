<template>
  <div class="templates-view">
    <!-- 页头：标题 + 当前状态 -->
    <header class="page-head">
      <div class="page-head__text">
        <h1 class="ui-page-title">简历模板</h1>
        <p class="ui-page-subtitle">对比 {{ templates.length }} 套 A4 版式；选中后可查看完整预览，再应用到已有简历或新建简历。</p>
      </div>

      <div class="page-head__actions">
        <label class="tpl-resume-target">
          <span class="ui-label">应用到简历</span>
          <select v-model="selectedResumeId" class="ui-input ui-select" aria-label="选择要应用模板的简历">
            <option value="">新建一份简历</option>
            <option v-for="resume in resumeStore.resumes" :key="resume.id" :value="String(resume.id)">
              {{ resume.title || '未命名简历' }}
            </option>
          </select>
        </label>
        <span class="ui-badge">{{ visibleTemplates.length }} 套模板</span>
      </div>
    </header>

    <!-- 错误提示 -->
    <div v-if="errorMessage" class="ui-callout ui-callout--danger" role="alert">
      <AppIcon name="warning-circle" :size="16" />
      <span>{{ errorMessage }}</span>
    </div>

    <div class="templates-view__body">
      <!-- 左侧筛选栏 -->
      <aside class="tpl-filters ui-panel ui-scroll" aria-label="模板筛选">
        <section class="tpl-filters__group">
          <h2 class="tpl-filters__title">
            <AppIcon name="layout" :size="14" />
            模板分类
          </h2>
          <ul class="tpl-filters__list">
            <li v-for="item in categoryOptions" :key="item.value">
              <button
                type="button"
                class="tpl-filters__item"
                :class="{ 'is-active': activeCategory === item.value }"
                :aria-pressed="activeCategory === item.value"
                @click="activeCategory = item.value"
              >
                <span class="tpl-filters__label">{{ item.label }}</span>
                <span class="ui-badge tpl-filters__count">{{ item.count }}</span>
              </button>
            </li>
          </ul>
        </section>

        <section class="tpl-filters__group">
          <h2 class="tpl-filters__title">
            <AppIcon name="target" :size="14" />
            求职阶段
          </h2>
          <ul class="tpl-filters__list">
            <li v-for="item in stageOptions" :key="item.value">
              <button
                type="button"
                class="tpl-filters__item"
                :class="{ 'is-active': activeStage === item.value }"
                :aria-pressed="activeStage === item.value"
                @click="toggleStage(item.value)"
              >
                <span class="tpl-filters__label">{{ item.label }}</span>
                <span class="ui-badge tpl-filters__count">{{ item.count }}</span>
              </button>
            </li>
          </ul>
        </section>

        <section class="tpl-filters__group">
          <h2 class="tpl-filters__title">
            <AppIcon name="briefcase" :size="14" />
            目标方向
          </h2>
          <ul class="tpl-filters__list">
            <li v-for="item in trackOptions" :key="item.value">
              <button
                type="button"
                class="tpl-filters__item"
                :class="{ 'is-active': activeTrack === item.value }"
                :aria-pressed="activeTrack === item.value"
                @click="toggleTrack(item.value)"
              >
                <span class="tpl-filters__label">{{ item.label }}</span>
                <span class="ui-badge tpl-filters__count">{{ item.count }}</span>
              </button>
            </li>
          </ul>
        </section>

        <section class="tpl-filters__group">
          <h2 class="tpl-filters__title">
            <AppIcon name="arrows-clockwise" :size="14" />
            排序
          </h2>
          <div class="ui-segmented tpl-filters__segmented" role="group" aria-label="排序方式">
            <button
              v-for="item in sortOptions"
              :key="item.value"
              type="button"
              class="ui-segmented__item"
              :class="{ 'is-active': activeSort === item.value }"
              :aria-pressed="activeSort === item.value"
              @click="activeSort = item.value"
            >
              {{ item.label }}
            </button>
          </div>
        </section>

        <button
          type="button"
          class="ui-btn ui-btn--sm ui-btn--outline ui-btn--block tpl-filters__reset"
          :disabled="!hasActiveFilter"
          @click="resetFilters"
        >
          <AppIcon name="x" :size="14" />
          清除筛选
        </button>
      </aside>

      <!-- 右侧模板卡片网格 -->
      <section class="tpl-results" aria-label="模板列表">
        <div class="ui-section-head tpl-results__head">
          <div>
            <h2 class="ui-section-head__title">
              {{ visibleTemplates.length }} 个模板<span v-if="hasActiveFilter">（已筛选）</span>
            </h2>
            <p class="ui-section-head__hint">点击卡片可先预览版式，再决定使用哪一套</p>
          </div>
        </div>

        <div class="tpl-results__content">
          <div class="tpl-gallery">
            <!-- 加载中：骨架卡片，不做整页遮罩 -->
            <div v-if="isBooting" class="tpl-grid" aria-busy="true">
              <div
                v-for="index in templates.length"
                :key="index"
                class="tpl-card ui-panel tpl-card--skeleton"
                aria-hidden="true"
              >
                <div class="tpl-card__preview">
                  <div class="ui-skeleton tpl-skeleton__thumb"></div>
                </div>
                <div class="tpl-card__body">
                  <span class="ui-skeleton tpl-skeleton__title"></span>
                  <span class="ui-skeleton tpl-skeleton__line"></span>
                  <span class="ui-skeleton tpl-skeleton__tag"></span>
                </div>
                <div class="tpl-card__actions">
                  <span class="ui-skeleton tpl-skeleton__button"></span>
                </div>
              </div>
            </div>

            <!-- 空状态 -->
            <div v-else-if="!visibleTemplates.length" class="ui-empty">
              <span class="ui-empty__icon">
                <AppIcon name="magnifying-glass" :size="24" />
              </span>
              <p class="ui-empty__title">没有符合条件的模板</p>
              <p class="ui-empty__hint">换一个分类或方向再看看，或清除筛选查看全部 {{ templates.length }} 套模板。</p>
              <button type="button" class="ui-btn ui-btn--outline" @click="resetFilters">清除筛选条件</button>
            </div>

            <div v-else class="tpl-grid">
              <article
                v-for="tpl in visibleTemplates"
                :key="tpl.id"
                class="tpl-card ui-panel ui-panel--hover"
                :class="{ 'is-selected': tpl.id === selectedTemplate?.id, 'is-current': tpl.id === currentTemplateId }"
              >
                <div
                  role="button"
                  tabindex="0"
                  class="tpl-card__pick"
                  :aria-pressed="tpl.id === selectedTemplate?.id"
                  :aria-label="`${tpl.name}，${tpl.tagline}`"
                  :title="tpl.desc"
                  @click="selectTemplate(tpl.id)"
                  @keydown.enter.prevent="selectTemplate(tpl.id)"
                  @keydown.space.prevent="selectTemplate(tpl.id)"
                >
                  <div class="tpl-card__preview">
                    <TemplatePreview :template-id="tpl.id" />
                    <span v-if="tpl.id === currentTemplateId" class="tpl-card__flag tpl-card__flag--current">
                      <AppIcon name="check-circle" :size="12" />
                      当前使用
                    </span>
                    <span v-else-if="tpl.id === selectedTemplate?.id" class="tpl-card__flag">
                      <AppIcon name="check" :size="12" />
                      已选择
                    </span>
                  </div>

                  <span class="tpl-card__body">
                    <span class="tpl-card__head">
                      <span class="tpl-card__name">{{ tpl.name }}</span>
                      <span class="ui-badge">{{ tpl.categoryLabel }}</span>
                    </span>
                    <span class="tpl-card__tagline">{{ tpl.tagline }}</span>
                    <span class="tpl-card__tags">
                      <span v-for="stage in tpl.stage" :key="stage" class="ui-badge">{{ stage }}</span>
                    </span>
                  </span>
                </div>

                <div class="tpl-card__actions">
                  <button
                    type="button"
                    class="ui-btn ui-btn--sm ui-btn--primary ui-btn--block"
                    :disabled="Boolean(creatingId)"
                    @click="handleUse(tpl)"
                  >
                    <AppIcon v-if="creatingId === tpl.id" name="spinner-gap" :size="14" spin />
                    <AppIcon v-else name="file-plus" :size="14" />
                    {{ creatingId === tpl.id ? '正在应用…' : selectedResumeId ? '应用到此简历' : '用此模板新建' }}
                  </button>
                </div>
              </article>
            </div>
          </div>

          <aside v-if="selectedTemplate" class="tpl-detail ui-panel" aria-label="选中模板的完整预览" aria-live="polite">
            <div class="tpl-detail__heading">
              <span class="ui-label">模板预览</span>
              <span class="ui-badge">{{ selectedTemplate.categoryLabel }}</span>
            </div>
            <div class="tpl-detail__sheet">
              <TemplatePreview :template-id="selectedTemplate.id" large />
            </div>
            <div class="tpl-detail__copy">
              <h3 class="tpl-detail__name">{{ selectedTemplate.name }}</h3>
              <p class="tpl-detail__tagline">{{ selectedTemplate.tagline }}</p>
              <p class="tpl-detail__description">{{ selectedTemplate.desc }}</p>
              <div class="tpl-detail__tags">
                <span v-for="stage in selectedTemplate.stage" :key="stage" class="ui-badge">{{ stage }}</span>
                <span v-for="track in selectedTemplate.track" :key="track" class="ui-badge ui-badge--neutral">{{ track }}</span>
              </div>
            </div>
            <button
              type="button"
              class="ui-btn ui-btn--primary ui-btn--block"
              :disabled="Boolean(creatingId)"
              @click="handleUse(selectedTemplate)"
            >
              <AppIcon v-if="creatingId === selectedTemplate.id" name="spinner-gap" :size="15" spin />
              <AppIcon v-else name="file-plus" :size="15" />
              {{ creatingId === selectedTemplate.id ? '正在应用…' : selectedResumeId ? '应用到此简历' : '用此模板新建' }}
            </button>
          </aside>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import AppIcon from '../components/common/AppIcon.vue'
import TemplatePreview from '../components/resume/TemplatePreview.vue'
import { useResumeStore } from '../store/resumeStore.js'
import { RESUME_TEMPLATE_OPTIONS } from '../data/resumeTemplates.js'

const router = useRouter()
const resumeStore = useResumeStore()

const templates = RESUME_TEMPLATE_OPTIONS

const CATEGORY_LIST = [
  { value: 'all', label: '全部' },
  ...RESUME_TEMPLATE_OPTIONS.map(({ category, categoryLabel }) => ({ value: category, label: categoryLabel })),
]

const sortOptions = [
  { value: 'recommended', label: '推荐' },
  { value: 'updated', label: '最近更新' },
  { value: 'name', label: '名称' },
]

const isBooting = ref(true)
const creatingId = ref('')
const errorMessage = ref('')
const activeCategory = ref('all')
const activeStage = ref('')
const activeTrack = ref('')
const activeSort = ref('recommended')
const selectedId = ref(templates[0]?.id || '')
const selectedResumeId = ref('')

const nameCollator = new Intl.Collator('zh-Hans-CN')

// 当前使用的模板来自用户选中的简历，不使用“最近更新”的简历代替选择。
const currentTemplateId = computed(() => {
  const resume = resumeStore.resumes.find((item) => String(item.id) === String(selectedResumeId.value))
  return resume?.template || ''
})

const selectedTemplate = computed(() => {
  return visibleTemplates.value.find((template) => template.id === selectedId.value) || visibleTemplates.value[0] || null
})

function matchCategory(tpl) {
  return activeCategory.value === 'all' || tpl.category === activeCategory.value
}

function matchStage(tpl) {
  return !activeStage.value || tpl.stage.includes(activeStage.value)
}

function matchTrack(tpl) {
  return !activeTrack.value || tpl.track.includes(activeTrack.value)
}

const visibleTemplates = computed(() => {
  const filtered = templates.filter((tpl) => matchCategory(tpl) && matchStage(tpl) && matchTrack(tpl))

  if (activeSort.value === 'updated') {
    return [...filtered].sort((left, right) => right.updatedAt.localeCompare(left.updatedAt))
  }

  if (activeSort.value === 'name') {
    return [...filtered].sort((left, right) => nameCollator.compare(left.name, right.name))
  }

  return [...filtered].sort((left, right) => left.weight - right.weight)
})

// 分类计数：受其它筛选条件影响，但不受自身所在分组影响
const categoryOptions = computed(() => {
  return CATEGORY_LIST.map((item) => ({
    ...item,
    count: templates.filter((tpl) => {
      if (!matchStage(tpl) || !matchTrack(tpl)) {
        return false
      }
      return item.value === 'all' || tpl.category === item.value
    }).length,
  }))
})

// 求职阶段与目标方向的选项来自模板自身的 stage / track 文案，label 直接取该文案
const stageOptions = computed(() => {
  const all = [...new Set(templates.flatMap((tpl) => tpl.stage))]
  return all.map((value) => ({
    value,
    label: value,
    count: templates.filter((tpl) => {
      if (!matchCategory(tpl) || !matchTrack(tpl)) {
        return false
      }
      return tpl.stage.includes(value)
    }).length,
  }))
})

const trackOptions = computed(() => {
  const all = [...new Set(templates.flatMap((tpl) => tpl.track))]
  return all.map((value) => ({
    value,
    label: value,
    count: templates.filter((tpl) => {
      if (!matchCategory(tpl) || !matchStage(tpl)) {
        return false
      }
      return tpl.track.includes(value)
    }).length,
  }))
})

const hasActiveFilter = computed(() => {
  return activeCategory.value !== 'all' || Boolean(activeStage.value) || Boolean(activeTrack.value)
})

function selectTemplate(templateId) {
  selectedId.value = templateId
}

function toggleStage(value) {
  activeStage.value = activeStage.value === value ? '' : value
}

function toggleTrack(value) {
  activeTrack.value = activeTrack.value === value ? '' : value
}

function resetFilters() {
  activeCategory.value = 'all'
  activeStage.value = ''
  activeTrack.value = ''
}

watch(visibleTemplates, (list) => {
  if (!list.some((template) => template.id === selectedId.value)) {
    selectedId.value = list[0]?.id || ''
  }
})

async function handleUse(tpl) {
  if (creatingId.value) {
    return
  }

  creatingId.value = tpl.id
  errorMessage.value = ''

  try {
    let id = selectedResumeId.value
    if (id) {
      await resumeStore.saveResume(id, { template: tpl.id })
    } else {
      const created = await resumeStore.createResume({ title: '新建简历', template: tpl.id })
      id = created?.id || created?._id
      if (!id) {
        errorMessage.value = '简历已创建，但没有拿到简历编号，请回到工作台查看。'
        return
      }
    }
    selectedId.value = tpl.id
    router.push({ path: `/editor/${id}`, query: { template: tpl.id } })
  } catch {
    errorMessage.value = '模板应用失败，请确认后端服务已启动，然后重试一次。'
  } finally {
    creatingId.value = ''
  }
}

onMounted(() => {
  isBooting.value = false
  void resumeStore.fetchResumes()
})
</script>

<style scoped>
/* ========================================
   模板市场 · 工作台布局
   左侧筛选栏（固定宽度 + 吸顶）+ 右侧模板卡片网格
   ======================================== */

.templates-view {
  /* 外层间距由壳层 .app-content 提供，这里不再叠加 padding */
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: var(--ui-space-6);
  color: var(--ui-fg-primary);
  font-family: var(--ui-font-sans);
}

.tpl-resume-target {
  display: grid;
  min-width: 210px;
  gap: var(--ui-space-1);
}

.tpl-resume-target .ui-label {
  margin: 0;
}

/* ---------- 页头 ---------- */
.page-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--ui-space-4);
  flex-wrap: wrap;
}

.page-head__text {
  min-width: 0;
}

.page-head__actions {
  display: flex;
  flex-shrink: 0;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--ui-space-2);
}

/* ---------- 主体两栏 ---------- */
.templates-view__body {
  display: flex;
  align-items: flex-start;
  gap: var(--ui-space-6);
  min-width: 0;
}

/* ---------- 左侧筛选栏 ---------- */
.tpl-filters {
  /* 固定宽度，不被右侧内容压扁；内容超高时内部滚动 */
  position: sticky;
  top: 0;
  flex: 0 0 216px;
  width: 216px;
  max-height: calc(100vh - var(--ui-header-height) - var(--ui-space-8));
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: var(--ui-space-5);
  padding: var(--ui-space-4);
}

.tpl-filters__group {
  display: flex;
  flex-direction: column;
  gap: var(--ui-space-2);
}

.tpl-filters__title {
  display: flex;
  align-items: center;
  gap: var(--ui-space-2);
  margin: 0;
  color: var(--ui-fg-muted);
  font-size: var(--ui-text-xs);
  font-weight: 600;
  letter-spacing: var(--ui-tracking-snug);
}

.tpl-filters__list {
  display: flex;
  flex-direction: column;
  gap: 2px;
  margin: 0;
  padding: 0;
  list-style: none;
}

/* 名称与计数徽标同一条基线：左名称右计数 */
.tpl-filters__item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--ui-space-2);
  width: 100%;
  min-width: 0;
  padding: var(--ui-space-1) var(--ui-space-2);
  border: 0;
  border-radius: var(--ui-radius-sm);
  background: transparent;
  color: var(--ui-fg-secondary);
  font-size: var(--ui-text-sm);
  line-height: 1.5rem;
  text-align: left;
  cursor: pointer;
  transition: background var(--ui-duration-fast) var(--ui-ease-out),
    color var(--ui-duration-fast) var(--ui-ease-out);
}

.tpl-filters__item:hover {
  background: var(--ui-bg-hover);
  color: var(--ui-fg-primary);
}

.tpl-filters__item:focus-visible {
  outline: none;
  box-shadow: var(--ui-ring);
}

.tpl-filters__item.is-active {
  background: var(--ui-bg-selected);
  color: var(--ui-fg-accent);
  font-weight: 600;
}

.tpl-filters__label {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.tpl-filters__count {
  flex-shrink: 0;
  min-width: 1.5rem;
  justify-content: center;
  font-variant-numeric: tabular-nums;
}

.tpl-filters__item.is-active .tpl-filters__count {
  background: var(--ui-bg-surface);
  border-color: var(--ui-accent-border);
  color: var(--ui-fg-accent);
}

/* 排序用分段控件，铺满一行 */
.tpl-filters__segmented {
  display: flex;
  width: 100%;
}

.tpl-filters__segmented .ui-segmented__item {
  flex: 1 1 0;
  min-width: 0;
  justify-content: center;
  padding: 0.3125rem var(--ui-space-1);
}

.tpl-filters__reset {
  margin-top: auto;
}

/* ---------- 右侧结果区 ---------- */
.tpl-results {
  flex: 1 1 auto;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: var(--ui-space-4);
}

.tpl-results__head {
  /* 间距交给 flex gap，避免和 .ui-section-head 默认外边距叠加 */
  margin-bottom: 0;
  align-items: flex-start;
}

.tpl-results__content {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(320px, 360px);
  align-items: start;
  gap: var(--ui-space-5);
}

.tpl-gallery {
  min-width: 0;
}

.tpl-detail {
  position: sticky;
  top: 0;
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: var(--ui-space-3);
  padding: var(--ui-space-4);
}

.tpl-detail__heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--ui-space-2);
}

.tpl-detail__sheet {
  height: 470px;
  overflow: hidden;
  border: 1px solid var(--ui-grid-line);
  background: var(--ui-neutral-100);
}

.tpl-detail__copy {
  display: flex;
  flex-direction: column;
  gap: var(--ui-space-2);
}

.tpl-detail__name,
.tpl-detail__tagline,
.tpl-detail__description {
  margin: 0;
}

.tpl-detail__name {
  color: var(--ui-fg-primary);
  font-size: var(--ui-text-lg);
  font-weight: 650;
}

.tpl-detail__tagline,
.tpl-detail__description {
  color: var(--ui-fg-secondary);
  font-size: var(--ui-text-sm);
  line-height: var(--ui-line-normal);
}

.tpl-detail__tags {
  display: flex;
  flex-wrap: wrap;
  gap: var(--ui-space-1);
}

/* ---------- 卡片网格 ---------- */
.tpl-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  align-items: stretch;
  gap: var(--ui-space-5);
}

.tpl-card {
  display: flex;
  flex-direction: column;
  min-width: 0;
  overflow: hidden;
}

.tpl-card.is-selected {
  border-color: var(--ui-accent-border);
  box-shadow: var(--ui-shadow-md);
}

.tpl-card.is-current {
  border-color: var(--ui-accent-border);
}

/* 整卡可点（选中预览），内部按钮独立 */
.tpl-card__pick {
  display: flex;
  flex-direction: column;
  gap: var(--ui-space-3);
  flex: 1 1 auto;
  width: 100%;
  padding: var(--ui-space-4);
  border: 0;
  background: transparent;
  color: inherit;
  font: inherit;
  text-align: left;
  cursor: pointer;
}

.tpl-card__pick:focus-visible {
  outline: none;
  box-shadow: var(--ui-ring);
  border-radius: var(--ui-radius-md);
}

.tpl-card__preview {
  position: relative;
  display: block;
  width: 100%;
  height: 300px;
  flex-shrink: 0;
  overflow: hidden;
  border-bottom: 1px solid var(--ui-grid-line);
  background: var(--ui-neutral-100);
}

.tpl-card__flag {
  position: absolute;
  top: var(--ui-space-2);
  right: var(--ui-space-2);
  display: inline-flex;
  align-items: center;
  gap: var(--ui-space-1);
  padding: 2px var(--ui-space-2);
  border-radius: var(--ui-radius-full);
  background: var(--ui-bg-surface);
  border: 1px solid var(--ui-border-strong);
  color: var(--ui-fg-secondary);
  font-size: var(--ui-text-xs);
  font-weight: 600;
  line-height: 1.25rem;
}

.tpl-card__flag--current {
  border-color: var(--ui-accent-border);
  background: var(--ui-accent-soft);
  color: var(--ui-fg-accent);
}

.tpl-card__body {
  display: flex;
  flex-direction: column;
  gap: var(--ui-space-2);
  min-width: 0;
}

.tpl-card__head {
  display: flex;
  align-items: center;
  gap: var(--ui-space-2);
  flex-wrap: wrap;
}

.tpl-card__name {
  font-size: var(--ui-text-md);
  font-weight: 600;
  color: var(--ui-fg-primary);
}

.tpl-card__tagline {
  color: var(--ui-fg-secondary);
  font-size: var(--ui-text-sm);
  line-height: var(--ui-line-normal);
}

.tpl-card__tags {
  display: flex;
  flex-wrap: wrap;
  gap: var(--ui-space-1) var(--ui-space-2);
}

.tpl-card__actions {
  margin-top: auto;
  padding: 0 var(--ui-space-4) var(--ui-space-4);
}

/* ---------- 骨架屏 ---------- */
.tpl-card--skeleton {
  pointer-events: none;
}

.tpl-skeleton__thumb {
  width: 100%;
  height: 100%;
  border-radius: 0;
}

.tpl-skeleton__title {
  width: 42%;
  height: 16px;
}

.tpl-skeleton__line {
  width: 88%;
  height: 11px;
}

.tpl-skeleton__tag {
  width: 56%;
  height: 18px;
  border-radius: var(--ui-radius-full);
}

.tpl-skeleton__button {
  width: 100%;
  height: 30px;
  border-radius: var(--ui-radius-md);
}

/* ---------- 窄窗口适配 ---------- */
@media (max-width: 1100px) {
  .templates-view__body {
    flex-direction: column;
    gap: var(--ui-space-4);
  }

  .tpl-filters {
    position: static;
    flex: none;
    width: 100%;
    max-height: none;
    overflow: visible;
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
    gap: var(--ui-space-4);
    align-items: start;
  }

  .tpl-detail {
    position: static;
  }

  .tpl-filters__reset {
    grid-column: 1 / -1;
    margin-top: 0;
  }
}
</style>
