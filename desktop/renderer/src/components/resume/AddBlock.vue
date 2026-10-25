<template>
  <section class="add-block">
    <header class="add-block__head">
      <div>
        <h2 class="ui-page-title section-title">添加区块</h2>
        <p class="add-block__hint">选择要加入简历的内容类型，已添加的区块会标记为不可重复添加。</p>
      </div>
      <input
        v-model="keyword"
        type="search"
        class="ui-input add-block__search"
        placeholder="搜索区块，例如「教育」"
        aria-label="搜索区块"
      />
    </header>

    <div v-if="showCustomForm" class="custom-form">
      <div class="custom-form__head">
        <span class="custom-form__title">创建自定义区块</span>
        <button type="button" class="ui-btn ui-btn--sm ui-btn--ghost" @click.stop="toggleCustomForm">收起</button>
      </div>

      <div class="custom-form__body">
        <div class="field">
          <label class="ui-label" for="custom-block-name">区块名称 <span class="required">*</span></label>
          <input
            id="custom-block-name"
            v-model="customName"
            class="ui-input"
            type="text"
            placeholder="例如：开源项目"
          />
        </div>

        <div class="field">
          <span class="ui-label">内容类型</span>
          <div class="radio-group">
            <label class="radio-item">
              <input type="radio" v-model="customSchemaType" value="TEXT" />
              <span>文本（TEXT）</span>
            </label>
            <label class="radio-item">
              <input type="radio" v-model="customSchemaType" value="LIST" />
              <span>列表（LIST）</span>
            </label>
            <label class="radio-item">
              <input type="radio" v-model="customSchemaType" value="TAGS" />
              <span>标签（TAGS）</span>
            </label>
          </div>
        </div>

        <div v-if="customSchemaType === 'TEXT'" class="field">
          <label class="ui-label" for="custom-block-text">内容</label>
          <textarea
            id="custom-block-text"
            v-model="customTextContent"
            class="ui-input custom-form__textarea"
            placeholder="请输入文本内容…"
            rows="4"
          ></textarea>
        </div>

        <div v-if="customSchemaType === 'LIST'" class="field">
          <label class="ui-label" for="custom-block-list">条目内容</label>
          <textarea
            id="custom-block-list"
            v-model="customListContent"
            class="ui-input custom-form__textarea"
            placeholder="每行一条，例如：&#10;项目一描述&#10;项目二描述&#10;项目三描述"
            rows="4"
          ></textarea>
          <span class="field-hint">每行作为一条列表项</span>
        </div>

        <div v-if="customSchemaType === 'TAGS'" class="field">
          <label class="ui-label" for="custom-block-tags">标签</label>
          <input
            id="custom-block-tags"
            v-model="customTagsContent"
            class="ui-input"
            type="text"
            placeholder="例如：Vue, React, TypeScript"
          />
          <span class="field-hint">用逗号分隔多个标签</span>
        </div>

        <button
          type="button"
          class="ui-btn ui-btn--sm ui-btn--primary custom-form__submit"
          :disabled="!customName.trim()"
          @click.stop="submitCustomBlock"
        >
          添加区块
        </button>
      </div>
    </div>

    <template v-for="group in visibleGroups" :key="group.key">
      <section class="add-block__group">
        <h3 class="add-block__group-title">{{ group.label }}</h3>
        <div class="blocks-grid">
          <button
            v-for="block in group.blocks"
            :key="block.id"
            type="button"
            class="block-card"
            :class="{ 'is-added': isAdded(block.id) }"
            :disabled="isAdded(block.id)"
            @click="handleAdd(block.id)"
          >
            <span class="block-icon" aria-hidden="true">
              <AppIcon :name="block.icon" :size="20" />
            </span>
            <span class="block-info">
              <span class="block-name">{{ block.name }}</span>
              <span class="block-desc">{{ block.desc }}</span>
            </span>
            <span v-if="isAdded(block.id)" class="ui-badge ui-badge--success block-check">已添加</span>
          </button>
        </div>
      </section>
    </template>

    <div v-if="!visibleGroups.length" class="ui-empty add-block__empty">
      <div class="ui-empty__icon">
        <AppIcon name="magnifying-glass" :size="24" />
      </div>
      <div class="ui-empty__title">没有匹配的区块</div>
      <div class="ui-empty__hint">换个关键词试试，或使用下方的「自定义区块」自由创建。</div>
    </div>

    <button
      type="button"
      class="block-card block-card--custom"
      :class="{ 'is-expanded': showCustomForm }"
      @click="toggleCustomForm"
    >
      <span class="block-icon" aria-hidden="true">
        <AppIcon name="plus" :size="20" />
      </span>
      <span class="block-info">
        <span class="block-name">自定义区块</span>
        <span class="block-desc">按自己的结构创建内容，如开源项目、获奖情况</span>
      </span>
    </button>
  </section>
