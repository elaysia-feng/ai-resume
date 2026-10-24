<template>
  <div class="templates-view">
    <!-- 顶部说明：当前使用 / 已选模板 -->
    <header class="templates-view__bar">
      <p class="templates-view__summary">
        <span class="templates-view__badge templates-view__badge--current">
          当前使用：{{ currentTemplate ? currentTemplate.name : '暂无简历' }}
        </span>
        <span class="templates-view__badge">已选：{{ selectedTemplateName }}</span>
        <span class="templates-view__count">共 {{ templates.length }} 套模板</span>
      </p>

      <p v-if="errorMessage" class="templates-view__error" role="alert">
        {{ errorMessage }}
      </p>
    </header>

    <div class="templates-view__body">
      <!-- 左侧筛选栏 -->
      <aside class="tpl-filters" aria-label="模板筛选">
        <section class="tpl-filters__group">
          <h2 class="tpl-filters__title">模板分类</h2>
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
                <span class="tpl-filters__count">{{ item.count }}</span>
              </button>
            </li>
          </ul>
        </section>

        <section class="tpl-filters__group">
          <h2 class="tpl-filters__title">求职阶段</h2>
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
                <span class="tpl-filters__count">{{ item.count }}</span>
              </button>
            </li>
          </ul>
        </section>

        <section class="tpl-filters__group">
          <h2 class="tpl-filters__title">目标方向</h2>
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
                <span class="tpl-filters__count">{{ item.count }}</span>
              </button>
            </li>
          </ul>
        </section>

        <section class="tpl-filters__group">
          <h2 class="tpl-filters__title">排序</h2>
          <ul class="tpl-filters__list">
            <li v-for="item in sortOptions" :key="item.value">
              <button
                type="button"
                class="tpl-filters__item"
                :class="{ 'is-active': activeSort === item.value }"
                :aria-pressed="activeSort === item.value"
                @click="activeSort = item.value"
              >
                <span class="tpl-filters__label">{{ item.label }}</span>
              </button>
            </li>
          </ul>
        </section>

        <button
          type="button"
          class="ui-btn ui-btn--sm ui-btn--outline ui-btn--block tpl-filters__reset"
          :disabled="!hasActiveFilter"
          @click="resetFilters"
        >
          清除筛选
        </button>
      </aside>

      <!-- 右侧模板卡片网格 -->
      <section class="tpl-results" aria-label="模板列表">
        <div class="tpl-results__head">
          <span class="tpl-results__title">
            {{ visibleTemplates.length }} 个模板<span v-if="hasActiveFilter">（已筛选）</span>
          </span>
          <span class="tpl-results__hint">点击卡片可先预览版式，再决定使用哪一套</span>
        </div>

        <!-- 加载中：骨架卡片，不做整页遮罩 -->
        <div v-if="isBooting" class="tpl-grid">
          <div v-for="index in templates.length" :key="index" class="tpl-card tpl-card--skeleton" aria-hidden="true">
            <div class="tpl-card__preview">
              <div class="tpl-thumb tpl-thumb--skeleton"></div>
            </div>
            <div class="tpl-card__body">
              <span class="tpl-skeleton tpl-skeleton--title"></span>
              <span class="tpl-skeleton tpl-skeleton--line"></span>
              <span class="tpl-skeleton tpl-skeleton--tag"></span>
            </div>
            <div class="tpl-card__actions">
              <span class="tpl-skeleton tpl-skeleton--button"></span>
            </div>
          </div>
        </div>

        <!-- 空状态 -->
        <div v-else-if="!visibleTemplates.length" class="ui-empty">
          <p class="ui-empty__title">没有符合条件的模板</p>
          <p class="ui-empty__hint">换一个分类或方向再看看，或清除筛选查看全部 {{ templates.length }} 套模板。</p>
          <button type="button" class="ui-btn ui-btn--outline" @click="resetFilters">清除筛选条件</button>
        </div>

        <div v-else class="tpl-grid">
          <article
            v-for="tpl in visibleTemplates"
            :key="tpl.id"
            class="tpl-card"
            :class="{ 'is-selected': tpl.id === selectedId, 'is-current': tpl.id === currentTemplateId }"
          >
            <button
              type="button"
              class="tpl-card__pick"
              :aria-pressed="tpl.id === selectedId"
              :title="tpl.desc"
              @click="selectTemplate(tpl.id)"
            >
              <span class="tpl-card__preview">
                <span class="tpl-thumb" :class="`tpl-thumb--${tpl.thumb}`" aria-hidden="true">
                  <span class="tpl-thumb__page">
                    <span class="tpl-thumb__bar">
                      <span class="tpl-thumb__bar-name"></span>
                      <span class="tpl-thumb__bar-role"></span>
                    </span>
                    <span class="tpl-thumb__cols">
                      <span class="tpl-thumb__aside">
                        <span class="tpl-thumb__avatar"></span>
                        <span class="tpl-thumb__name"></span>
                        <span class="tpl-thumb__role"></span>
                        <span class="tpl-thumb__chip"></span>
                        <span class="tpl-thumb__chip"></span>
                        <span class="tpl-thumb__chip"></span>
                      </span>
                      <span class="tpl-thumb__main">
                        <span class="tpl-thumb__title"></span>
                        <span class="tpl-thumb__sub"></span>
                        <span class="tpl-thumb__block">
                          <span class="tpl-thumb__label"></span>
                          <span class="tpl-thumb__line"></span>
                          <span class="tpl-thumb__line tpl-thumb__line--short"></span>
                        </span>
                        <span class="tpl-thumb__block">
                          <span class="tpl-thumb__label"></span>
                          <span class="tpl-thumb__line"></span>
                          <span class="tpl-thumb__line tpl-thumb__line--short"></span>
                          <span class="tpl-thumb__line"></span>
                        </span>
                      </span>
                    </span>
                  </span>
                </span>

                <span v-if="tpl.id === currentTemplateId" class="tpl-card__flag tpl-card__flag--current">当前使用</span>
                <span v-else-if="tpl.id === selectedId" class="tpl-card__flag">已选择</span>
              </span>

              <span class="tpl-card__body">
                <span class="tpl-card__head">
                  <span class="tpl-card__name">{{ tpl.name }}</span>
                  <span class="ui-badge">{{ tpl.categoryLabel }}</span>
                </span>
                <span class="tpl-card__tagline">{{ tpl.tagline }}</span>
                <span class="tpl-card__tags">
                  <span v-for="tag in tpl.tags" :key="tag" class="ui-badge">{{ tag }}</span>
                </span>
              </span>
            </button>

            <div class="tpl-card__actions">
              <button
                type="button"
                class="ui-btn ui-btn--sm ui-btn--primary ui-btn--block"
                :disabled="Boolean(creatingId)"
                @click="handleUse(tpl)"
              >
                {{ creatingId === tpl.id ? '正在创建…' : '使用该模板' }}
              </button>
            </div>
          </article>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useResumeStore } from '../store/resumeStore.js'

const router = useRouter()
const resumeStore = useResumeStore()

// 模板注册表：分类 / 适用场景 / 版式缩略图 / 排序权重
const templates = [
  {
    id: 'classic',
    name: '标准版',
    category: 'classic',
    categoryLabel: '经典',
    tagline: '单栏通栏，最通用的正式投递版式',
    desc: '姓名与联系方式置顶，模块标题下带细线分隔，层级稳定，适合正式投递与打印归档。',
    stage: ['通用', '校招', '社招'],
    track: ['程序员', '产品'],
    thumb: 'classic',
    updatedAt: '2026-09-02',
    weight: 1,
  },
  {
    id: 'modern',
    name: '简约求职版',
    category: 'modern',
    categoryLabel: '现代',
    tagline: '左侧信息栏 + 右侧主栏，留白多、层级清楚',
    desc: '个人信息集中在左栏，正文在右栏展开，段落间距宽松，一眼能看清经历主线。',
    stage: ['通用', '校招', '社招'],
    track: ['程序员', '前端'],
    thumb: 'modern',
    updatedAt: '2026-10-06',
    weight: 2,
  },
  {
    id: 'compact',
    name: '双列紧凑版',
    category: 'compact',
    categoryLabel: '紧凑',
    tagline: '字号更小、间距更紧，一页塞下更多信息',
    desc: '窄左栏加紧凑正文，项目、证书可以多放几条，适合实习与应届投递。',
    stage: ['校招', '实习生'],
    track: ['前端', '技术岗'],
    thumb: 'compact',
    updatedAt: '2026-09-21',
    weight: 3,
  },
  {
    id: 'academic',
    name: '学术科研版',
    category: 'academic',
    categoryLabel: '学术',
    tagline: '居中式抬头 + 全宽分隔线，严谨克制',
    desc: '教育背景前置，模块标题用衬线体加全宽细线，适合科研、教育与技术研发方向。',
    stage: ['校招', '社招'],
    track: ['研究', '教育', '技术岗'],
    thumb: 'academic',
    updatedAt: '2026-08-27',
    weight: 4,
  },
  {
    id: 'creative',
    name: '创意进阶版',
    category: 'creative',
    categoryLabel: '创意',
    tagline: '强调色块 + 分区卡片，视觉分区更明显',
    desc: '抬头用色块与竖向分割线切分版面，每个模块独立成卡，适合设计与品牌类岗位。',
    stage: ['社招'],
    track: ['设计', 'UI', '品牌'],
    thumb: 'creative',
    updatedAt: '2026-10-12',
    weight: 5,
  },
  {
    id: 'dual-creative',
    name: '双列创意版',
    category: 'dual-creative',
    categoryLabel: '双栏创意',
    tagline: '左右等宽双栏 + 中缝分隔线，信息密度与美感兼顾',
    desc: '两栏等宽排布，左栏放技能与证书、右栏放经历，栏间用细线分隔，适合前端与交互岗位。',
    stage: ['校招', '社招'],
    track: ['前端', '设计', '产品'],
    thumb: 'dual',
    updatedAt: '2026-10-19',
    weight: 6,
  },
]