</template>

<script setup>
import { computed, ref } from 'vue'
import AppIcon from '../common/AppIcon.vue'

const emit = defineEmits(['add-block'])

// 自定义区块表单状态
const showCustomForm = ref(false)
const customName = ref('')
const customSchemaType = ref('TEXT')
const customTextContent = ref('')
const customListContent = ref('')
const customTagsContent = ref('')
const keyword = ref('')

function toggleCustomForm() {
  showCustomForm.value = !showCustomForm.value
  if (!showCustomForm.value) {
    resetCustomForm()
  }
}

function resetCustomForm() {
  customName.value = ''
  customSchemaType.value = 'TEXT'
  customTextContent.value = ''
  customListContent.value = ''
  customTagsContent.value = ''
}

function submitCustomBlock() {
  if (!customName.value.trim()) return

  const sectionCode = 'CUSTOM_' + Date.now()
  let contentJson = {}

  if (customSchemaType.value === 'TEXT') {
    contentJson = { text: customTextContent.value }
  } else if (customSchemaType.value === 'LIST') {
    // 每行拆成一条列表项
    const items = customListContent.value
      .split('\n')
      .map(line => line.trim())
      .filter(line => line.length > 0)
      .map(text => ({ text }))
    contentJson = { items }
  } else if (customSchemaType.value === 'TAGS') {
    // 逗号分隔成标签项
    const items = customTagsContent.value
      .split(',')
      .map(tag => tag.trim())
      .filter(tag => tag.length > 0)
    contentJson = { items }
  }

  emit('add-block', {
    sectionType: 'CUSTOM',
    sectionCode,
    sectionTitle: customName.value.trim(),
    schemaType: customSchemaType.value,
    contentJson,
  })

  showCustomForm.value = false
  resetCustomForm()
}

// 各区块的图标，取自 src/icons/paths.js 的键名
const ICONS = {
  person: 'user',
  target: 'target',
  cap: 'graduation-cap',
  case: 'briefcase',
  school: 'buildings',
  bolt: 'code',
  award: 'medal',
  pen: 'note-pencil',
  rocket: 'git-branch',
  sprout: 'hourglass-medium',
}