const CATEGORY_LIST = [
  { value: 'all', label: '全部' },
  { value: 'classic', label: '经典' },
  { value: 'modern', label: '现代' },
  { value: 'compact', label: '紧凑' },
  { value: 'academic', label: '学术' },
  { value: 'creative', label: '创意' },
  { value: 'dual-creative', label: '双栏创意' },
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
const selectedId = ref('')

const nameCollator = new Intl.Collator('zh-Hans-CN')

// 「当前使用」= 最近更新的那份简历所用的模板
const currentTemplateId = computed(() => {
  const list = resumeStore.resumes
  if (!list.length) {
    return ''
  }

  const latest = [...list].sort((left, right) => {
    return new Date(right?.updatedAt || 0).getTime() - new Date(left?.updatedAt || 0).getTime()
  })[0]

  return latest?.template || ''
})

const currentTemplate = computed(() => {
  return templates.find((item) => item.id === currentTemplateId.value) || null
})

const selectedTemplateName = computed(() => {
  return templates.find((item) => item.id === selectedId.value)?.name || '未选择'
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

const stageOptions = computed(() => {
  const all = [...new Set(templates.flatMap((tpl) => tpl.stage))]
  return all.map((value) => ({
    value,
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

async function handleUse(tpl) {
  if (creatingId.value) {
    return
  }

  creatingId.value = tpl.id
  errorMessage.value = ''

  try {
    const created = await resumeStore.createResume({ title: 'Untitled Resume', template: tpl.id })
    const id = created?.id || created?._id
    if (!id) {
      errorMessage.value = '简历已创建，但没有拿到简历编号，请到「我的简历」列表里打开它。'
      return
    }
    selectedId.value = tpl.id
    router.push({ path: `/editor/${id}`, query: { template: tpl.id } })
  } catch {
    errorMessage.value = '创建简历失败，请确认后端服务已启动，然后重试一次。'
  } finally {
    creatingId.value = ''
  }
}

onMounted(async () => {
  await resumeStore.fetchResumes()
  selectedId.value = currentTemplateId.value
  isBooting.value = false
})
</script>

<style scoped>
/* ========================================
   模板市场 · 工作台布局
   左侧筛选栏 + 右侧模板卡片网格
   ======================================== */

.templates-view {
  /* 外层间距由壳层 .app-content 提供，这里不再叠加 padding，间距做在内部区块上 */
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: var(--ui-space-6);
  color: var(--ui-fg-primary);
  font-family: var(--ui-font-sans);
}

/* ---------- 顶部说明 ---------- */
.templates-view__bar {
  display: flex;
  flex-direction: column;
  gap: var(--ui-space-2);
}

.templates-view__summary {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--ui-space-2) var(--ui-space-3);
  margin: 0;
}

.templates-view__badge {
  display: inline-flex;
  align-items: center;
  padding: 2px var(--ui-space-3);
  border: 1px solid var(--ui-border);
  border-radius: var(--ui-radius-full);
  background: var(--ui-bg-surface);
  color: var(--ui-fg-secondary);
  font-size: var(--ui-text-xs);
  line-height: 1.5rem;
}

.templates-view__badge--current {
  border-color: var(--ui-accent-border);
  background: var(--ui-accent-soft);
  color: var(--ui-fg-accent);
  font-weight: 600;
}

.templates-view__count {
  color: var(--ui-fg-muted);
  font-size: var(--ui-text-xs);
}

.templates-view__error {
  margin: 0;
  padding: var(--ui-space-2) var(--ui-space-3);
  border: 1px solid var(--ui-danger-border);
  border-radius: var(--ui-radius-md);
  background: var(--ui-danger-soft);
  color: var(--ui-danger);
  font-size: var(--ui-text-sm);
}

/* ---------- 主体两栏 ---------- */
.templates-view__body {
  display: grid;
  grid-template-columns: 208px minmax(0, 1fr);
  gap: var(--ui-space-6);
  align-items: start;
}

/* ---------- 左侧筛选栏 ---------- */
.tpl-filters {
  position: sticky;
  top: 0;
  display: flex;
  flex-direction: column;
  gap: var(--ui-space-5);
  padding: var(--ui-space-4);
  border: 1px solid var(--ui-border);
  border-radius: var(--ui-radius-lg);
  background: var(--ui-bg-surface);
  box-shadow: var(--ui-shadow-xs);
}

.tpl-filters__group {
  display: flex;
  flex-direction: column;
  gap: var(--ui-space-2);
}

.tpl-filters__title {
  margin: 0;
  color: var(--ui-fg-muted);
  font-size: var(--ui-text-xs);
  font-weight: 600;
  letter-spacing: 0.04em;
}

.tpl-filters__list {
  display: flex;
  flex-direction: column;
  gap: 2px;
  margin: 0;
  padding: 0;
  list-style: none;
}

.tpl-filters__item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--ui-space-2);
  width: 100%;
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

.tpl-filters__count {
  flex-shrink: 0;
  min-width: 20px;
  padding: 0 6px;
  border-radius: var(--ui-radius-full);
  background: var(--ui-bg-sunken);
  color: var(--ui-fg-muted);
  font-size: var(--ui-text-xs);
  line-height: 1.25rem;
  text-align: center;
}

.tpl-filters__item.is-active .tpl-filters__count {
  background: var(--ui-accent-soft);
  color: var(--ui-fg-accent);
}

.tpl-filters__reset {
  margin-top: auto;
}

/* ---------- 右侧结果区 ---------- */
.tpl-results {
  display: flex;
  flex-direction: column;
  gap: var(--ui-space-4);
  min-width: 0;
}

.tpl-results__head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: var(--ui-space-3);
  flex-wrap: wrap;
}

.tpl-results__title {
  font-size: var(--ui-text-md);
  font-weight: 600;
  color: var(--ui-fg-primary);
}

.tpl-results__hint {
  color: var(--ui-fg-muted);
  font-size: var(--ui-text-xs);
}

/* ---------- 卡片网格 ---------- */
.tpl-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(272px, 1fr));
  gap: var(--ui-space-5);
}

.tpl-card {
  display: flex;
  flex-direction: column;
  min-width: 0;
  border: 1px solid var(--ui-border);
  border-radius: var(--ui-radius-lg);
  background: var(--ui-bg-surface);
  box-shadow: var(--ui-shadow-xs);
  overflow: hidden;
  transition: transform var(--ui-duration-normal) var(--ui-ease-out),
    box-shadow var(--ui-duration-normal) var(--ui-ease-out),
    border-color var(--ui-duration-normal) var(--ui-ease-out);
}

.tpl-card:hover {
  transform: translateY(-3px);
  box-shadow: var(--ui-shadow-md);
  border-color: var(--ui-border-strong);
}

.tpl-card:focus-within {
  border-color: var(--ui-border-focus);
}

.tpl-card.is-selected {
  border-color: var(--ui-accent);
  box-shadow: var(--ui-ring), var(--ui-shadow-sm);
}

.tpl-card.is-current {
  border-color: var(--ui-accent-border);
}

/* 整卡可点（选中预览），内部按钮独立 -->
.tpl-card__pick {
  display: flex;
  flex-direction: column;
  gap: var(--ui-space-3);
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
}

.tpl-card__flag {
  position: absolute;
  top: var(--ui-space-2);
  right: var(--ui-space-2);
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

.tpl-skeleton {
  display: block;
  border-radius: var(--ui-radius-sm);
  background: var(--ui-bg-sunken);
  animation: tpl-skeleton-pulse 1.4s var(--ui-ease-in-out) infinite;
}

.tpl-skeleton--title {
  width: 42%;
  height: 16px;
}

.tpl-skeleton--line {
  width: 88%;
  height: 11px;
}

.tpl-skeleton--tag {
  width: 56%;
  height: 18px;
  border-radius: var(--ui-radius-full);
}

.tpl-skeleton--button {
  width: 100%;
  height: 30px;
  border-radius: var(--ui-radius-md);
}

@keyframes tpl-skeleton-pulse {
  0%,
  100% {
    opacity: 1;
  }

  50% {
    opacity: 0.45;
  }
}

/* ========================================
   模板缩略预览（纯 CSS 版式示意）
   ======================================== */
.tpl-thumb {
  display: block;
  width: 100%;
  aspect-ratio: 210 / 297;
  border: 1px solid var(--ui-border);
  border-radius: var(--ui-radius-md);
  background: var(--ui-neutral-100);
  overflow: hidden;
}

.tpl-thumb__page {
  display: flex;
  flex-direction: column;
  width: 100%;
  height: 100%;
  background: var(--ui-neutral-0);
}

.tpl-thumb__bar {
  display: none;
  flex-direction: column;
  gap: 4px;
  padding: 9% 8% 7%;
  border-left: 4px solid var(--ui-accent);
  background: var(--ui-accent-soft);
}

.tpl-thumb__bar-name {
  display: block;
  width: 44%;
  height: 7px;
  border-radius: 2px;
  background: var(--ui-neutral-800);
}

.tpl-thumb__bar-role {
  display: block;
  width: 26%;
  height: 4px;
  border-radius: 2px;
  background: var(--ui-neutral-400);
}

.tpl-thumb__cols {
  display: grid;
  flex: 1;
  min-height: 0;
  gap: 7%;
  padding: 8% 8% 9%;
}

.tpl-thumb__aside {
  display: none;
  flex-direction: column;
  gap: 5px;
  padding: 8%;
  background: var(--ui-neutral-100);
}

.tpl-thumb__avatar {
  display: block;
  width: 26px;
  height: 26px;
  border-radius: 50%;
  background: var(--ui-neutral-300);
}

.tpl-thumb__name {
  display: block;
  width: 78%;
  height: 6px;
  border-radius: 2px;
  background: var(--ui-neutral-800);
}

.tpl-thumb__role {
  display: block;
  width: 56%;
  height: 4px;
  border-radius: 2px;
  background: var(--ui-neutral-400);
}

.tpl-thumb__chip {
  display: block;
  width: 86%;
  height: 5px;
  border-radius: 3px;
  background: var(--ui-neutral-200);
}

.tpl-thumb__main {
  display: flex;
  flex-direction: column;
  gap: 8px;
  min-width: 0;
}

.tpl-thumb__title {
  display: block;
  width: 46%;
  height: 8px;
  border-radius: 2px;
  background: var(--ui-neutral-800);
}

.tpl-thumb__sub {
  display: block;
  width: 28%;
  height: 4px;
  border-radius: 2px;
  background: var(--ui-neutral-400);
}

.tpl-thumb__block {
  display: flex;
  flex-direction: column;
  gap: 3px;
  padding-bottom: 5px;
  border-bottom: 1px solid var(--ui-neutral-200);
}

.tpl-thumb__label {
  display: block;
  width: 24%;
  height: 5px;
  border-radius: 2px;
  background: var(--ui-accent);
}

.tpl-thumb__line {
  display: block;
  width: 100%;
  height: 3px;
  border-radius: 2px;
  background: var(--ui-neutral-200);
}

.tpl-thumb__line--short {
  width: 56%;
}

/* 经典：单栏 + 顶部强调线 */
.tpl-thumb--classic .tpl-thumb__cols {
  grid-template-columns: 1fr;
}

.tpl-thumb--classic .tpl-thumb__title {
  box-shadow: -5px 0 0 0 var(--ui-accent);
  padding-left: 7px;
}

.tpl-thumb--classic .tpl-thumb__block {
  border-bottom-color: var(--ui-neutral-300);
}

/* 现代：左栏 + 右主栏 */
.tpl-thumb--modern .tpl-thumb__cols {
  grid-template-columns: 30% minmax(0, 1fr);
  padding: 0;
  gap: 0;
}

.tpl-thumb--modern .tpl-thumb__aside {
  display: flex;
  margin: 0;
  padding: 12% 8%;
}

.tpl-thumb--modern .tpl-thumb__main {
  padding: 12% 9%;
}

.tpl-thumb--modern .tpl-thumb__title,
.tpl-thumb--modern .tpl-thumb__sub {
  display: none;
}

.tpl-thumb--modern .tpl-thumb__block {
  padding: 7px;
  border: 1px solid var(--ui-neutral-200);
  border-radius: 4px;
  background: var(--ui-neutral-50);
}

/* 紧凑：更窄左栏、行距更密 */
.tpl-thumb--compact .tpl-thumb__cols {
  grid-template-columns: 25% minmax(0, 1fr);
  padding: 0;
  gap: 0;
}

.tpl-thumb--compact .tpl-thumb__aside {
  display: flex;
  margin: 0;
  padding: 10% 7%;
  gap: 3px;
}

.tpl-thumb--compact .tpl-thumb__avatar {
  width: 20px;
  height: 20px;
}

.tpl-thumb--compact .tpl-thumb__main {
  padding: 9% 8%;
  gap: 5px;
}

.tpl-thumb--compact .tpl-thumb__title,
.tpl-thumb--compact .tpl-thumb__sub {
  display: none;
}

.tpl-thumb--compact .tpl-thumb__block {
  padding-bottom: 3px;
  gap: 2px;
}

.tpl-thumb--compact .tpl-thumb__line {
  height: 2px;
}

/* 学术：居中抬头 + 全宽细线 */
.tpl-thumb--academic .tpl-thumb__cols {
  grid-template-columns: 1fr;
}

.tpl-thumb--academic .tpl-thumb__main {
  align-items: center;
}

.tpl-thumb--academic .tpl-thumb__title {
  width: 40%;
  text-align: center;
}

.tpl-thumb--academic .tpl-thumb__sub {
  width: 24%;
  text-align: center;
  padding-bottom: 8px;
  border-bottom: 2px solid var(--ui-neutral-700);
  margin-bottom: 4px;
}

.tpl-thumb--academic .tpl-thumb__block {
  width: 100%;
  align-items: flex-start;
}

.tpl-thumb--academic .tpl-thumb__label {
  width: 30%;
  height: 4px;
  background: var(--ui-neutral-700);
  padding-bottom: 3px;
  border-bottom: 1px solid var(--ui-neutral-400);
}

/* 创意：强调色块 + 竖向分割线 */
.tpl-thumb--creative .tpl-thumb__bar {
  display: flex;
}

.tpl-thumb--creative .tpl-thumb__cols {
  grid-template-columns: 33% minmax(0, 1fr);
  padding: 0;
  gap: 0;
}

.tpl-thumb--creative .tpl-thumb__aside {
  display: flex;
  margin: 0;
  padding: 10% 8%;
  border-right: 2px solid var(--ui-accent);
  background: transparent;
}

.tpl-thumb--creative .tpl-thumb__avatar,
.tpl-thumb--creative .tpl-thumb__name,
.tpl-thumb--creative .tpl-thumb__role {
  display: none;
}

.tpl-thumb--creative .tpl-thumb__main {
  padding: 10% 9%;
}

.tpl-thumb--creative .tpl-thumb__title,
.tpl-thumb--creative .tpl-thumb__sub {
  display: none;
}

.tpl-thumb--creative .tpl-thumb__block {
  padding: 7px;
  border: 1px solid var(--ui-neutral-200);
  border-radius: 5px;
  background: var(--ui-neutral-50);
}

.tpl-thumb--creative .tpl-thumb__block .tpl-thumb__label {
  width: 34%;
}

/* 双栏创意：等宽双栏 + 中缝细线 */
.tpl-thumb--dual .tpl-thumb__bar {
  display: flex;
}

.tpl-thumb--dual .tpl-thumb__cols {
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  padding: 0;
  gap: 0;
}

.tpl-thumb--dual .tpl-thumb__aside {
  display: flex;
  margin: 0;
  padding: 10% 9%;
  border-right: 1px solid var(--ui-neutral-300);
  background: transparent;
}

.tpl-thumb--dual .tpl-thumb__avatar,
.tpl-thumb--dual .tpl-thumb__name,
.tpl-thumb--dual .tpl-thumb__role {
  display: none;
}

.tpl-thumb--dual .tpl-thumb__main {
  padding: 10% 9%;
}

.tpl-thumb--dual .tpl-thumb__title,
.tpl-thumb--dual .tpl-thumb__sub {
  display: none;
}

.tpl-thumb--dual .tpl-thumb__block {
  border-bottom-style: solid;
}

/* 骨架占位 */
.tpl-thumb--skeleton {
  background: var(--ui-bg-sunken);
  border-color: var(--ui-border-subtle);
  animation: tpl-skeleton-pulse 1.4s var(--ui-ease-in-out) infinite;
}

/* ---------- 窄窗口适配 ---------- */
@media (max-width: 1180px) {
  .templates-view__body {
    grid-template-columns: 180px minmax(0, 1fr);
    gap: var(--ui-space-5);
  }
}

@media (max-width: 900px) {
  .templates-view__body {
    grid-template-columns: minmax(0, 1fr);
  }

  .tpl-filters {
    position: static;
  }
}
</style>