const blocks = [
  {
    id: 'BASIC',
    group: 'base',
    icon: ICONS.person,
    name: '基本信息',
    desc: '姓名、职位、联系方式',
    sectionCode: 'BASIC',
    sectionTitle: '基本信息',
    schemaType: 'TEXT',
    contentJson: {},
  },
  {
    id: 'JOB_INTENT',
    group: 'base',
    icon: ICONS.target,
    name: '求职意向',
    desc: '期望职位、城市、薪资',
    sectionCode: 'JOB_INTENT',
    sectionTitle: '求职意向',
    schemaType: 'TEXT',
    contentJson: {},
  },
  {
    id: 'EDUCATION',
    group: 'story',
    icon: ICONS.cap,
    name: '教育背景',
    desc: '学校、学历、专业',
    sectionCode: 'EDUCATION',
    sectionTitle: '教育背景',
    schemaType: 'LIST',
    contentJson: { items: [] },
  },
  {
    id: 'EXPERIENCE',
    group: 'story',
    icon: ICONS.case,
    name: '工作经验',
    desc: '工作经历与成就',
    sectionCode: 'EXPERIENCE',
    sectionTitle: '工作经验',
    schemaType: 'LIST',
    contentJson: { items: [] },
  },
  {
    id: 'INTERNSHIP',
    group: 'story',
    icon: ICONS.sprout,
    name: '实习经历',
    desc: '实习经历与收获',
    sectionCode: 'INTERNSHIP',
    sectionTitle: '实习经历',
    schemaType: 'LIST',
    contentJson: { items: [] },
  },
  {
    id: 'PROJECTS',
    group: 'story',
    icon: ICONS.rocket,
    name: '项目经历',
    desc: '项目经历与成果',
    sectionCode: 'PROJECTS',
    sectionTitle: '项目经历',
    schemaType: 'LIST',
    contentJson: { items: [] },
  },
  {
    id: 'CAMPUS',
    group: 'story',
    icon: ICONS.school,
    name: '校园经历',
    desc: '社团、志愿者、竞赛',
    sectionCode: 'CAMPUS',
    sectionTitle: '校园经历',
    schemaType: 'LIST',
    contentJson: { items: [] },
  },
  {
    id: 'SKILLS',
    group: 'proof',
    icon: ICONS.bolt,
    name: '技能特长',
    desc: '专业技能与工具',
    sectionCode: 'SKILLS',
    sectionTitle: '技能特长',
    schemaType: 'TAGS',
    contentJson: { items: [] },
  },
  {
    id: 'CERTIFICATES',
    group: 'proof',
    icon: ICONS.award,
    name: '荣誉证书',
    desc: '获奖、证书、资质',
    sectionCode: 'CERTIFICATES',
    sectionTitle: '荣誉证书',
    schemaType: 'LIST',
    contentJson: { items: [] },
  },
  {
    id: 'SELF_EVALUATION',
    group: 'proof',
    icon: ICONS.pen,
    name: '自我评价',
    desc: '个人总结与优势',
    sectionCode: 'SELF_EVALUATION',
    sectionTitle: '自我评价',
    schemaType: 'TEXT',
    contentJson: { text: '' },
  },
]

const groupLabels = {
  base: '基本信息',
  story: '经历与背景',
  proof: '能力与证明',
}

const groupOrder = ['base', 'story', 'proof']

// isAdded is called by the template before the store prop is available,
// so we expose sections via a prop passed from the parent
const props = defineProps({
  addedSectionCodes: {
    type: Array,
    default: () => []
  }
})

const visibleGroups = computed(() => {
  const search = keyword.value.trim().toLowerCase()

  return groupOrder
    .map((key) => {
      const matched = blocks.filter((block) => {
        if (block.group !== key) return false
        if (!search) return true
        return [block.name, block.desc, block.sectionCode]
          .join(' ')
          .toLowerCase()
          .includes(search)
      })

      if (!matched.length) return null
      return { key, label: groupLabels[key], blocks: matched }
    })
    .filter(Boolean)
})

function isAdded(id) {
  return props.addedSectionCodes.includes(id)
}

function handleAdd(id) {
  const block = blocks.find(b => b.id === id)
  if (block) {
    emit('add-block', {
      sectionType: 'SYSTEM',
      sectionCode: block.sectionCode,
      sectionTitle: block.sectionTitle,
      schemaType: block.schemaType,
      contentJson: { ...block.contentJson },
    })
  }
}
</script>

<style scoped>
.add-block {
  display: flex;
  flex-direction: column;
  gap: var(--ui-space-5);
  padding: var(--ui-space-4);
}

.add-block__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--ui-space-4);
  flex-wrap: wrap;
}

.section-title {
  margin: 0;
  font-size: var(--ui-text-lg);
  font-weight: 600;
  color: var(--ui-fg-primary);
}
.add-block__hint {
  margin: var(--ui-space-1) 0 0;
  font-size: var(--ui-text-sm);
  color: var(--ui-fg-secondary);
}

.add-block__search {
  width: 220px;
  max-width: 100%;
}

.add-block__group {
  display: flex;
  flex-direction: column;
  gap: var(--ui-space-2);
}

.add-block__group-title {
  margin: 0;
  font-size: var(--ui-text-xs);
  font-weight: 600;
  letter-spacing: 0.04em;
  color: var(--ui-fg-muted);
}

.blocks-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: var(--ui-space-3);
}

.block-card {
  display: flex;
  align-items: center;
  gap: var(--ui-space-3);
  padding: var(--ui-space-3);
  border: 1px solid var(--ui-border);
  border-radius: var(--ui-radius-lg);
  background: var(--ui-bg-surface);
  background-image: var(--ui-surface-wash);
  color: var(--ui-fg-primary);
  text-align: left;
  cursor: pointer;
  box-shadow: var(--ui-shadow-sm);
  transition: border-color var(--ui-duration-fast) var(--ui-ease-out),
    background var(--ui-duration-fast) var(--ui-ease-out),
    box-shadow var(--ui-duration-fast) var(--ui-ease-out);
}

.block-card:hover:not(.is-added) {
  border-color: var(--ui-accent-border);
  background: var(--ui-accent-soft);
  box-shadow: var(--ui-shadow-sm);
}

.block-card:focus-visible {
  outline: none;
  box-shadow: var(--ui-ring);
}

.block-card.is-added {
  cursor: default;
  background: var(--ui-bg-sunken);
}

.block-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  flex-shrink: 0;
  border-radius: var(--ui-radius-md);
  background: var(--ui-bg-sunken);
  color: var(--ui-fg-secondary);
}

.block-card:hover:not(.is-added) .block-icon {
  background: var(--ui-bg-surface);
  color: var(--ui-fg-accent);
}

.block-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
  flex: 1;
  min-width: 0;
}

.block-name {
  font-size: var(--ui-text-sm);
  font-weight: 600;
  color: var(--ui-fg-primary);
}

.block-desc {
  font-size: var(--ui-text-xs);
  color: var(--ui-fg-secondary);
}

.block-check {
  flex-shrink: 0;
}

.block-card--custom {
  border-style: dashed;
  border-color: var(--ui-border-strong);
  background: transparent;
}

.block-card--custom .block-icon {
  background: var(--ui-accent-soft);
  color: var(--ui-fg-accent);
}

.block-card--custom.is-expanded {
  border-color: var(--ui-accent-border);
  border-style: solid;
  background: var(--ui-accent-soft);
}

.add-block__empty {
  padding: var(--ui-space-8) var(--ui-space-4);
}

/* ── 自定义区块表单 ── */
.custom-form {
  border: 1px solid var(--ui-border);
  border-radius: var(--ui-radius-lg);
  background: var(--ui-bg-surface);
  overflow: hidden;
}

.custom-form__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--ui-space-2);
  padding: var(--ui-space-3) var(--ui-space-4);
  border-bottom: 1px solid var(--ui-border);
  background: var(--ui-bg-sunken);
}

.custom-form__title {
  font-size: var(--ui-text-sm);
  font-weight: 600;
  color: var(--ui-fg-primary);
}

.custom-form__body {
  display: flex;
  flex-direction: column;
  gap: var(--ui-space-4);
  padding: var(--ui-space-4);
}

.field {
  display: flex;
  flex-direction: column;
  gap: var(--ui-space-2);
}

.field :deep(.ui-label) {
  margin-bottom: 0;
}

.required {
  color: var(--ui-danger);
}

.custom-form__textarea {
  resize: vertical;
  min-height: 88px;
  font-family: inherit;
}

.field-hint {
  font-size: var(--ui-text-xs);
  color: var(--ui-fg-muted);
}

.radio-group {
  display: flex;
  flex-wrap: wrap;
  gap: var(--ui-space-4);
}

.radio-item {
  display: inline-flex;
  align-items: center;
  gap: var(--ui-space-2);
  font-size: var(--ui-text-sm);
  color: var(--ui-fg-secondary);
  cursor: pointer;
}

.radio-item input[type='radio'] {
  accent-color: var(--ui-accent);
}

.custom-form__submit {
  align-self: flex-start;
}
</style>
